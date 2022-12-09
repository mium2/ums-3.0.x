package kr.uracle.ums.core.processor.push;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.service.send.kko.BaseKkoAltSendService;
import kr.uracle.ums.core.service.send.kko.BaseKkoFrtSendService;
import kr.uracle.ums.core.service.send.mms.BaseMmsSendService;
import kr.uracle.ums.core.service.send.naver.BaseNaverSendService;
import kr.uracle.ums.core.service.send.rcs.BaseRcsSendService;
import kr.uracle.ums.core.service.send.sms.BaseSmsSendService;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
@SuppressWarnings("unchecked")
public class PushWorkerThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    
    private String ThreadName;
    private final PushWorkerMgr pushWorkerMgr;
    private boolean isRun = true;
    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final boolean USE_FATIGUE;
    private final String PROVIDER = "URACLE";

    public PushWorkerThread(String name, PushWorkerMgr _pushWorkerMgr){
        super(name);
        this.ThreadName=getName();
        this.pushWorkerMgr=_pushWorkerMgr;
        this.USE_FATIGUE = _pushWorkerMgr.getUSE_FATIGUE().equalsIgnoreCase("Y")?true:false;
    }
    public void run(){
    	boolean whiteFilter = !UmsInitListener.getWHITELIST_TARGET().isEmpty();

        while(isRun){
            ///push 전송 로직
            long umsSeqno = 0;
            PushBasicProcessBean baseBean = null;
            try {
                baseBean = pushWorkerMgr.takeWork();
                if(baseBean == null) {
                	continue;
                }
                
                umsSeqno = baseBean.getMASTERTABLE_SEQNO();
                baseBean.setPROVIDER(PROVIDER);
                if(baseBean instanceof PushEachProcessBean) {
                    PushEachProcessBean prcsBean = (PushEachProcessBean)baseBean;
                    
                    // 화이트 리스트 체크
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(prcsBean.getCUID()) == false)) {
                    	logger.debug("화이트 리스트 필터로 인한 무시 처리:"+prcsBean.getCUID());
                    	return;
                    }
                    
                    // 발송 전 취소요청 확인 취소가 들어온 UMS_SEQNO일 경우 취소로 인한 실패처리로직 구현
                    if(pushWorkerMgr.getCancleManager().isCancleUmsSeqno(""+prcsBean.getMASTERTABLE_SEQNO())){
                        UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                        umsResultBaseBean.setPROVIDER(PROVIDER);
                        umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                        umsResultBaseBean.setCUID(prcsBean.getCUID());
                        umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                        umsResultBaseBean.setERRCODE(ErrorManager.ERR_5005);
                        umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5005));
                        umsResultBaseBean.setSEND_RESULT("FF"); // 최종실패. 대체발송 없음.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("Y");
                        // STEP 1 : 원장발송카운트 실패처리로 등록
                        pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno, SendType.PUSH,true);
                        // STEP 2 : 푸시발송 상세 실패정보 입력 + 일별/시스템별/... 통계정보 처리
                        pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    // 피로도 체크
                    if(isFatigue(baseBean)){
                        boolean isSendAble = pushWorkerMgr.getUmsSendCommonService().chkFatigue(prcsBean.getCUID());
                        if(!isSendAble){
                            // 피로도 체크에 의한 실패처리
                            UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                            umsResultBaseBean.setPROVIDER(PROVIDER);
                            umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                            umsResultBaseBean.setCUID(prcsBean.getCUID());
                            umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                            umsResultBaseBean.setERRCODE(ErrorManager.ERR_5006);
                            umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5006));
                            umsResultBaseBean.setSEND_RESULT("FF"); // 최종실패. 대체발송 없음.
                            umsResultBaseBean.setSUCC_STATUS("0");
                            umsResultBaseBean.setPROCESS_END("Y");
                            // STEP 1 : 원장발송카운트 실패처리로 등록
                            pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                            // STEP 2 : 푸시발송 상세 실패정보 입력 + 일별/시스템별/... 통계정보 처리
                            pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                            continue;
                        }
                    }
                    
                    // 발송 유효 시간 체크
                    if (sendTimeCheck(prcsBean.getMIN_START_TIME(), prcsBean.getMAX_END_TIME()) == false) {
                    	UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                        umsResultBaseBean.setPROVIDER(PROVIDER);
                        umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                        umsResultBaseBean.setCUID(prcsBean.getCUID());
                        umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                        umsResultBaseBean.setERRCODE(ErrorManager.ERR_5008);
                        umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5008));                        
                        umsResultBaseBean.setSEND_RESULT("FF"); // 최종실패. 대체발송 없음.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("Y");
                        // STEP 1 : 원장발송카운트 실패처리로 등록
                        pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                        // STEP 2 : 푸시발송 상세 실패정보 입력 + 일별/시스템별/... 통계정보 처리
                        pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    String prefix = "P"+ (baseBean.getUMS_MSG_TYPE().equals("I")?"I":"A") + (baseBean.getTRANS_TYPE() == TransType.BATCH?"B":"R");
                    prcsBean.setCUST_KEY(prefix+"_"+prcsBean.getMASTERTABLE_SEQNO()+"_"+prcsBean.getTRANSACTION_KEY());
                    httpPushEachSend(prcsBean);

                }else if(baseBean instanceof PushFailProcessBean){
                    PushFailProcessBean pushFailProcessBean = (PushFailProcessBean)baseBean;
                    
                    // 화이트 리스트 체크
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(pushFailProcessBean.getCUID()) == false)) {
                    	logger.debug("화이트 리스트 필터로 인한 무시 처리:"+pushFailProcessBean.getCUID());
                    	return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushFailProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FS"); // 실패 후 대체발송요청
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    if(pushFailProcessBean.isPROCESS_END()){
                        umsResultBaseBean.setSEND_RESULT("FF");
                        umsResultBaseBean.setPROCESS_END("Y");
                    }
                    // STEP 1 : 발송카운트 실패처리로 등록
                    pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH, pushFailProcessBean.isPROCESS_END());
                    // STEP 2 : 푸시발송 상세 실패정보 입력
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }else if(baseBean instanceof PushNotSendFailProcessBean) {
                    PushNotSendFailProcessBean pushNotSendFailProcessBean = (PushNotSendFailProcessBean)baseBean;
                    
                    // 화이트 리스트 체크
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(pushNotSendFailProcessBean.getCUID()) == false)) {
                    	logger.debug("화이트 리스트 필터로 인한 무시 처리:"+pushNotSendFailProcessBean.getCUID());
                    	return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushNotSendFailProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushNotSendFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushNotSendFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushNotSendFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushNotSendFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushNotSendFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FF"); //최종실패
                    umsResultBaseBean.setRESULTMSG(pushNotSendFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setMASTERTABLE_SEQNO(umsSeqno);
                    umsResultBaseBean.setPROCESS_END("Y");

                    // STEP 1 : 발송카운트 실패처리로 등록
                    pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH, true);
                    // 확인 :  발송카운트에 등록하지 않았으므로 발송카운터 매니저에 등록할 필요 없다.
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }
                baseBean = null;  // young GC 대상으로 만들기 위해
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException 발생");
//                break;
            }catch (Exception e){
            	e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

	private boolean isFatigue(BaseProcessBean baseBean) {
		boolean isFatigue = false;
		if ("Y".equals(baseBean.getFATIGUE_YN()) || USE_FATIGUE ) isFatigue = true;
		if(baseBean.getUMS_MSG_TYPE().equalsIgnoreCase("I")) isFatigue = false;
		return isFatigue;
	}
	
    private boolean sendTimeCheck(String start, String end) {

    	if(StringUtils.isNotBlank(start)) start = start.replaceAll("\\D", "");
    	if(StringUtils.isNotBlank(end)) end = end.replaceAll("\\D", "");
    	int minTime = StringUtils.isBlank(start)?0:Integer.parseInt(start);
    	int maxTime = StringUtils.isBlank(end)?0:Integer.parseInt(end);
    	if(minTime <= 0 && maxTime<=0 ) return true;
    	int now = Integer.parseInt(DATE_TIME_FORMAT.format(LocalTime.now())); 
    	if(minTime >0 && now < minTime) return false;
    	if(maxTime >0 && now > maxTime) return false;
    	
    	return true;
    }
    

    /**
     * 치환문자가 있는(#{아이디}, #{이름})메세지는 치환을 해서 보낸다. 또는 개인화 메세지
     */
    private void httpPushEachSend(final PushEachProcessBean pushEachProcessBean){
        boolean isPushSendOK = false;
        String upmcResultMSG = "SUCCESS";
        String pushSendMsg = pushEachProcessBean.getMESSAGE();
        String upmcResultCode = "0000";

        String transactionKey = pushEachProcessBean.getTRANSACTION_KEY();
        try {
            Map<String, Object> postParam = makeParamMap(pushEachProcessBean);
            pushSendMsg = postParam.get("MESSAGE").toString();
            String conUpmcHost = pushWorkerMgr.getTcpAliveConManager().getConHostName();
            final String calUri = conUpmcHost + "/rcv_register_message.ctl";
            Map<String, String> httpHeadParam = new HashMap<String, String>();
            httpHeadParam.put("Content-Typ", "application/x-www-form-urlencoded");
            ResponseBean responseBean = HttpPoolClient.getInstance().sendPost(calUri, httpHeadParam, postParam);

            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                logger.debug("[PUSH] upmc respons data : {}", responseBean.getBody());
                Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), HashMap.class);
                Map<String, String> upmcHeadMap = (Map<String, String>) responseMap.get("HEADER");
                Map<String, Object> upmcBodyMap = (Map<String, Object>) responseMap.get("BODY");

                upmcResultCode = upmcHeadMap.get("RESULT_CODE");
                upmcResultMSG = "[UPMC PUSH ERR MESSAGE] ("+upmcResultCode+")"+upmcHeadMap.get("RESULT_BODY");

                if ("0000".equals(upmcResultCode)) { // code값이 어떨때는 Double 숫자이고 어떨때는 String이기 때문 startWith로 처리.
                    isPushSendOK = true;  // 푸시 발송 성공으로 셋팅

                    String upmcSeqno = upmcBodyMap.get("SENDMSG_SEQNO").toString();
                    
                    // 각 유저아이디별 발송상세 저장.
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushEachProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setSEND_TYPE_SEQCODE(transactionKey);
                    umsResultBaseBean.setSEND_RESULT("RS");
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setRESULTMSG("발송요청성공");
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushEachProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushEachProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setSEND_MSG(pushSendMsg);
                    if(!"".equals(pushEachProcessBean.getMSG_VARS())) {
                        umsResultBaseBean.setMSG_VARS(pushEachProcessBean.getMSG_VARS());
                    }

                    //푸시 발송상세 성공으로 정보 등록
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean);
            		if(isFatigue(pushEachProcessBean) && umsResultBaseBean.getSEND_RESULT().equals("RS")) {
            			pushWorkerMgr.getUmsSendCommonService().upFatigue(pushEachProcessBean.getCUID(), false);                			
            		}
                    //성공카운트 처리
                    pushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.PUSH);
                }else{
                    //UPMC 응답에러
                    upmcResultCode = "UPMC_"+upmcResultCode;
                    if(upmcResultMSG.getBytes().length>800){
                        upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody().substring(0,600)+"...";
                    }else{
                        upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody();
                    }
                }
            } else {
                // Http 프로토콜 에러
                upmcResultCode = ErrorManager.UPMC_ERR_9004;
                if(responseBean.getBody().getBytes().length>800){
                    upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody().substring(0,600)+"...";
                }else{
                    upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody();
                }

            }
        }catch (Exception e){
            upmcResultCode = ErrorManager.UPMC_ERR_9004;
            // UPMC에 연결이 안되는 에러이므로 http 응답코드를 받지 못한경우
            upmcResultMSG = "UPMC 연결 가능 한 세션이 없습니다.";
            logger.error("[Each UMS ==> UPMC] error : {}",e.toString());
        }
        // UPMC와 통신에러로 인한 푸시발송을 실패했을 경우 대체발송설정을 확인하여 처리.
        if(!isPushSendOK){ // 실패처리

            //STEP 1 : UPMC와 통신에러시 대체발송을 하겠다고 설정 했을 경우 확인. UPMC 장애시 사용자의 의도와 상관없이 모두 발송하여 알림톡,SMS 엄청남 비용이 발생할수 있으므로 설정확인.
            boolean isProcessEnd = true;
            if("Y".equals(pushWorkerMgr.getErrOtherSend())){  //UPMC와 통신에러시 대체발송을 하겠다고 설정 했을 경우.
                isProcessEnd = false; //process 진행상태가 끝나지 않았다고 셋팅
            }

            UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushEachProcessBean);
            umsResultBaseBean.setPROVIDER(PROVIDER);
            umsResultBaseBean.setSEND_TYPE_SEQCODE(transactionKey);
            umsResultBaseBean.setSEND_RESULT("FF"); // 최종실패. 대체발송없음.
            umsResultBaseBean.setSUCC_STATUS("0");
            umsResultBaseBean.setERRCODE(upmcResultCode);
            umsResultBaseBean.setRESULTMSG(upmcResultMSG);
            umsResultBaseBean.setCNAME(pushEachProcessBean.getCNAME());
            umsResultBaseBean.setCUID(pushEachProcessBean.getCUID());
            umsResultBaseBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
            umsResultBaseBean.setSEND_MSG(pushSendMsg);
            umsResultBaseBean.setMSG_VARS(pushEachProcessBean.getMSG_VARS());
            umsResultBaseBean.setPROCESS_END("Y");
            if(isProcessEnd) {
                //STEP 2 :UPMC 통신결과 대체발송 하겠다고 설정 했을 경우. 실패카운트를 증가 시켜 프로세스 완료시킴.
                pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
            }else{
                // 대체발송 처리
                try {
                    if(pushEachProcessBean.getFAIL_RETRY_SENDTYPE() == null || pushEachProcessBean.getFAIL_RETRY_SENDTYPE().size()==0){
                        // 대체발송메세지를 설정하지 않아 최종 실패처리
                        pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
                    }else{
                        umsResultBaseBean.setSEND_RESULT("RS"); // 실패처리. 대체발송 있음.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("N");
                        //대체발송 처리 - 대체발송은 발송 성공으로 지정하며 T_UMS_LOG에 기록하여 Agent에서 대체 발송 판단 및 후 처리 한다.
                        //sendDaeche(pushEachProcessBean);
                        pushWorkerMgr.getUmsDao().inUmsLog(umsResultBaseBean);
                        //pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,false);
                        pushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.PUSH);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    umsResultBaseBean.setPROCESS_END("Y"); //UPMC에러에 대해서는 대체발송을 하지 않는다고 셋팅했으므로 최종 실패로 셋팅.
                    pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
                    logger.error("#[PushEachProcessBean] UPMC ERROR로 인해 대체발송처리하였으나 에러 : {}",e.toString());
                }
            }
            // STEP 3 : UPMC로 발송실패로  fail Result로 DB에 등록한다.
            try {
                pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean); // 푸시발송(실패) 결과처리큐에 담음.
            } catch (Exception e) {
                logger.error(e.toString());
                e.printStackTrace();
            }

        }
        // 처리속도를 알기 위해 TPS매니저에 처리 카운트 증가시킴.
        TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.PUSH);
    }

    private Map<String, Object> makeParamMap(PushEachProcessBean pushEachProcessBean) throws Exception{
        Map<String, Object> postParam = new HashMap<String, Object>();
        // 공통영역 셋팅
        postParam.put("APP_ID", pushEachProcessBean.getAPP_ID());
        postParam.put("TYPE", pushEachProcessBean.getPUSH_TYPE());
        postParam.put("PRIORITY",pushEachProcessBean.getPRIORITY());
        if(pushEachProcessBean.getTRANS_TYPE()== TransType.BATCH){
            postParam.put("PRIORITY","1"); //푸시 5.1에서는 배치메세지로 처리함.
        }
        postParam.put("RESERVEDATE",pushEachProcessBean.getRESERVEDATE());
        postParam.put("SERVICECODE",pushEachProcessBean.getSERVICECODE());
        postParam.put("SENDERCODE",pushEachProcessBean.getSENDERCODE());
        postParam.put("DB_IN",pushEachProcessBean.getDB_IN());
        postParam.put("PUSH_FAIL_SMS_SEND", pushEachProcessBean.getPUSH_FAIL_SMS_SEND());
        postParam.put("SMS_READ_WAIT_MINUTE",pushEachProcessBean.getPUSH_FAIL_WAIT_MIN());
        postParam.put("EXT",pushEachProcessBean.getEXT());
        postParam.put("TEMPLATE_YN", "Y");
        postParam.put("BADGENO",pushEachProcessBean.getBADGENO());
        // 푸시5.1버전 추가파라미터.
        postParam.put("CUST_KEY",pushEachProcessBean.getCUST_KEY());
        postParam.put("CUST_VAR1",pushEachProcessBean.getCUST_VAR1());
        postParam.put("CUST_VAR2",pushEachProcessBean.getCUST_VAR2());
        postParam.put("CUST_VAR3",pushEachProcessBean.getCUST_VAR3());

        // 푸시 메세지 생성
        String pushBodyMsg = pushEachProcessBean.getMESSAGE();
        String pushTitle = pushEachProcessBean.getTITLE();
        String extMsg = pushEachProcessBean.getEXT();
        boolean isReplaceExt = false;
        // EXT 정보 치환처리
        if(StringUtils.isNotBlank(extMsg) && extMsg.indexOf("#{")>-1){
            isReplaceExt = true;
        }

        if(pushBodyMsg.indexOf("#{")>-1) {
            pushBodyMsg = pushBodyMsg.replace("#{아이디}", pushEachProcessBean.getCUID());
            // 푸시대상자에서 발송일 경우 핸드폰 번호가 없어서 이름이 빈값이다. 푸시에 치환처리 할 수 있도록 빈값일 경우는 치환하지 않는다.
            if(!"".equals(pushEachProcessBean.getCNAME())){
                pushBodyMsg = pushBodyMsg.replace("#{이름}",pushEachProcessBean.getCNAME());
            }else{
                pushBodyMsg = pushBodyMsg.replace("#{이름}","%CNAME%");
            }

            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // 개인화 메세지 치환처리
                Map<String, String> personalMap = pushEachProcessBean.getMSG_VAR_MAP();
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    pushBodyMsg = pushBodyMsg.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
        }
        String pushSendMsg = pushBodyMsg;

        if(!"".equals(pushTitle) ){  //푸시 제목이 있을 경우 {"title":"","body":""} 형태로 발송
            String pushTitleMsg = pushTitle;
            if(pushTitle.indexOf("#{")>-1) {
                pushTitleMsg = pushTitle.replace("#{이름}", pushEachProcessBean.getCNAME()).replace("#{아이디}", pushEachProcessBean.getCUID());
            }
            Map<String,String> alertMap = new HashMap<>();
            alertMap.put("title",pushTitleMsg);
            alertMap.put("body",pushBodyMsg);
            pushSendMsg = gson.toJson(alertMap);
        }
        postParam.put("MESSAGE", pushSendMsg);
        postParam.put("CUID",pushEachProcessBean.getCUID());

        if(isReplaceExt){
            extMsg= extMsg.replace("#{아이디}", pushEachProcessBean.getCUID());
            extMsg= extMsg.replace("#{이름}", pushEachProcessBean.getCNAME());
            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // 개인화 메세지 치환처리
                Map<String, String> personalMap = pushEachProcessBean.getMSG_VAR_MAP();
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    extMsg= extMsg.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
            postParam.put("EXT", extMsg);
        }
        return postParam;
    }

    private void sendDaeche(PushEachProcessBean pushEachProcessBean) throws Exception{
        //STEP 2 : 최종 실패카운트는 올리지 않고 대체발송처리 하였으므로 DB에 PUSH실패카운트, 대체발송 카운트 수 업데이트 시킴
        Set<SendType> daecheSendTypes = pushEachProcessBean.getFAIL_RETRY_SENDTYPE();
        for(SendType sendType : daecheSendTypes){
            if(sendType==SendType.PUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                continue;
            }
            // UMS3.0 실패 카운트 로직 변경 필요
            String provider = null;
            switch (sendType) {
                case WPUSH:
                    // 대체발송이 웹푸시일 경우 웹푸시용 PushEachProcessBean으로 만든다.
                    PushEachProcessBean wpushEachProcessBean = makeDaeCheWPushEachProcessBean(pushEachProcessBean);
                    wpushEachProcessBean.setCUID(pushEachProcessBean.getCUID());
                    wpushEachProcessBean.setCNAME(pushEachProcessBean.getCNAME());
                    wpushEachProcessBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());

                    wpushEachProcessBean.setSPLIT_MSG_CNT(pushEachProcessBean.getSPLIT_MSG_CNT());
                    wpushEachProcessBean.setDELAY_SECOND(pushEachProcessBean.getDELAY_SECOND());
                    wpushEachProcessBean.setALT_REPLACE_VARS(pushEachProcessBean.getALT_REPLACE_VARS());
                    pushWorkerMgr.getwPushWorkerMgrPool().putWork(wpushEachProcessBean); // 웹푸시 발송프로세스 발송큐에 담음.
                    break;
                case KKOALT:
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseKkoAltSendService altSendService = pushWorkerMgr.getUmsChannelProvierFactory().getKkoAltProviderService(provider);
                    altSendService.umsKkoAllimTolkSend(pushEachProcessBean);
                    break;
                case KKOFRT:
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseKkoFrtSendService frtSendService = pushWorkerMgr.getUmsChannelProvierFactory().getKkoFrtProviderService(provider);
                    frtSendService.umsKkoFriendTolkSend(pushEachProcessBean);
                    break;
                case SMS:
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseSmsSendService smsSendService = pushWorkerMgr.getUmsChannelProvierFactory().getSmsProviderService(provider);
                    smsSendService.umsSmsSend(pushEachProcessBean);
                    break;
                case LMS: case MMS:
                    provider = pushWorkerMgr.getAllotterManager().getProvider("MMS", pushEachProcessBean.getMOBILE_NUM());
                    BaseMmsSendService mmsSendService = pushWorkerMgr.getUmsChannelProvierFactory().getMmsProviderService(provider);
                    mmsSendService.umsMmsSend(pushEachProcessBean);
                    break;
                case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseRcsSendService rcsSendService = pushWorkerMgr.getUmsChannelProvierFactory().getRcsProviderService(provider);
                    rcsSendService.umsReSend(pushEachProcessBean);
                    break;
                case NAVERT:
                    //NAVERT 발송서비스처리 구현
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseNaverSendService naverSendService = pushWorkerMgr.getUmsChannelProvierFactory().getNaverProviderService(provider);
                    naverSendService.umsNaverSend(pushEachProcessBean);
                    break;
            }
        }
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }

    // 푸시 발송결과 만들기
    private UmsResultBaseBean makeUmsResultBaseBean(PushBasicProcessBean pushBasicProcessBean){
        UmsResultBaseBean umsResultBaseBean = new UmsResultBaseBean(pushBasicProcessBean.getTRANS_TYPE(),SendType.PUSH);
        umsResultBaseBean.setPROVIDER(PROVIDER);
        umsResultBaseBean.setROOT_CHANNEL_YN(pushBasicProcessBean.getROOT_CHANNEL_YN()); // 대체발송에 의한 발송여부 셋팅.
        umsResultBaseBean.setAPP_ID(pushBasicProcessBean.getAPP_ID());
        umsResultBaseBean.setSENDERID(pushBasicProcessBean.getSENDERID());
        umsResultBaseBean.setSENDERGROUPCODE(pushBasicProcessBean.getSENDGROUPCODE());
        umsResultBaseBean.setMASTERTABLE_SEQNO(pushBasicProcessBean.getMASTERTABLE_SEQNO());
        umsResultBaseBean.setSEND_MSG(pushBasicProcessBean.getMESSAGE());
        umsResultBaseBean.setMSG_VARS(pushBasicProcessBean.getMSG_VARS());
        umsResultBaseBean.setSEND_TITLE(pushBasicProcessBean.getTITLE());
        return umsResultBaseBean;
    }

    private PushEachProcessBean makeDaeCheWPushEachProcessBean(PushEachProcessBean eachPrcsBean){
        // 푸시 실패시 웹푸시대체발송 데이타 만들기
        PushEachProcessBean newEachPrcsBean = new PushEachProcessBean();
        newEachPrcsBean.setTRANS_TYPE(eachPrcsBean.getTRANS_TYPE());
        newEachPrcsBean.setROOT_CHANNEL_YN("N"); // 대체발송으로 발송된다고 셋팅.
        newEachPrcsBean.setMASTERTABLE_SEQNO(eachPrcsBean.getMASTERTABLE_SEQNO());
        newEachPrcsBean.setSENDGROUPCODE(eachPrcsBean.getSENDGROUPCODE());
        newEachPrcsBean.setSENDERID(eachPrcsBean.getSENDERID());
        newEachPrcsBean.setMIN_START_TIME(eachPrcsBean.getMIN_START_TIME());
        newEachPrcsBean.setMAX_END_TIME(eachPrcsBean.getMAX_END_TIME());
        newEachPrcsBean.setFATIGUE_YN(eachPrcsBean.getFATIGUE_YN());
        newEachPrcsBean.setWPUSH_DOMAIN(eachPrcsBean.getWPUSH_DOMAIN());
        newEachPrcsBean.setWPUSH_TITLE(eachPrcsBean.getWPUSH_TITLE());
        newEachPrcsBean.setWPUSH_MSG(eachPrcsBean.getWPUSH_MSG());
        newEachPrcsBean.setWPUSH_EXT(eachPrcsBean.getWPUSH_EXT());
        newEachPrcsBean.setWPUSH_BADGENO(eachPrcsBean.getWPUSH_BADGENO());
        newEachPrcsBean.setWPUSH_ICON(eachPrcsBean.getWPUSH_ICON());
        newEachPrcsBean.setWPUSH_LINK(eachPrcsBean.getWPUSH_LINK());

        newEachPrcsBean.setPUSH_TYPE(eachPrcsBean.getPUSH_TYPE());
        newEachPrcsBean.setBADGENO(eachPrcsBean.getBADGENO());
        newEachPrcsBean.setPRIORITY(eachPrcsBean.getPRIORITY());
        newEachPrcsBean.setSERVICECODE(eachPrcsBean.getSERVICECODE());
        newEachPrcsBean.setSENDERCODE(eachPrcsBean.getSENDERCODE());
        newEachPrcsBean.setDB_IN(eachPrcsBean.getDB_IN());
        newEachPrcsBean.setPUSH_FAIL_SMS_SEND(eachPrcsBean.getPUSH_FAIL_SMS_SEND());
        newEachPrcsBean.setPUSH_FAIL_WAIT_MIN(eachPrcsBean.getPUSH_FAIL_WAIT_MIN());
        if(eachPrcsBean.getFAIL_RETRY_SENDTYPE2()!=null && eachPrcsBean.getFAIL_RETRY_SENDTYPE2().size()>0){
            newEachPrcsBean.setFAIL_RETRY_SENDTYPE(eachPrcsBean.getFAIL_RETRY_SENDTYPE2());
        }
        newEachPrcsBean.setCUST_KEY(eachPrcsBean.getCUST_KEY());
        newEachPrcsBean.setCUST_VAR1(eachPrcsBean.getCUST_VAR1());
        newEachPrcsBean.setCUST_VAR2(eachPrcsBean.getCUST_VAR2());
        newEachPrcsBean.setCUST_VAR3(eachPrcsBean.getCUST_VAR3());
        newEachPrcsBean.setMSG_VAR_MAP(eachPrcsBean.getMSG_VAR_MAP());
        newEachPrcsBean.setMSG_VARS(eachPrcsBean.getMSG_VARS());
        return newEachPrcsBean;
    }
}
