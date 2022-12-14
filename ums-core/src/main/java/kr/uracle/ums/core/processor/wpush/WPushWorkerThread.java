package kr.uracle.ums.core.processor.wpush;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.processor.push.PushBasicProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.processor.push.PushFailProcessBean;
import kr.uracle.ums.core.processor.push.PushNotSendFailProcessBean;
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

public class WPushWorkerThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    
    private String ThreadName;
    private final WPushWorkerMgr wpushWorkerMgr;
    private boolean isRun = true;
    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    
    private final String PROVIDER = "URACLE";
    private final boolean USE_FATIGUE;
    public WPushWorkerThread(String name, WPushWorkerMgr _wpushWorkerMgr){
        super(name);
        this.ThreadName=getName();
        this.wpushWorkerMgr=_wpushWorkerMgr;
        USE_FATIGUE = _wpushWorkerMgr.getUSE_FATIGUE().equalsIgnoreCase("Y")?true:false;
    }
    public void run(){
    	boolean whiteFilter = !UmsInitListener.getWHITELIST_TARGET().isEmpty();
        while(isRun){
            ///push ?????? ??????
            long umsSeqno = 0;
            PushBasicProcessBean baseBean = null;
            try {
                baseBean = wpushWorkerMgr.takeWork();
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
                    if(wpushWorkerMgr.getCancleManager().isCancleUmsSeqno(""+prcsBean.getMASTERTABLE_SEQNO())){
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
                        // STEP 1 : ??????????????? ??????????????? ??????
                        wpushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.WPUSH,true);
                        // STEP 2 : ???????????? ?????? ???????????? ??????
                        wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    // ????????? ??????
                    if(isFatigue(baseBean)){
                        boolean isSendAble = wpushWorkerMgr.getUmsSendCommonService().chkFatigue(prcsBean.getCUID());
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
                            wpushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                            // STEP 2 : ???????????? ?????? ???????????? ?????? + ??????/????????????/... ???????????? ??????
                            wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                            continue;
                        }
                    }
                    // ?????? ?????? ?????? ??????
                    if (sendTimeCheck(baseBean.getMIN_START_TIME(), baseBean.getMAX_END_TIME()) == false) {
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
                        wpushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.PUSH,true);
                        // STEP 2 : ???????????? ?????? ???????????? ?????? + ??????/????????????/... ???????????? ??????
                        wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                        continue;
                    }

                    String prefix = "W"+ (baseBean.getUMS_MSG_TYPE().equals("I")?"I":"A") + (baseBean.getTRANS_TYPE() == TransType.BATCH?"B":"R");
                    prcsBean.setCUST_KEY(prefix+"_"+prcsBean.getMASTERTABLE_SEQNO()+"_"+prcsBean.getTRANSACTION_KEY());

                    httpPushEachSend(prcsBean);

                }else if(baseBean instanceof PushFailProcessBean){
                    PushFailProcessBean pushFailProcessBean = (PushFailProcessBean)baseBean;
                    
                    // ????????? ????????? ??????
                    if(whiteFilter) {
                    	logger.debug("????????? ????????? ????????? ?????? ?????? ??????:"+pushFailProcessBean.getCUID());
                    	if(UmsInitListener.checkWHITELIST_TARGET(pushFailProcessBean.getCUID()) == false) return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(baseBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FS"); // ?????? ??? ??????????????????
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setSUCC_STATUS("0");
                    if(pushFailProcessBean.isPROCESS_END()){
                        umsResultBaseBean.setPROCESS_END("Y");
                    }

                    // STEP 1 : ??????????????? ??????????????? ??????
                    wpushWorkerMgr.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(),umsSeqno,SendType.WPUSH,pushFailProcessBean.isPROCESS_END());
                    // STEP 2 : ???????????? ?????? ???????????? ??????
                    wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }else if(baseBean instanceof PushNotSendFailProcessBean) {
                    PushNotSendFailProcessBean pushNotSendFailProcessBean = (PushNotSendFailProcessBean)baseBean;
                    
                    // ????????? ????????? ??????
                    if(whiteFilter) {
                    	logger.debug("????????? ????????? ????????? ?????? ?????? ??????:"+pushNotSendFailProcessBean.getCUID());
                    	if(UmsInitListener.checkWHITELIST_TARGET(pushNotSendFailProcessBean.getCUID()) == false) return;
                    }
                    
                    UmsResultBaseBean umsResultBaseBean = makeUmsResultBaseBean(baseBean);
                    umsResultBaseBean.setPROVIDER(PROVIDER);
                    umsResultBaseBean.setCNAME(pushNotSendFailProcessBean.getCNAME());
                    umsResultBaseBean.setCUID(pushNotSendFailProcessBean.getCUID());
                    umsResultBaseBean.setMOBILE_NUM(pushNotSendFailProcessBean.getMOBILE_NUM());
                    umsResultBaseBean.setERRCODE(pushNotSendFailProcessBean.getERRCODE());
                    umsResultBaseBean.setRESULTMSG(pushNotSendFailProcessBean.getRESULTMSG());
                    umsResultBaseBean.setSEND_RESULT("FF"); //????????????
                    umsResultBaseBean.setSUCC_STATUS("0");
                    umsResultBaseBean.setPROCESS_END("Y");

                    // ?????? :  ?????????????????? ???????????? ??????????????? ??????????????? ???????????? ????????? ?????? ??????.
                    wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, baseBean);
                }
                baseBean = null;  // young GC ???????????? ????????? ??????
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException ??????");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
    }


    /**
     * ??????????????? ??????(#{?????????}, #{??????})???????????? ????????? ?????? ?????????. ?????? ????????? ?????????
     */
    private void httpPushEachSend(final PushEachProcessBean pushEachProcessBean){
        boolean isPushSendOK = false;
        String upmcResultMSG = "SUCCESS";
        String wpushSendMsg = pushEachProcessBean.getWPUSH_MSG();
        String upmcResultCode = "0000";
        String transactionKey = pushEachProcessBean.getTRANSACTION_KEY();

        try {
            Map<String, Object> postParam = makeParamMap(pushEachProcessBean);
            wpushSendMsg = postParam.get("MESSAGE").toString();
            String conUpmcHost = wpushWorkerMgr.getTcpAliveConManager().getConHostName();
            final String calUri = conUpmcHost + "/rcv_register_message.ctl";
            Map<String, String> httpHeadParam = new HashMap<String, String>();
            httpHeadParam.put("Content-Typ", "application/x-www-form-urlencoded");
            ResponseBean responseBean = HttpPoolClient.getInstance().sendPost(calUri, httpHeadParam, postParam);
            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                logger.debug("[WPUSH] upmc respons data : {}", responseBean.getBody());
                Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), HashMap.class);
                Map<String, String> upmcHeadMap = (Map<String, String>) responseMap.get("HEADER");
                Map<String, Object> upmcBodyMap = (Map<String, Object>) responseMap.get("BODY");

                upmcResultCode = upmcHeadMap.get("RESULT_CODE");
                upmcResultMSG = "[UPMC WPUSH ERR MESSAGE] ("+upmcResultCode+")"+upmcHeadMap.get("RESULT_BODY");

                if ("0000".equals(upmcResultCode)) { // code?????? ???????????? Double ???????????? ???????????? String?????? ?????? startWith??? ??????.
                    isPushSendOK = true;  // ?????? ?????? ???????????? ??????

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
                    umsResultBaseBean.setSEND_MSG(wpushSendMsg);
                    if(!"".equals(pushEachProcessBean.getMSG_VARS())) {
                        umsResultBaseBean.setMSG_VARS(pushEachProcessBean.getMSG_VARS());
                    }
                    //?????? ???????????? ???????????? ?????? ??????
                    wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean);
            		if(isFatigue(pushEachProcessBean) && umsResultBaseBean.getSEND_RESULT().equals("RS")) {
            			wpushWorkerMgr.getUmsSendCommonService().upFatigue(pushEachProcessBean.getCUID(), false);                			
            		}
                    //??????????????? ??????
                    wpushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.WPUSH);
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
                upmcResultCode = ErrorManager.UPMC_ERR_9004;
                // Http ???????????? ??????
                if(responseBean.getBody().getBytes().length>1000){
                    upmcResultMSG = "[WPUSH UPMC HTTP SERVER MESSAGE] "+responseBean.getBody().substring(0,600)+"...";
                }else{
                    upmcResultMSG = "[WPUSH UPMC HTTP SERVER MESSAGE] "+responseBean.getBody();
                }

            }
        }catch (Exception e){
            upmcResultCode = ErrorManager.UPMC_ERR_9004;
            // UPMC??? ????????? ????????? ??????????????? http ??????????????? ?????? ????????????
            upmcResultMSG = "WPUSH UPMC ?????? ?????? ??? ????????? ????????????.";
            logger.error("[Each UMS ==> UPMC] error : {}",e.toString());
        }
        // UPMC??? ??????????????? ?????? ??????????????? ???????????? ?????? ????????????????????? ???????????? ??????.
        if(!isPushSendOK){ // ????????????

            //STEP 1 : UPMC??? ??????????????? ??????????????? ???????????? ?????? ?????? ?????? ??????. UPMC ????????? ???????????? ????????? ???????????? ?????? ???????????? ?????????,SMS ????????? ????????? ???????????? ???????????? ????????????.
            boolean isProcessEnd = true;
            if("Y".equals(wpushWorkerMgr.getErrOtherSend())){  //UPMC??? ??????????????? ??????????????? ???????????? ?????? ?????? ??????.
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
            umsResultBaseBean.setSEND_MSG(wpushSendMsg);
            umsResultBaseBean.setPROCESS_END("Y");
            if(isProcessEnd) {
                //STEP 2 :UPMC ???????????? ???????????? ???????????? ?????? ?????? ??????. ?????????????????? ?????? ?????? ???????????? ????????????.
                wpushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.WPUSH,true);
            }else{
                // ???????????? ??????
                try {
                    if(pushEachProcessBean.getFAIL_RETRY_SENDTYPE() == null || pushEachProcessBean.getFAIL_RETRY_SENDTYPE().size()==0){
                        // ???????????????????????? ???????????? ?????? ?????? ????????????
                        wpushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.WPUSH,true);
                    }else{
                        //???????????? ??????
                        //???????????? ?????? - ??????????????? ?????? ???????????? ???????????? T_UMS_LOG??? ???????????? Agent?????? ?????? ?????? ?????? ??? ??? ?????? ??????.
                        umsResultBaseBean.setSEND_RESULT("RS");
                        umsResultBaseBean.setSUCC_STATUS("0");
                        umsResultBaseBean.setPROCESS_END("N");
                        wpushWorkerMgr.getUmsDao().inUmsLog(umsResultBaseBean);
                        //wpushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.WPUSH,false);
                        wpushWorkerMgr.getSentInfoManager().addSendSucc(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(), SendType.WPUSH);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    umsResultBaseBean.setPROCESS_END("Y"); //UPMC????????? ???????????? ??????????????? ?????? ???????????? ?????????????????? ?????? ????????? ??????.
                    wpushWorkerMgr.getSentInfoManager().addSendFail(pushEachProcessBean.getTRANS_TYPE(),pushEachProcessBean.getMASTERTABLE_SEQNO(),SendType.WPUSH,true);
                    logger.error("#[PushEachProcessBean] UPMC ERROR??? ?????? ?????????????????????????????? ?????? : {}",e.toString());
                }
            }
            // STEP 3 : UPMC??? ???????????????  fail Result??? DB??? ????????????.
            try {
                wpushWorkerMgr.getUmsDao().inUmsSendDetail(umsResultBaseBean, pushEachProcessBean); // ????????????(??????) ?????????????????? ??????.
            } catch (Exception e) {
                logger.error(e.toString());
                e.printStackTrace();
            }

        }
        // ??????????????? ?????? ?????? TPS???????????? ?????? ????????? ????????????.
        TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.WPUSH);
    }

    private Map<String, Object> makeParamMap(PushEachProcessBean pushEachProcessBean) throws Exception{
        Map<String, Object> postParam = new HashMap<String, Object>();
        // ???????????? ??????
        postParam.put("APP_ID", pushEachProcessBean.getWPUSH_DOMAIN());
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

        postParam.put("TEMPLATE_YN", "Y");
        postParam.put("BADGENO",pushEachProcessBean.getWPUSH_BADGENO());
        // ??????5.1?????? ??????????????????.
        postParam.put("CUST_KEY",pushEachProcessBean.getCUST_KEY());
        postParam.put("CUST_VAR1",pushEachProcessBean.getCUST_VAR1());
        postParam.put("CUST_VAR2",pushEachProcessBean.getCUST_VAR2());
        postParam.put("CUST_VAR3",pushEachProcessBean.getCUST_VAR3());

        // ????????? ??????????????? ??????
        String wpushBodyMsg = pushEachProcessBean.getWPUSH_MSG();
        String wpushTitle = pushEachProcessBean.getWPUSH_TITLE();
        String wpushExt = pushEachProcessBean.getWPUSH_EXT();
        boolean isReplaceWExt = false;
        // EXT ?????? ????????????
        if(StringUtils.isNotBlank(wpushExt) && wpushExt.indexOf("#{")>-1){
            isReplaceWExt = true;
        }

        // ????????? ????????? ??????
        if(wpushBodyMsg.indexOf("#{")>-1) {
            wpushBodyMsg = wpushBodyMsg.replace("#{?????????}", pushEachProcessBean.getCUID());
            // ????????????????????? ????????? ?????? ????????? ????????? ????????? ????????? ????????????. ????????? ???????????? ??? ??? ????????? ????????? ????????? ???????????? ?????????.
            if(!"".equals(pushEachProcessBean.getCNAME())){
                wpushBodyMsg = wpushBodyMsg.replace("#{??????}",pushEachProcessBean.getCNAME());
            }else{
                wpushBodyMsg = wpushBodyMsg.replace("#{??????}","%CNAME%");
            }
            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // ????????? ????????? ????????????
                Map<String, String> personalMap = pushEachProcessBean.getMSG_VAR_MAP();
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    wpushBodyMsg = wpushBodyMsg.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
        }
        // ???EXT ????????????
        if(isReplaceWExt){
            wpushExt= wpushExt.replace("#{?????????}", pushEachProcessBean.getCUID());
            wpushExt= wpushExt.replace("#{??????}", pushEachProcessBean.getCNAME());
            if (pushEachProcessBean.getMSG_VAR_MAP() != null && pushEachProcessBean.getMSG_VAR_MAP().size() > 0) {
                // ????????? ????????? ????????????
                Map<String, String> personalMap = pushEachProcessBean.getMSG_VAR_MAP();
                Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
                for (Map.Entry<String, String> personalEntry : personalMapSet) {
                    wpushExt= wpushExt.replace(personalEntry.getKey(), personalEntry.getValue());
                }
            }
        }
        String wpushSendMsg = wpushBodyMsg;

        if(!"".equals(wpushTitle)){  //?????? ????????? ?????? ?????? {"title":"","body":""} ????????? ??????
            if(wpushTitle.indexOf("#{")>-1) {
                wpushTitle = wpushTitle.replace("#{??????}", pushEachProcessBean.getCNAME()).replace("#{?????????}", pushEachProcessBean.getCUID());
            }
            Map<String,String> alertMap = new HashMap<>();
            alertMap.put("title",wpushTitle);
            alertMap.put("body",wpushBodyMsg);
            wpushSendMsg = gson.toJson(alertMap);
        }
        postParam.put("MESSAGE", wpushSendMsg);
        postParam.put("CUID",pushEachProcessBean.getCUID());

        // ????????? EXT
        Map<String,String> wpushExtMap = new HashMap<>();
        if(!StringUtils.isBlank(pushEachProcessBean.getWPUSH_ICON())){
            wpushExtMap.put("ICON",pushEachProcessBean.getWPUSH_ICON());
        }
        if(!StringUtils.isBlank(pushEachProcessBean.getWPUSH_LINK())){
            wpushExtMap.put("LINK",pushEachProcessBean.getWPUSH_LINK());
        }
        if(!StringUtils.isBlank(pushEachProcessBean.getWPUSH_EXT())){
            wpushExtMap.put("DATA",wpushExt);
        }
        if(wpushExtMap.size()>0){
            postParam.put("EXT",gson.toJson(wpushExtMap));
        }

        return postParam;
    }

    private void sendDaeche(PushEachProcessBean pushEachProcessBean) throws Exception{
        //STEP 2 : ?????? ?????????????????? ????????? ?????? ?????????????????? ??????????????? DB??? PUSH???????????????, ???????????? ????????? ??? ???????????? ??????
        Set<SendType> daecheSendTypes = pushEachProcessBean.getFAIL_RETRY_SENDTYPE();
        for(SendType sendType : daecheSendTypes){
            if(sendType== SendType.WPUSH){ // ?????? ????????? ????????? ???????????? PUSH>PUSH ???????????? ?????? ??????????????? ????????? ?????? ???????????? ??????.
                continue;
            }
            // UMS3.0 ?????? ????????? ?????? ?????? ??????
            String provider = null;
            switch (sendType) {
                case PUSH:
                    // ??????????????? ????????? ?????? ????????? PushEachProcessBean?????? ?????????.
                    PushEachProcessBean newPushEachProcessBean = makeDaeChePushEachProcessBean(pushEachProcessBean);
                    newPushEachProcessBean.setCUID(pushEachProcessBean.getCUID());
                    newPushEachProcessBean.setCNAME(pushEachProcessBean.getCNAME());
                    newPushEachProcessBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
                    newPushEachProcessBean.setMSG_VAR_MAP(pushEachProcessBean.getMSG_VAR_MAP());
                    newPushEachProcessBean.setMSG_VARS(pushEachProcessBean.getMSG_VARS());
                    newPushEachProcessBean.setSPLIT_MSG_CNT(pushEachProcessBean.getSPLIT_MSG_CNT());
                    newPushEachProcessBean.setDELAY_SECOND(pushEachProcessBean.getDELAY_SECOND());
                    newPushEachProcessBean.setALT_REPLACE_VARS(pushEachProcessBean.getALT_REPLACE_VARS());
                    wpushWorkerMgr.getPushWorkerMgrPool().putWork(newPushEachProcessBean); // ?????? ???????????????????????? ??????????????????????????? ??????.

                    break;
                case KKOALT:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseKkoAltSendService altSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getKkoAltProviderService(provider);
                	altSendService.umsKkoAllimTolkSend(pushEachProcessBean);
                    break;
                case KKOFRT:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseKkoFrtSendService frtSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getKkoFrtProviderService(provider);
                	frtSendService.umsKkoFriendTolkSend(pushEachProcessBean);
                    break;
                case SMS:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseSmsSendService smsSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getSmsProviderService(provider);
                	smsSendService.umsSmsSend(pushEachProcessBean);
                    break;
                case LMS: case MMS:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseMmsSendService mmsSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getMmsProviderService(provider);
                	mmsSendService.umsMmsSend(pushEachProcessBean);
                    break;
                case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseRcsSendService rcsSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getRcsProviderService(provider);
                	rcsSendService.umsReSend(pushEachProcessBean);
                    break;
                case NAVERT:
                	provider = wpushWorkerMgr.getAllotterManager().getProvider(sendType.toString(), pushEachProcessBean.getMOBILE_NUM());
                	BaseNaverSendService naverSendService = wpushWorkerMgr.getUmsChannelProvierFactory().getNaverProviderService(provider);
                	naverSendService.umsNaverSend(pushEachProcessBean);
                    break;
            }
        }
    }

    public boolean isRun() {
        return isRun;
    }

    public void setRun(boolean isRun) {
        this.isRun = isRun;
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

    private UmsResultBaseBean makeUmsResultBaseBean(PushBasicProcessBean pushBasicProcessBean){
        UmsResultBaseBean umsResultBaseBean = new UmsResultBaseBean(pushBasicProcessBean.getTRANS_TYPE(),SendType.WPUSH);
        umsResultBaseBean.setPROVIDER(PROVIDER);
        umsResultBaseBean.setROOT_CHANNEL_YN(pushBasicProcessBean.getROOT_CHANNEL_YN());
        umsResultBaseBean.setAPP_ID(pushBasicProcessBean.getWPUSH_DOMAIN());
        umsResultBaseBean.setSENDERID(pushBasicProcessBean.getSENDERID());
        umsResultBaseBean.setSENDERGROUPCODE(pushBasicProcessBean.getSENDGROUPCODE());
        umsResultBaseBean.setMASTERTABLE_SEQNO(pushBasicProcessBean.getMASTERTABLE_SEQNO());
        umsResultBaseBean.setSEND_MSG(pushBasicProcessBean.getWPUSH_MSG());
        umsResultBaseBean.setSEND_TITLE(pushBasicProcessBean.getWPUSH_TITLE());
        umsResultBaseBean.setMSG_VARS(pushBasicProcessBean.getMSG_VARS());

        return umsResultBaseBean;
    }

    private PushEachProcessBean makeDaeChePushEachProcessBean(PushEachProcessBean eachPrcsBean){
        // ????????? ????????? ?????????????????? ????????? ?????????
        PushEachProcessBean newEachPrcsBean = new PushEachProcessBean();
        newEachPrcsBean.setTRANS_TYPE(eachPrcsBean.getTRANS_TYPE());
        newEachPrcsBean.setROOT_CHANNEL_YN("N"); // ?????????????????? ??????????????? ??????.
        newEachPrcsBean.setMASTERTABLE_SEQNO(eachPrcsBean.getMASTERTABLE_SEQNO());
        newEachPrcsBean.setSENDGROUPCODE(eachPrcsBean.getSENDGROUPCODE());
        newEachPrcsBean.setSENDERID(eachPrcsBean.getSENDERID());
        newEachPrcsBean.setMIN_START_TIME(eachPrcsBean.getMIN_START_TIME());
        newEachPrcsBean.setMAX_END_TIME(eachPrcsBean.getMAX_END_TIME());
        newEachPrcsBean.setFATIGUE_YN(eachPrcsBean.getFATIGUE_YN());
        
        newEachPrcsBean.setAPP_ID(eachPrcsBean.getAPP_ID());
        newEachPrcsBean.setTITLE(eachPrcsBean.getTITLE());
        newEachPrcsBean.setMESSAGE(eachPrcsBean.getMESSAGE());
        newEachPrcsBean.setEXT(eachPrcsBean.getEXT());
        newEachPrcsBean.setBADGENO(eachPrcsBean.getBADGENO());

        newEachPrcsBean.setPUSH_TYPE(eachPrcsBean.getPUSH_TYPE());
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
