package kr.uracle.ums.core.processor.sms;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.AmSmsProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.ImoSmsProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.LotteSmsProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.util.amsoft.AmProcessor;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import kr.uracle.ums.tcppitcher.codec.messages.AmSmsMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;


/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
public class SmsWorkerThread extends Thread{
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final String ThreadName;
    private final SmsWorkerMgr workerManager;
    private boolean isRun = true;
    
	private final boolean USE_FATIGUE;

    private final Gson gson = new Gson();
	
    public SmsWorkerThread(String name, SmsWorkerMgr _smsWorkerMgr){
        super(name);
        this.ThreadName=getName();
        this.workerManager =_smsWorkerMgr;
        USE_FATIGUE = _smsWorkerMgr.getUSE_FATIGUE().equalsIgnoreCase("Y");
    }
    public void run(){
        while(isRun){
            ///push 전송 로직
            try {
            	BaseProcessBean baseBean = workerManager.takeWork();
                if(baseBean == null) {
                	continue;
                }
                execute(baseBean);
                baseBean = null;  // GC 대상으로 만들기 위해
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException 발생");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
            }

            TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.SMS);

        }
    }

    private void execute(BaseProcessBean baseBean) {
        // STEP 1 : 발송 데이터 추출 및 조립
        UmsResultBaseBean rsltBean = makeResultBean(baseBean);
        final String PROVIDER = baseBean.getPROVIDER();
        boolean isFatigue = false;
        boolean isSend = true;
        String errorMsg = null;
        String errorCode = null;
        SendType sendType = SendType.SMS;
        String transactionKey = baseBean.getTRANSACTION_KEY();

        // UMS <> PROVIDER 간의 조인 키 = SEND_TYPE_SEQCODE <> 마지막 여유 필드
        // KT 조인키는 RESERVED9    - XML QUERY MAPPING
        // LOTTE 조인키는 USER9     - XML QUERY MAPPING
        // AM 조인키는 MessageId    - AmKkoAltMessage Assemble MAPPING
        // LGU 조인키는 ETC6        - XML QUERY MAPPING
        try {
            isFatigue = isFatigue(baseBean);
            // STEP 2 : 치환 후 메시지 길이 변경에 따른 채널 타입 체크
            if(baseBean.getMSG_BODY().getBytes("EUC-KR").length > 90 ){
                sendType = SendType.LMS;
                transactionKey = TransactionUtil.changeChannelInfo(transactionKey, sendType.toString());

                rsltBean.setSEND_TYPE(sendType.toString());
                rsltBean.setDETAIL_SEND_TYPE(sendType.toString());
                rsltBean.setSEND_TYPE_SEQCODE(transactionKey);
                baseBean.setTRANSACTION_KEY(transactionKey);
                baseBean.setSTART_SEND_TYPE(sendType.toString());
                
            }
            
            if (baseBean instanceof LotteSmsProcessBean) {
                LotteSmsProcessBean prcsBean = (LotteSmsProcessBean) baseBean;
                prcsBean.setCMID(transactionKey.substring(3));
            }

            //STEP 3 : 발송가능 여부 체크
            isSend = workerManager.getUmsSendCheckService().chkSend(baseBean, rsltBean, isFatigue);

            //STEP 4 : 발송상세 결과정보 저장
            workerManager.getUmsDao().inUmsSendDetail(rsltBean, baseBean);

            //STEP 5 : 발송처리
            if(isSend) {
                if(baseBean instanceof AmSmsProcessBean){
                    AmSmsMessage amSmsMessage = makeAmSmsMessage((AmSmsProcessBean)baseBean, sendType.toString());
                    AmProcessor.getInstance().sendMsg(amSmsMessage);
                }else if(baseBean instanceof ImoSmsProcessBean){
                    ImoSmsProcessBean prcsBean = (ImoSmsProcessBean)baseBean;
                    prcsBean.putBody("uid",         baseBean.getTRANSACTION_KEY());
                    prcsBean.putBody("contents",    baseBean.getMSG_BODY());
                    prcsBean.putBody("msgType",     sendType.toString());

                    ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(prcsBean.getHOST_URL(), prcsBean.getHEADER_MAP(), prcsBean.getBODY_MAP());
                    if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                        Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), new TypeToken<Map<String, Object>>(){}.getType());
                        String rsltCode = responseMap.get("code") == null ? null : responseMap.get("code").toString();
                        if(rsltCode == null || rsltCode.equals("200") == false){
                            errorCode = rsltCode;
                            errorMsg = "IMO 서버 에러"+ responseMap.get("message");
                        }
                    }else{
                        errorCode = ErrorManager.ERR_500;
                        errorMsg = "IMO 서버 에러 - HTTP STATUS :"+responseBean.getStatusCode();
                    }
                    if(StringUtils.isNotBlank(errorMsg)) logger.info("IMO 서버 에러 발생 :"+errorMsg);
                }else{
                    if(sendType == SendType.SMS){
                        workerManager.getProviderDao(PROVIDER).inSmsSendMsg(baseBean);
                    }else{
                        workerManager.getProviderDao(PROVIDER).inFromSmsToMmsSendMsg(baseBean);
                    }
                }
            }
        }catch (RuntimeException re){
            errorCode = ErrorManager.ERR_500;
            errorMsg = "네트워크 에러";
            re.printStackTrace();
        }catch (SQLException se){
            errorCode = ErrorManager.ERR_500;
            errorMsg = "DB 에러";
            se.printStackTrace();
        }catch (Exception e) {
            errorCode = ErrorManager.ERR_500;
            errorMsg = ErrorManager.getInstance().getMsg(ErrorManager.ERR_500);
            e.printStackTrace();
        }

        //STEP 6 : 내부 에러 시 Agent 결과 처리를 위해 통합 테이블 입력
        if(errorMsg != null){
            rsltBean.setERRCODE(errorCode);
            rsltBean.setRESULTMSG(errorMsg);
            workerManager.getUmsDao().inUmsLog(rsltBean);
        }

        //STEP 7: 통계 데이터 축적
        if("RS".equals(rsltBean.getSEND_RESULT())){
            workerManager.getSentInfoManager().addSendSucc(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), sendType);
            if(isFatigue){
                workerManager.getUmsSendCommonService().upFatigue(baseBean.getCUID(), false);
            }
        }else{
            workerManager.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), sendType, true);
        }
    }

    private AmSmsMessage makeAmSmsMessage(AmSmsProcessBean prcsBean, String sendChannel){
        AmSmsMessage amMsg = new AmSmsMessage();
        amMsg.setAckYn("Y");
        amMsg.setTranType(prcsBean.getTRANS_TYPE()== TransType.REAL?"R":"B");
        amMsg.setAdsYn(prcsBean.getUMS_MSG_TYPE().equals("I")?"N":"Y");
        amMsg.setUmsSeqKey(""+prcsBean.getMASTERTABLE_SEQNO());
        amMsg.setMessageId(prcsBean.getTRANSACTION_KEY());
        amMsg.setSendChannel(sendChannel);
        amMsg.setSenderGroup(prcsBean.getSENDGROUPCODE());
        amMsg.setSenderSys(prcsBean.getSENDERID());
        amMsg.setUserId(prcsBean.getUSER_ID());
        amMsg.setPhoneNum(prcsBean.getMOBILE_NUM());
        amMsg.setCallbackNum(prcsBean.getCALLBACK_NUM());
        amMsg.setTitle(prcsBean.getTITLE());
        amMsg.setMessage(prcsBean.getMSG_BODY());
        amMsg.setVar1(prcsBean.getVAR1());
        amMsg.setVar2(prcsBean.getVAR2());
        amMsg.setVar3(prcsBean.getVAR3());
        amMsg.setVar4(prcsBean.getVAR4());
        amMsg.setVar5(prcsBean.getVAR5());
        return amMsg;
    }

	private boolean isFatigue(BaseProcessBean baseBean) {
		boolean isFatigue = false;
		if ("Y".equals(baseBean.getFATIGUE_YN()) || USE_FATIGUE ) isFatigue = true;
		if(baseBean.getUMS_MSG_TYPE().equalsIgnoreCase("I")) isFatigue = false;
		return isFatigue;
	}
    
	private UmsResultBaseBean makeResultBean(BaseProcessBean baseBean) {
		UmsResultBaseBean rsltBean = new UmsResultBaseBean(baseBean.getTRANS_TYPE(), SendType.SMS);
        rsltBean.setPROVIDER(baseBean.getPROVIDER());
		rsltBean.setMASTERTABLE_SEQNO(baseBean.getMASTERTABLE_SEQNO());
		rsltBean.setSENDERID(baseBean.getSENDERID());
		rsltBean.setSENDERGROUPCODE(baseBean.getSENDGROUPCODE());
		rsltBean.setROOT_CHANNEL_YN(baseBean.getROOT_CHANNEL_YN());
		rsltBean.setCUID(baseBean.getCUID());
		rsltBean.setCNAME(baseBean.getCNAME());
		if (!"".equals(baseBean.getMSG_VARS())) {
			rsltBean.setMSG_VARS(baseBean.getMSG_VARS());
		}
        rsltBean.setSEND_TYPE_SEQCODE(baseBean.getTRANSACTION_KEY());
        rsltBean.setCALLBACKNUM(baseBean.getCALLBACK_NUM());
        rsltBean.setMOBILE_NUM(baseBean.getMOBILE_NUM());
        String replaceMsgBody = replaceMsg(baseBean.getMSG_BODY(), baseBean);
        baseBean.setMSG_BODY(replaceMsgBody);
        rsltBean.setSEND_MSG(replaceMsgBody);
        rsltBean.setSEND_TITLE(baseBean.getTITLE());
        rsltBean.setSEND_RESULT("RS");
        rsltBean.setSUCC_STATUS("0");
        rsltBean.setRESULTMSG("발송요청성공");
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
