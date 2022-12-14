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
            ///push ?????? ??????
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
                    
                    // ????????? ????????? ??????
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(prcsBean.getCUID()) == false)) {
                    	logger.debug("????????? ????????? ????????? ?????? ?????? ??????:"+prcsBean.getCUID());
                    	return;
                    }
                    
                    // ?????? ??? ???????????? ?????? ????????? ????????? UMS_SEQNO??? ?????? ????????? ?????? ?????????????????? ??????
                    if(pushWorkerMgr.getCancleManager().isCancleUmsSeqno(""+prcsBean.getMASTERTABLE_SEQNO())){
                        UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                        umsResultBaseBean.setPROVIDER(PROVIDER);
                        umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                        umsResultBaseBean.setCUID(prcsBean.getCUID());
                        umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                        umsResultBaseBean.setERRCODE(ErrorManager.ERR_5005);
                        umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5005));
                        umsResultBaseBean.setSEND_RESULT("FF"); // ????????????. ???????????? ??????.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("Y");
                        // STEP 1 : ????????????????????? ??????????????? ??????
                        pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno, SendType.PUSH,true);
                        // STEP 2 : ???????????? ?????? ???????????? ?????? + ??????/????????????/... ???????????? ??????
                        pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    // ????????? ??????
                    if(isFatigue(baseBean)){
                        boolean isSendAble = pushWorkerMgr.getUmsSendCommonService().chkFatigue(prcsBean.getCUID());
                        if(!isSendAble){
                            // ????????? ????????? ?????? ????????????
                            UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                            umsResultBaseBean.setPROVIDER(PROVIDER);
                            umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                            umsResultBaseBean.setCUID(prcsBean.getCUID());
                            umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                            umsResultBaseBean.setERRCODE(ErrorManager.ERR_5006);
                            umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5006));
                            umsResultBaseBean.setSEND_RESULT("FF"); // ????????????. ???????????? ??????.
                            umsResultBaseBean.setSUCC_STATUS("0");
                            umsResultBaseBean.setPROCESS_END("Y");
                            // STEP 1 : ????????????????????? ??????????????? ??????
                            pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                            // STEP 2 : ???????????? ?????? ???????????? ?????? + ??????/????????????/... ???????????? ??????
                            pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                            continue;
                        }
                    }
                    
                    // ?????? ?????? ?????? ??????
                    if (sendTimeCheck(prcsBean.getMIN_START_TIME(), prcsBean.getMAX_END_TIME()) == false) {
                    	UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(prcsBean);
                        umsResultBaseBean.setPROVIDER(PROVIDER);
                        umsResultBaseBean.setCNAME(prcsBean.getCNAME());
                        umsResultBaseBean.setCUID(prcsBean.getCUID());
                        umsResultBaseBean.setMOBILE_NUM(prcsBean.getMOBILE_NUM());
                        umsResultBaseBean.setERRCODE(ErrorManager.ERR_5008);
                        umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5008));                        
                        umsResultBaseBean.setSEND_RESULT("FF"); // ????????????. ???????????? ??????.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("Y");
                        // STEP 1 : ????????????????????? ??????????????? ??????
                        pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                        // STEP 2 : ???????????? ?????? ???????????? ?????? + ??????/????????????/... ???????????? ??????
                        pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    String prefix = "P"+ (baseBean.getUMS_MSG_TYPE().equals("I")?"I":"A") + (baseBean.getTRANS_TYPE() == TransType.BATCH?"B":"R");
                    prcsBean.setCUST_KEY(prefix+"_"+prcsBean.getMASTERTABLE_SEQNO()+"_"+prcsBean.getTRANSACTION_KEY());
                    httpPushEachSend(prcsBean);

                }else if(baseBean instanceof PushFailProcessBean){
                    PushFailProcessBean pushFailProcessBean = (PushFailProcessBean)baseBean;
                    
                    // ????????? ????????? ??????
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(pushFailProcessBean.getCUID()) == false)) {
                    	logger.debug("????????? ????????? ????????? ?????? ?????? ??????:"+pushFailProcessBean.getCUID());
                    	return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushFailProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FS"); // ?????? ??? ??????????????????
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    if(pushFailProcessBean.isPROCESS_END()){
                        umsResultBaseBean.setSEND_RESULT("FF");
                        umsResultBaseBean.setPROCESS_END("Y");
                    }
                    // STEP 1 : ??????????????? ??????????????? ??????
                    pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH, pushFailProcessBean.isPROCESS_END());
                    // STEP 2 : ???????????? ?????? ???????????? ??????
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }else if(baseBean instanceof PushNotSendFailProcessBean) {
                    PushNotSendFailProcessBean pushNotSendFailProcessBean = (PushNotSendFailProcessBean)baseBean;
                    
                    // ????????? ????????? ??????
                    if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(pushNotSendFailProcessBean.getCUID()) == false)) {
                    	logger.debug("????????? ????????? ????????? ?????? ?????? ??????:"+pushNotSendFailProcessBean.getCUID());
                    	return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushNotSendFailProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushNotSendFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushNotSendFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushNotSendFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushNotSendFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushNotSendFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FF"); //????????????
                    umsResultBaseBean.setRESULTMSG(pushNotSendFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setMASTERTABLE_SEQNO(umsSeqno);
                    umsResultBaseBean.setPROCESS_END("Y");

                    // STEP 1 : ??????????????? ??????????????? ??????
                    pushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH, true);
                    // ?????? :  ?????????????????? ???????????? ??????????????? ??????????????? ???????????? ????????? ?????? ??????.
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }
                baseBean = null;  // young GC ???????????? ????????? ??????
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException ??????");
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
     * ??????????????? ??????(#{?????????}, #{??????})???????????? ????????? ?????? ?????????. ?????? ????????? ?????????
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

                if ("0000".equals(upmcResultCode)) { // code?????? ???????????? Double ???????????? ???????????? String?????? ?????? startWith??? ??????.
                    isPushSendOK = true;  // ?????? ?????? ???????????? ??????

                    String upmcSeqno = upmcBodyMap.get("SENDMSG_SEQNO").toString();
                    
                    // ??? ?????????????????? ???????????? ??????.
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushEachProcessBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setSEND_TYPE_SEQCODE(transactionKey);
                    umsResultBaseBean.setSEND_RESULT("RS");
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setRESULTMSG("??????????????????");
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushEachProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushEachProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setSEND_MSG(pushSendMsg);
                    if(!"".equals(pushEachProcessBean.getMSG_VARS())) {
                        umsResultBaseBean.setMSG_VARS(pushEachProcessBean.getMSG_VARS());
                    }

                    //?????? ???????????? ???????????? ?????? ??????
                    pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean);
            		if(isFatigue(pushEachProcessBean) && umsResultBaseBean.getSEND_RESULT().equals("RS")) {
            			pushWorkerMgr.getUmsSendCommonService().upFatigue(pushEachProcessBean.getCUID(), false);                			
            		}
                    //??????????????? ??????
                    pushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.PUSH);
                }else{
                    //UPMC ????????????
                    upmcResultCode = "UPMC_"+upmcResultCode;
                    if(upmcResultMSG.getBytes().length>800){
                        upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody().substring(0,600)+"...";
                    }else{
                        upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody();
                    }
                }
            } else {
                // Http ???????????? ??????
                upmcResultCode = ErrorManager.UPMC_ERR_9004;
                if(responseBean.getBody().getBytes().length>800){
                    upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody().substring(0,600)+"...";
                }else{
                    upmcResultMSG = "[UPMC HTTP SERVER MESSAGE] "+responseBean.getBody();
                }

            }
        }catch (Exception e){
            upmcResultCode = ErrorManager.UPMC_ERR_9004;
            // UPMC??? ????????? ????????? ??????????????? http ??????????????? ?????? ????????????
            upmcResultMSG = "UPMC ?????? ?????? ??? ????????? ????????????.";
            logger.error("[Each UMS ==> UPMC] error : {}",e.toString());
        }
        // UPMC??? ??????????????? ?????? ??????????????? ???????????? ?????? ????????????????????? ???????????? ??????.
        if(!isPushSendOK){ // ????????????

            //STEP 1 : UPMC??? ??????????????? ??????????????? ???????????? ?????? ?????? ?????? ??????. UPMC ????????? ???????????? ????????? ???????????? ?????? ???????????? ?????????,SMS ????????? ????????? ???????????? ???????????? ????????????.
            boolean isProcessEnd = true;
            if("Y".equals(pushWorkerMgr.getErrOtherSend())){  //UPMC??? ??????????????? ??????????????? ???????????? ?????? ?????? ??????.
                isProcessEnd = false; //process ??????????????? ????????? ???????????? ??????
            }

            UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(pushEachProcessBean);
            umsResultBaseBean.setPROVIDER(PROVIDER);
            umsResultBaseBean.setSEND_TYPE_SEQCODE(transactionKey);
            umsResultBaseBean.setSEND_RESULT("FF"); // ????????????. ??????????????????.
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
                //STEP 2 :UPMC ???????????? ???????????? ???????????? ?????? ?????? ??????. ?????????????????? ?????? ?????? ???????????? ????????????.
                pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
            }else{
                // ???????????? ??????
                try {
                    if(pushEachProcessBean.getFAIL_RETRY_SENDTYPE() == null || pushEachProcessBean.getFAIL_RETRY_SENDTYPE().size()==0){
                        // ???????????????????????? ???????????? ?????? ?????? ????????????
                        pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
                    }else{
                        umsResultBaseBean.setSEND_RESULT("RS"); // ????????????. ???????????? ??????.
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("N");
                        //???????????? ?????? - ??????????????? ?????? ???????????? ???????????? T_UMS_LOG??? ???????????? Agent?????? ?????? ?????? ?????? ??? ??? ?????? ??????.
                        //sendDaeche(pushEachProcessBean);
                        pushWorkerMgr.getUmsDao().inUmsLog(umsResultBaseBean);
                        //pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,false);
                        pushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.PUSH);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    umsResultBaseBean.setPROCESS_END("Y"); //UPMC????????? ???????????? ??????????????? ?????? ???????????? ?????????????????? ?????? ????????? ??????.
                    pushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.PUSH,true);
                    logger.error("#[PushEachProcessBean] UPMC ERROR??? ?????? ?????????????????????????????? ?????? : {}",e.toString());
                }
            }
            // STEP 3 : UPMC??? ???????????????  fail Result??? DB??? ????????????.
            try {
                pushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean); // ????????????(??????) ?????????????????? ??????.
            } catch (Exception e) {
                logger.error(e.toString());
                e.printStackTrace();
            }

        }
        // ??????????????? ?????? ?????? TPS???????????? ?????? ????????? ????????????.
        TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.PUSH);
    }

    private Map<String, Object> makeParamMap(PushEachProcessBean pushEachProcessBean) throws Exception{
        Map<String, Object> postParam = new HashMap<String, Object>();
        // ???????????? ??????
        postParam.put("APP_ID", pushEachProcessBean.getAPP_ID());
        postParam.put("TYPE", pushEachProcessBean.getPUSH_TYPE());
        postParam.put("PRIORITY",pushEachProcessBean.getPRIORITY());
        if(pushEachProcessBean.getTRANS_TYPE()== TransType.BATCH){
            postParam.put("PRIORITY","1"); //?????? 5.1????????? ?????????????????? ?????????.
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
        // ??????5.1?????? ??????????????????.
        postParam.put("CUST_KEY",pushEachProcessBean.getCUST_KEY());
        postParam.put("CUST_VAR1",pushEachProcessBean.getCUST_VAR1());
        postParam.put("CUST_VAR2",pushEachProcessBean.getCUST_VAR2());
        postParam.put("CUST_VAR3",pushEachProcessBean.getCUST_VAR3());

        // ?????? ????????? ??????
        String pushBodyMsg = pushEachProcessBean.getMESSAGE();
        String pushTitle = pushEachProcessBean.getTITLE();
        String extMsg = pushEachProcessBean.getEXT();
        boolean isReplaceExt = false;
        // EXT ?????? ????????????
        if(StringUtils.isNotBlank(extMsg) && extMsg.indexOf("#{")>-1){
            isReplaceExt = true;
        }

        if(pushBodyMsg.indexOf("#{")>-1) {
            pushBodyMsg = pushBodyMsg.replace("#{?????????}", pushEachProcessBean.getCUID());
            // ????????????????????? ????????? ?????? ????????? ????????? ????????? ????????? ????????????. ????????? ???????????? ??? ??? ????????? ????????? ????????? ???????????? ?????????.
            if(!"".equals(pushEachProcessBean.getCNAME())){
                pushBodyMsg = pushBodyMsg.replace("#{??????}",pushEachProcessBean.getCNAME());
            }else{
                pushBodyMsg = pushBodyMsg.replace("#{??????}","%CNAME%");
            }

            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // ????????? ????????? ????????????
                Map<String, String> personalMap = pushEachProcessBean.getMSG_VAR_MAP();
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    pushBodyMsg = pushBodyMsg.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
        }
        String pushSendMsg = pushBodyMsg;

        if(!"".equals(pushTitle) ){  //?????? ????????? ?????? ?????? {"title":"","body":""} ????????? ??????
            String pushTitleMsg = pushTitle;
            if(pushTitle.indexOf("#{")>-1) {
                pushTitleMsg = pushTitle.replace("#{??????}", pushEachProcessBean.getCNAME()).replace("#{?????????}", pushEachProcessBean.getCUID());
            }
            Map<String,String> alertMap = new HashMap<>();
            alertMap.put("title",pushTitleMsg);
            alertMap.put("body",pushBodyMsg);
            pushSendMsg = gson.toJson(alertMap);
        }
        postParam.put("MESSAGE", pushSendMsg);
        postParam.put("CUID",pushEachProcessBean.getCUID());

        if(isReplaceExt){
            extMsg= extMsg.replace("#{?????????}", pushEachProcessBean.getCUID());
            extMsg= extMsg.replace("#{??????}", pushEachProcessBean.getCNAME());
            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // ????????? ????????? ????????????
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
        //STEP 2 : ?????? ?????????????????? ????????? ?????? ?????????????????? ??????????????? DB??? PUSH???????????????, ???????????? ????????? ??? ???????????? ??????
        Set<SendType> daecheSendTypes = pushEachProcessBean.getFAIL_RETRY_SENDTYPE();
        for(SendType sendType : daecheSendTypes){
            if(sendType==SendType.PUSH){ // ?????? ????????? ????????? ???????????? PUSH>PUSH ???????????? ?????? ??????????????? ????????? ?????? ???????????? ??????.
                continue;
            }
            // UMS3.0 ?????? ????????? ?????? ?????? ??????
            String provider = null;
            switch (sendType) {
                case WPUSH:
                    // ??????????????? ???????????? ?????? ???????????? PushEachProcessBean?????? ?????????.
                    PushEachProcessBean wpushEachProcessBean = makeDaeCheWPushEachProcessBean(pushEachProcessBean);
                    wpushEachProcessBean.setCUID(pushEachProcessBean.getCUID());
                    wpushEachProcessBean.setCNAME(pushEachProcessBean.getCNAME());
                    wpushEachProcessBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());

                    wpushEachProcessBean.setSPLIT_MSG_CNT(pushEachProcessBean.getSPLIT_MSG_CNT());
                    wpushEachProcessBean.setDELAY_SECOND(pushEachProcessBean.getDELAY_SECOND());
                    wpushEachProcessBean.setALT_REPLACE_VARS(pushEachProcessBean.getALT_REPLACE_VARS());
                    pushWorkerMgr.getwPushWorkerMgrPool().putWork(wpushEachProcessBean); // ????????? ?????????????????? ???????????? ??????.
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
                    //NAVERT ????????????????????? ??????
                    provider = pushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                    BaseNaverSendService naverSendService = pushWorkerMgr.getUmsChannelProvierFactory().getNaverProviderService(provider);
                    naverSendService.umsNaverSend(pushEachProcessBean);
                    break;
            }
        }
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }

    // ?????? ???????????? ?????????
    private UmsResultBaseBean makeUmsResultBaseBean(PushBasicProcessBean pushBasicProcessBean){
        UmsResultBaseBean umsResultBaseBean = new UmsResultBaseBean(pushBasicProcessBean.getTRANS_TYPE(),SendType.PUSH);
        umsResultBaseBean.setPROVIDER(PROVIDER);
        umsResultBaseBean.setROOT_CHANNEL_YN(pushBasicProcessBean.getROOT_CHANNEL_YN()); // ??????????????? ?????? ???????????? ??????.
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
        // ?????? ????????? ????????????????????? ????????? ?????????
        PushEachProcessBean newEachPrcsBean = new PushEachProcessBean();
        newEachPrcsBean.setTRANS_TYPE(eachPrcsBean.getTRANS_TYPE());
        newEachPrcsBean.setROOT_CHANNEL_YN("N"); // ?????????????????? ??????????????? ??????.
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
