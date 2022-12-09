package kr.uracle.ums.core.processor.naver;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class NaverWorkerThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
        
    private String ThreadName;
    private final NaverWorkerMgr workerManager;
    private boolean isRun = true;

    public NaverWorkerThread(String name, NaverWorkerMgr _naverWorkerMgr){
        super(name);
        this.ThreadName = getName();
        this.workerManager = _naverWorkerMgr;
    }

    public void run(){
        while(isRun){
            try {
                // Step 1 : 처리일감 가져옴.
            	BaseProcessBean baseBean = workerManager.takeWork();
            	if(baseBean == null) {
            		continue;
            	}
                execute(baseBean);
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException 발생");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
                e.printStackTrace();
            }
            TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.NAVER);
        }
    }
    
    private void execute(BaseProcessBean baseBean) {
        // STEP 1 : 발송 데이터 추출 및 조립
        UmsResultBaseBean rsltBean = makeResultBean(baseBean);
        final String PROVIDER = baseBean.getPROVIDER();
        boolean isSend = true;
        String errorMsg = null;

        // UMS <> PROVIDER 간의 조인 키 = SEND_TYPE_SEQCODE <> 마지막 여유 필드
        // MTS 조인키는 TRAN_ETC4 - XML QUERY MAPPING
        try {
            // STEP 2 : 발송 가능여부 체크
            isSend = workerManager.getUmsSendCheckService().chkSend(baseBean, rsltBean, false);

            // STEP 3 : 발송처리
            if(isSend) {
                workerManager.getProviderDao(PROVIDER).inNaverSendMsg(baseBean);
            }

            // STEP 4 : 발송 결과 조립
            if(isSend) {
                rsltBean.setSEND_RESULT("RS");
                rsltBean.setSUCC_STATUS("0");
                rsltBean.setRESULTMSG("발송요청성공");
            }
        }catch (RuntimeException re){
            errorMsg = "네트워크 에러";
            re.printStackTrace();
        }catch (SQLException se){
            errorMsg = "DB 에러";
            se.printStackTrace();
        }catch (Exception e) {
            errorMsg = ErrorManager.getInstance().getMsg(ErrorManager.ERR_500);
            e.printStackTrace();
        }
        //STEP 5 : 발송상세 결과정보 저장
        workerManager.getUmsDao().inUmsSendDetail(rsltBean, baseBean);

        //STEP 6 : 내부 에러 시 Agent 결과 처리를 위해 통합 테이블 입력
        if(errorMsg != null){
            rsltBean.setERRCODE(ErrorManager.ERR_500);
            rsltBean.setRESULTMSG(errorMsg);
            workerManager.getUmsDao().inUmsLog(rsltBean);
        }

        //STEP 7 : 통계 데이터 축적
        if("RS".equals(rsltBean.getSEND_RESULT())){
            workerManager.getSentInfoManager().addSendSucc(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.NAVERT);
        }else{
            workerManager.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.NAVERT, true);
        }
    }

    private UmsResultBaseBean makeResultBean(BaseProcessBean baseBean) {
    	 UmsResultBaseBean rsltBean = new UmsResultBaseBean(baseBean.getTRANS_TYPE(), SendType.NAVERT);
         rsltBean.setPROVIDER(baseBean.getPROVIDER());
    	 rsltBean.setMASTERTABLE_SEQNO(baseBean.getMASTERTABLE_SEQNO());
    	 rsltBean.setROOT_CHANNEL_YN(baseBean.getROOT_CHANNEL_YN());
    	 rsltBean.setSENDERID(baseBean.getSENDERID());
    	 rsltBean.setSENDERGROUPCODE(baseBean.getSENDGROUPCODE());
    	 rsltBean.setCUID(baseBean.getCUID());
    	 rsltBean.setCNAME(baseBean.getCNAME());
         if (StringUtils.isNotBlank(baseBean.getMSG_VARS())) {
        	 rsltBean.setMSG_VARS(baseBean.getMSG_VARS());
         }
        rsltBean.setSEND_TYPE_SEQCODE(baseBean.getTRANSACTION_KEY());
        rsltBean.setSVC_ID(baseBean.getSERVICE_ID());
        rsltBean.setCALLBACKNUM(baseBean.getCALLBACK_NUM());
        rsltBean.setMOBILE_NUM(baseBean.getMOBILE_NUM());
        String replaceMsgBody = replaceMsg(baseBean.getMSG_BODY(), baseBean);
        baseBean.setMSG_BODY(replaceMsgBody);
        rsltBean.setSEND_MSG(replaceMsgBody);
        rsltBean.setSEND_TITLE(baseBean.getTITLE());
        return rsltBean;
    }

    private String replaceMsg(String sendMsg, BaseProcessBean baseBean) {
        if (sendMsg.indexOf("#{") > -1) {
            sendMsg = sendMsg.replace("#{이름}", baseBean.getCNAME()).replace("#{아이디}", baseBean.getCUID());
            Map<String, String> personalMap = baseBean.getMSG_VAR_MAP();
            if (personalMap != null && personalMap.size() > 0) {
                // 개인화 메세지 치환처리
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    sendMsg = sendMsg.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
        }
        return sendMsg;
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }
}