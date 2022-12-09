package kr.uracle.ums.core.batch;

import com.google.gson.Gson;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.http.client.config.RequestConfig;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class McsNaverTemplBatch {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddhhmmss");
    private final Map<String, Object> param = new HashMap<String, Object>();
    private String WORKTRACE = "";
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    private SqlSessionTemplate sqlSessionTemplate;

    @Value("${NAVER.BATCH_MIN:360}")
    private String BATCH_LOOP_MIN;

    @Value(("${NAVER.TEMPL.SYNC.HOST:https://api.mtsco.co.kr}"))
    private String NAVER_SYNC_HOST_URL;

    private ThreadPoolTaskScheduler scheduler;
    private Gson gson = new Gson();

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### MTS NAVER TEMPLATE SYNC BATCH STOP!");
    }
    public void startScheduler() {
        logger.info("### MTS NAVER TEMPLATE SYNC BATCH RUN~~!");
        this.WORKTRACE = MasterRollChecker.getInstance().getProcessorKey();

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable(){
        return new Runnable(){
            @Override
            public void run() {
                if (!MasterRollChecker.getInstance().isMaster()) return;
                try {
                    param.put("WORKTRACE", WORKTRACE);
                    param.put("PROCESSORID", String.format("MtsNaverBatch_%s", format1.format(new Date())));
                    sqlSessionTemplate.insert("mybatis.batch.insProcessorInfo", param);
                    templApproveSyncExec();
                    sqlSessionTemplate.update("mybatis.batch.uptProcessorInfo", param);
                    param.clear();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };
    }

    private Trigger getTrigger() {	// 작업 주기 설정
//        int batchLoopMin = 360000;  //6시간에 한번씩
//        int batchInitDelayMin = 1; //1분 지연
//        PeriodicTrigger periodicTrigger = new PeriodicTrigger(batchLoopMin, TimeUnit.SECONDS);
//        periodicTrigger.setInitialDelay(batchInitDelayMin); //구동후 기본 1분 뒤 실행. 설정값의 의해 변경됨.
//        return periodicTrigger;

        int batchLoopMin = 360;  //6시간에 한번씩
        int batchInitDelaySecond = 30000; //30초 지연

        PeriodicTrigger periodicTrigger = new PeriodicTrigger(batchLoopMin, TimeUnit.MINUTES);
        periodicTrigger.setInitialDelay(batchInitDelaySecond); //구동후 기본 30초 뒤 실행. 설정값의 의해 변경됨.
        return periodicTrigger;

//        return new CronTrigger("0 0 2 * * *");
    }

    private void templApproveSyncExec() {
        // STEP 1 : T_UMS_PROCESSORINFO 테이블에 STATUS와 COMPLETEDDATE을 이용하여 배치가 돌지 말지 결정. 다른 UMS에서 먼저 돌았을 수 있기 때문.
        logger.info("### MTS NAVER TEMPLATE SYNC BATCH START~~!");

        String RESULTCODE = "S";
        String RESULTMSG = "";

        try {
            Map<String,String> dbParamMap = new HashMap<>();
            List<HashMap<String,Object>> dbNaverTemplList = sqlSessionTemplate.selectList("mybatis.common.selMtsNaverInfo",dbParamMap);

            int SyncTemplCnt = 0;
            if(dbNaverTemplList!=null && dbNaverTemplList.size()>0){
                NAVER_SYNC_HOST_URL = NAVER_SYNC_HOST_URL.trim();
                for(Map<String,Object> dbNaverTemplMap : dbNaverTemplList){
                    if(dbNaverTemplMap.containsKey("PARTNERKEY") && dbNaverTemplMap.containsKey("TMPL_CD")){
                        String PARTNERKEY = dbNaverTemplMap.get("PARTNERKEY").toString();
                        String TMPL_CD = dbNaverTemplMap.get("TMPL_CD").toString();

                        try {
                            Map<String, String> httpHeadParam = new HashMap();
                            httpHeadParam.put("Content-Type","application/json");
                            Map<String, Object> postParam = new HashMap<String,Object>();

                            int responsTimeout = 30;
                            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(responsTimeout * 1000).build();

                            String apiUrl = NAVER_SYNC_HOST_URL+"/naver/v1/template/"+PARTNERKEY+"/"+TMPL_CD;
                            ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(apiUrl, httpHeadParam, postParam, requestConfig);
                            // 응답데이타 샘플 :
                            // {"success":true,"template":{"id":9053,"code":"URACLE_TEST004","text":"[TEST] 배송이 시작되었습니다.","partnerId":"w4qkza","categoryCode":"S006"
                            // ,"templateStatusType":"APPROVED","templateSendingStatusType":"WAITING"
                            // ,"buttons":[{"type":"WEB_LINK","buttonCode":"btn-web-1","buttonName":"배송조회하기"},{"type":"APP_LINK","buttonCode":"btn-app-1","buttonName":"배송조회하기"}]
                            // ,"sampleImage":"https://bizalarm-phinf.pstatic.net/MjAyMjA1MjBfNzUg/MDAxNjUzMDA2Njg4ODkw.CQJE4RPzqHyaBTsLKVTL03VXqato6QfcN_-sRuhpRtQg.0sbJVXLfzU-qH9nG_rvfzj4aWreBl3685sCEMijs4E4g.PNG/20220520093128556.png?type=m600"
                            // ,"createdAt":"2022-05-20 15:07:19","modifiedAt":"2022-05-24 10:14:05"}}

                            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                                Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), Map.class);

                                if(Boolean.parseBoolean(responseMap.get("success").toString())) {
                                    Map<String,Object> templateInfoMap = (Map<String,Object>)responseMap.get("template");
                                    Map<String, Object> dbparamMap = new HashMap<>();
                                    dbParamMap.put("TMPL_CD", TMPL_CD);
                                    try {
                                        Double tmplIdLong = (Double) templateInfoMap.get("id");
                                        int templIdInd = tmplIdLong.intValue();
                                        dbParamMap.put("TMPL_ID", templIdInd+"");
                                    }catch (Exception e){
                                        dbParamMap.put("TMPL_ID", templateInfoMap.get("id").toString());
                                    }
                                    dbParamMap.put("MSG", templateInfoMap.get("text").toString());
                                    dbParamMap.put("PARTNERKEY", PARTNERKEY);
                                    dbParamMap.put("NAVER_PROFILE", templateInfoMap.get("partnerId").toString());
                                    dbParamMap.put("CATEGORYCODE", templateInfoMap.get("categoryCode").toString());
                                    if(dbParamMap.containsKey("BUTTONS")){
                                        dbParamMap.put("BUTTONS", templateInfoMap.get("buttons").toString());
                                    }else{
                                        dbParamMap.put("BUTTONS", "");
                                    }
                                    if(templateInfoMap.containsKey("sampleImage")) {
                                        dbParamMap.put("IMGHASH", templateInfoMap.get("sampleImage").toString());
                                    }else{
                                        dbParamMap.put("IMGHASH", "");
                                    }
                                    dbParamMap.put("TEMPL_STATUS", templateInfoMap.get("templateStatusType").toString());
                                    dbParamMap.put("TEMPL_SEND_STATUS", templateInfoMap.get("templateSendingStatusType").toString());
                                    dbParamMap.put("REGISTER_DATE", templateInfoMap.get("createdAt").toString());
                                    dbParamMap.put("UPDATE_DATE", templateInfoMap.get("modifiedAt").toString());

                                    int applyRow = sqlSessionTemplate.update("mybatis.naver.mts.upTempletInfo", dbParamMap);
                                    if(applyRow==0){
                                        sqlSessionTemplate.insert("mybatis.naver.mts.inTempletInfo", dbParamMap);
                                    }
                                }else{
                                    param.put("RESULTMSG", responseBean.getBody());
                                    logger.error("MST 네이버톡 응답 에러 : "+responseBean.getBody());
                                }
                            }else{
                                param.put("RESULTMSG", "HTTP 에러 :"+responseBean.getStatusCode());
                                logger.error("MST 네이버톡 HTTP 에러 : "+responseBean.getBody());
                            }

                            SyncTemplCnt++;
                        } catch (Exception var13) {
                            param.put("RESULTMSG", var13.getCause());
                            logger.error(var13.getMessage());
                        }
                    }else{
                        param.put("RESULTMSG", "DB에 저장된 MTS 네이버 템플릿정보에 PARTNERKEY or TMPL_CD 키가 존재하지 않아 동기화 호출 하지 않음.");
                        logger.error("DB에 저장된 MTS 네이버 템플릿정보에 PARTNERKEY or TMPL_CD 키가 존재하지 않아 동기화 호출 하지 않음.");
                    }
                }
            }

            param.put("RESULTCODE", RESULTCODE);
            if(dbNaverTemplList!=null && dbNaverTemplList.size()>0){
                if(SyncTemplCnt==0){
                    param.put("RESULTCODE", "E");
                    param.put("RESULTMSG", "MTS 네이버 템플릿 동기화 처리하였으나 에러가 발생. "+param.get("RESULTMSG"));
                }else{
                    param.put("RESULTMSG", "MTS 네이버 템플릿 검증 수 :"+SyncTemplCnt);
                }
            }
            logger.info("### MTS NAVER TEMPLATE SYNC BATCH COMPLATE~~!");
        } catch (Exception e) {
            param.put("RESULTCODE", "E");
            param.put("RESULTMSG", e.getCause());
            logger.error(e.getMessage(), e);
        }

        try{
            // 프로세스 실행결과 저장
            sqlSessionTemplate.update("mybatis.batch.uptProcessorInfo", param);
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
