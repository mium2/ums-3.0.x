package kr.uracle.ums.core.batch;

import com.google.gson.Gson;
import kr.uracle.ums.core.batch.vo.LotteAltTemplVo;
import kr.uracle.ums.core.batch.vo.LotteTokenVo;
import kr.uracle.ums.core.service.template.AlmTokTemplService;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
public class LotteAlmTokTemplBatch {
    private Logger logger = LoggerFactory.getLogger(LotteAlmTokTemplBatch.class);

    private SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddhhmmss");
    private Map<String, Object> param = new HashMap<String, Object>();
    private String WORKTRACE = "";
    private RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(15 * 1000).build();
    private String TOKENAPI = "/v1/auth/tokens";
    private String TEMPLATE_ALL_INFO = "/v1/template/all";
    private String TEMPLATE_INFO = "/v1/template";
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private AlmTokTemplService almTokTemplService;
    @Autowired(required = true)
    protected Gson gson;
    @Value(("${KKO.ALLIMTOLK.INIT_DELAY_MIN:1}"))
    private String INIT_DELAY_MIN;
    @Value("${KKO.ALLIMTOLK.BATCH_MIN:360}")
    private String BATCH_LOOP_MIN;

    @Value("${LOTTE.API_HOST:https://lapi.bizlotte.com}")
    private String API_HOST;
    @Value("${LOTTE.CLIENT_ID:}")
    private String CLIENT_ID;
    @Value("${LOTTE.AGENT_PWD:}")
    private String AGENT_PWD;
    @Value("${LOTTE.SENDERKEY:}")
    private String SENDERKEY;
    @Value("${LOTTE.SENDER_TYPE:S}")
    private String SENDER_TYPE;
    private LotteTokenVo lotteTokenVo;
    private ThreadPoolTaskScheduler scheduler;
    private List<String> SENDERKEY_LIST = new ArrayList<>();

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### [LOTTE ALLIMTOLK TEMPLATE] SYNC BATCH STOP!");
    }
    public void startScheduler() {
        logger.info("### [LOTTE ALLIMTOLK TEMPLATE] SYNC BATCH RUN~~!");
        this.WORKTRACE = MasterRollChecker.getInstance().getProcessorKey();

        if(SENDERKEY.indexOf(",")>0){
            String[] SENDERKEY_ARR = SENDERKEY.split(",");
            for(String senderKey : SENDERKEY_ARR){
                SENDERKEY_LIST.add(senderKey.trim());
            }
        }else{
            SENDERKEY_LIST.add(SENDERKEY.trim());
        }

        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        // ??????????????? ???????????? ??????
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable(){
        return new Runnable(){
            @Override
            public void run() {
                if (!MasterRollChecker.getInstance().isMaster()) return;
                try {
                    param.put("WORKTRACE", WORKTRACE);
                    param.put("PROCESSORID", String.format("LOTTE_KKOALT_Batch_%s", format1.format(new Date())));
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
    private Trigger getTrigger() {	// ?????? ?????? ??????
        int batchLoopMin = 360;  //6????????? ?????????
        int batchInitDelayMin = 1; //1??? ??????
        try {
            batchLoopMin = Integer.parseInt(BATCH_LOOP_MIN.trim());
            batchInitDelayMin = Integer.parseInt(INIT_DELAY_MIN.trim());
        }catch(Exception e){
            logger.error(e.toString());
        }
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(batchLoopMin, TimeUnit.MINUTES);
        periodicTrigger.setInitialDelay(batchInitDelayMin); //????????? ?????? 1??? ??? ??????. ???????????? ?????? ?????????.
        return periodicTrigger;
    }
    private void templApproveSyncExec() {
        // ??? ????????? ?????? LOTTE??? ????????? ????????? ???????????? ????????? ???????????? ????????? ?????? ??????.
        logger.info("###[LOTTE] ALLIMTOLK TEMPLATE SYNC BATCH START~~!");
        String RESULTCODE = "S";
        String RESULTMSG = "";
        try {
            if(StringUtils.isBlank(API_HOST)){
                param.put("RESULTCODE", "E");
                param.put("RESULTMSG", "API_HOST ???????????? ????????????. ??????????????? ???????????????");
                return;
            }
            if(StringUtils.isBlank(CLIENT_ID)){
                param.put("RESULTCODE", "E");
                param.put("RESULTMSG", "CLIENT_ID ???????????? ????????????. ?????????????????????");
                return;
            }
            if(StringUtils.isBlank(AGENT_PWD)){
                param.put("RESULTCODE", "E");
                param.put("RESULTMSG", "AGENT_PWD ???????????? ????????????. ?????????????????????");
                return;
            }
            if(StringUtils.isBlank(SENDERKEY)){
                param.put("RESULTCODE", "E");
                param.put("RESULTMSG", "SENDERKEY ???????????? ????????????. ?????????????????????");
                return;
            }
            //STEP 1 : ?????? ????????????
            // ????????? ????????? ?????? ????????? ?????? ??????.
            if(lotteTokenVo==null){
                this.lotteTokenVo = getToken(CLIENT_ID, AGENT_PWD);
            }
            // ?????? ?????????????????? ?????? ??????
            if(System.currentTimeMillis()>lotteTokenVo.getExpireTimeStamp()){
                this.lotteTokenVo = getToken(CLIENT_ID, AGENT_PWD);
            }
            if(lotteTokenVo==null){
                param.put("RESULTCODE", "E");
                param.put("RESULTMSG", "TOKEN??? ?????? ?????? ???????????????.");
                return;
            }

            param.put("RESULTCODE", "S");
            param.put("RESULTMSG", "OK");;

            //STEP 2 : ?????? ????????? ???????????? ??????
            for(String syncSenderKey : SENDERKEY_LIST) {
                String resultJsonList = getTemplateAll(syncSenderKey, SENDER_TYPE);
                Map<String, Object> resultJsonMap = gson.fromJson(resultJsonList, Map.class);
                if ("200.0".equals(resultJsonMap.get("code").toString()) || "200".equals(resultJsonMap.get("code").toString())) {
//                if(true){
//                    ///////////////////////////////////////////////////////////////////////////////////////////////////
//                    // TEST??? ??????
//                    String TEST_RESULT_JSON = "{" +
//                            "\"code\": 200," +
//                            "\"msg\": \"OK\"," +
//                            "\"data\": [" +
//                            "{\"tem_code\": \"LMSG_20190404134824000040\",\"tem_name\": \"LMSG_20190404134824000040\",\"tem_stat_code\": \"A\"}" +
//                            "]" +
//                            "}";
//                    resultJsonMap = gson.fromJson(TEST_RESULT_JSON, Map.class);
//                    ///////////////////////////////////////////////////////////////////////////////////////////////////

                    List<Map<String,String>> templateSchList = (List<Map<String,String>>)resultJsonMap.get("data");
                    if(templateSchList!=null && templateSchList.size()>0) {
                        for (int i=0; i<templateSchList.size(); i++){
                            Map<String,String> templInfoMap = templateSchList.get(i);
                            String sch_tem_code = templInfoMap.get("tem_code");
                            String sch_tem_name = templInfoMap.get("tem_name");
                            String sch_tem_stat_code = templInfoMap.get("tem_stat_code");
                            // STEP 3 : ????????? ????????????
                            String templateDetailJsonResult = getTemplateInfo(syncSenderKey,sch_tem_code);
                            try {
                                Map<String, Object> templDetailResMap = gson.fromJson(templateDetailJsonResult, Map.class);
                                if("200.0".equals(templDetailResMap.get("code").toString()) || "200".equals(templDetailResMap.get("code").toString())) {
                                    Map<String, Object> templDetailMap = (Map<String, Object>)templDetailResMap.get("data");
                                    // STEP 4 : ????????? ???????????? ???????????? ????????? lotteAltTemVo
                                    LotteAltTemplVo lotteAltTemplVo = new LotteAltTemplVo();
                                    lotteAltTemplVo.setSENDER_KEY(templDetailMap.get("sender_key").toString());
                                    lotteAltTemplVo.setTEMPLATECODE(templDetailMap.get("tem_code").toString());
                                    lotteAltTemplVo.setSENDER_TYPE(templDetailMap.get("sender_type").toString());
                                    lotteAltTemplVo.setTEMPLATE_NAME(templDetailMap.get("tem_name").toString());
                                    lotteAltTemplVo.setTEMPLATE_CONTENT(templDetailMap.get("tem_content").toString());
                                    lotteAltTemplVo.setTEM_STAT_CODE(templDetailMap.get("tem_stat_code").toString());
                                    if (templDetailMap.get("tem_msg_type")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_MESSAGE_TYPE(templDetailMap.get("tem_msg_type").toString());
                                    }
                                    if (templDetailMap.get("tem_extra")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_EXTRA(templDetailMap.get("tem_extra").toString());
                                    }
                                    if (templDetailMap.get("tem_ad")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_AD(templDetailMap.get("tem_ad").toString());
                                    }
                                    if (templDetailMap.get("tem_emphasize_type")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_EMPHASIZE_TYPE(templDetailMap.get("tem_emphasize_type").toString());
                                    }
                                    if (templDetailMap.get("tem_title")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_TITLE(templDetailMap.get("tem_title").toString());
                                    }
                                    if (templDetailMap.get("tem_subtitle")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_SUBTITLE(templDetailMap.get("tem_subtitle").toString());
                                    }
                                    if (templDetailMap.get("tem_image_name")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_IMAGE_NAME(templDetailMap.get("tem_image_name").toString());
                                    }
                                    if (templDetailMap.get("tem_image_url")!=null) {
                                        lotteAltTemplVo.setTEMPLATE_IMAGE_URL(templDetailMap.get("tem_image_url").toString());
                                    }
                                    if (templDetailMap.get("tem_category_code")!=null) {
                                        lotteAltTemplVo.setCATEGORY_CODE(templDetailMap.get("tem_category_code").toString());
                                    }
                                    if (templDetailMap.get("tem_security_flag")!=null) {
                                        lotteAltTemplVo.setSECURITY_FLAG(templDetailMap.get("tem_security_flag").toString());
                                    }
                                    if (templDetailMap.get("tem_button_json")!=null) {
                                        lotteAltTemplVo.setBUTTONS(templDetailMap.get("tem_button_json").toString());
                                    }
                                    if (templDetailMap.get("block")!=null) {
                                        lotteAltTemplVo.setBLOCK(templDetailMap.get("block").toString());
                                    }
                                    if (templDetailMap.get("dormant")!=null) {
                                        lotteAltTemplVo.setDORMANT(templDetailMap.get("dormant").toString());
                                    }
                                    if (templDetailMap.get("create_id")!=null) {
                                        lotteAltTemplVo.setCREATE_ID(templDetailMap.get("create_id").toString());
                                    }
                                    if (templDetailMap.get("create_date")!=null) {
                                        lotteAltTemplVo.setCREATE_DATE(templDetailMap.get("create_date").toString());
                                    }
                                    if (templDetailMap.get("approval_date")!=null) {
                                        lotteAltTemplVo.setAPPROVAL_DATE(templDetailMap.get("approval_date").toString());
                                    }
                                    if (templDetailMap.get("comments")!=null) {
                                        lotteAltTemplVo.setCOMMENTS(templDetailMap.get("comments").toString());
                                    }
                                    // STEP 5 : ?????????????????? ???????????? ?????? ????????? DB??? ????????????
                                    int upApplyRow = sqlSessionTemplate.update("mybatis.template.allimtolk.lotte.syncTemplate", lotteAltTemplVo);
                                    if (upApplyRow == 0) {
                                        // DB??? ?????? ???????????? ???????????? ?????? ????????? ??????
                                        int inApplyRow = sqlSessionTemplate.update("mybatis.template.allimtolk.lotte.inTemplate", lotteAltTemplVo);
                                    }
                                }else{
                                    logger.error("LOTTE ????????? ????????????????????? ?????? ?????? ?????? : "+templateDetailJsonResult);
                                }

                            }catch (Exception e){
                                logger.error("LOTTE ????????? ??????????????? ?????? ????????? ?????? ?????? : "+e.getMessage());
                            }
                        }
                    }
                } else {
                    param.put("RESULTCODE", resultJsonMap.get("code").toString());
                    param.put("RESULTMSG", resultJsonMap.get("msg").toString());
                }
            }

            logger.info("###[LOTTE] ALLIMTOLK TEMPLATE SYNC BATCH COMPLATE~~!");
        } catch (Exception e) {
            param.put("RESULTCODE", "E");
            param.put("RESULTMSG", e.toString());
            logger.error(e.getMessage(), e);
        }
    }

    public String getTemplateInfo(String senderKey, String templateCode){
        Map<String,Object> failMap = new HashMap<>();
        failMap.put("code","500");
        failMap.put("msg","FAIL");

        try {
            Map<String, String> httpHeadParam = new HashMap<>();
            httpHeadParam.put("Authorization", lotteTokenVo.getAuthorization());

            Map<String,Object> postParam = new HashMap<>();
            postParam.put("senderKey",senderKey);
            postParam.put("templateCode",templateCode);

            String callUrl = API_HOST+TEMPLATE_INFO;
            ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(callUrl, httpHeadParam, postParam, requestConfig);
            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                return responseBean.getBody();
            }else{
                failMap.put("code", ""+responseBean.getStatusCode());
                failMap.put("msg", responseBean.getBody());
            }
        } catch (Exception ex) {
            failMap.put("code","500");
            failMap.put("msg",ex.getMessage());
        }
        return gson.toJson(failMap);
    }

    private String getTemplateAll(String senderKey, String senderKeyType){
        Map<String,Object> failMap = new HashMap<>();
        failMap.put("code","500");
        failMap.put("msg","FAIL");

        try {
            Map<String, String> httpHeadParam = new HashMap<>();
            httpHeadParam.put("Authorization", lotteTokenVo.getAuthorization());

            String callUrl = API_HOST+TEMPLATE_ALL_INFO;
            callUrl+="?senderKey="+senderKey+"&senderKeyType="+senderKeyType;
            ResponseBean responseBean = HttpPoolClient.getInstance().sendGet(callUrl, httpHeadParam);
            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                return responseBean.getBody();
            }else{
                failMap.put("code", ""+responseBean.getStatusCode());
                failMap.put("msg", responseBean.getBody());
            }
        } catch (Exception ex) {
            failMap.put("code","500");
            failMap.put("msg",ex.getMessage());
        }
        return gson.toJson(failMap);
    }

    protected LotteTokenVo getToken(String clientId, String agentPwd) throws Exception{
        String callUrl = API_HOST+TOKENAPI;
        LotteTokenVo lotteTokenVo = null;
        Map<String,Object> postParam = new HashMap<>();
        postParam.put("client_id",clientId);
        postParam.put("agent_pwd",agentPwd);

        ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(callUrl, postParam, requestConfig);
        if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
            Map<String,Object> respMap = gson.fromJson(responseBean.getBody(),Map.class);
            if(respMap.containsKey("code")){
                Double resq_code = (Double)respMap.get("code");
                if(resq_code.intValue()==200) {
                    Map<String,String> dataMap = (Map<String,String>)respMap.get("data");
                    String resp_access_key = dataMap.get("access_key");
                    String resp_access_secret = dataMap.get("access_secret");
                    String resp_expire_time = dataMap.get("expire_time");
                    Date expireDate = format1.parse(resp_expire_time);
                    lotteTokenVo = new LotteTokenVo();
                    lotteTokenVo.setLoginId(dataMap.get("login_id"));
                    lotteTokenVo.setAccessKey(resp_access_key);
                    lotteTokenVo.setAccessKey(resp_access_secret);
                    lotteTokenVo.setApiFlag(dataMap.get("api_flag"));
                    lotteTokenVo.setApiFlag(resp_expire_time);
                    lotteTokenVo.setExpireTimeStamp(expireDate.getTime());
                    String encodeStr = resp_access_key + ":" + resp_access_secret;
                    byte[] base64Bytes = Base64.encodeBase64(encodeStr.getBytes());
                    String auth = "Basic " + new String(base64Bytes, "UTF-8");
                    lotteTokenVo.setAuthorization(auth);
                }else{
                    throw new RuntimeException("LOTTE ???????????? ?????? : ???????????? : "+respMap.get("code")+" ??????????????? : "+respMap.get("msg"));
                }
            }else{
                throw new RuntimeException("LOTTE ???????????? ????????? ???????????? ???????????? ?????? : "+responseBean.getBody());
            }
        }else{
            throw new RuntimeException("LOTTE ???????????? ????????? HTTP ?????? : "+responseBean.getBody());
        }

        return lotteTokenVo;
    }
}
