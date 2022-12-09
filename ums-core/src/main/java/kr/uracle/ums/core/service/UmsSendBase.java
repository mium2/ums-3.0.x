package kr.uracle.ums.core.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.processor.push.PushFailProcessBean;
import kr.uracle.ums.core.processor.push.PushNotSendFailProcessBean;
import kr.uracle.ums.core.processor.push.PushWorkerMgrPool;
import kr.uracle.ums.core.processor.wpush.WPushWorkerMgrPool;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.kko.BaseKkoAltSendService;
import kr.uracle.ums.core.service.send.kko.BaseKkoFrtSendService;
import kr.uracle.ums.core.service.send.mms.BaseMmsSendService;
import kr.uracle.ums.core.service.send.naver.BaseNaverSendService;
import kr.uracle.ums.core.service.send.push.PushSendService;
import kr.uracle.ums.core.service.send.rcs.BaseRcsSendService;
import kr.uracle.ums.core.service.send.sms.BaseSmsSendService;
import kr.uracle.ums.core.service.send.wpush.WPushSendService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;

/**
 * 발송할 채널의 발송타겟팅대상자를 이용하여 response 발송정보 셋팅, 발송채널의 발송 메세지 유효성검사를 공통으로 처리하는 클래스
 */
@Service
@SuppressWarnings("unchecked")
public class UmsSendBase {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    protected MessageSource messageSource;
    @Autowired(required = true)
    protected UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    protected UmsChannelProviderFactory umsChannelProviderFactory;
    @Autowired(required = true)
    protected UmsSendMacroService umsSendMacroService;

    @Autowired(required = true)
    protected PushWorkerMgrPool pushWorkerMgrPool;
    @Autowired (required = true)
    protected PushSendService pushSendService;

    @Autowired(required = true)
    protected WPushWorkerMgrPool wpushWorkerMgrPool;
    @Autowired (required = true)
    protected WPushSendService wpushSendService;

    @Autowired(required = true)
    protected SentInfoManager sentInfoManager;

    @Autowired(required = true)
    protected UmsDao umsDao;

    @Autowired
    protected Gson gson;

    /**
     * 발송메세지 채널별 검증 및 personalVars도 치환
     * @param umsSendMsgBean
     * @throws Exception
     */
    protected void checkSendMsg(final UmsSendMsgBean umsSendMsgBean) throws Exception{
    	String errorMsg = null;
    	// 푸시 메세지 검증
    	if( StringUtils.isNotBlank(umsSendMsgBean.getPUSH_MSG())) {
    		errorMsg = checkSendMsg(umsSendMsgBean.getPUSH_MSG(), umsSendMsgBean);
    		if(errorMsg != null) throw new RequestErrException(errorMsg);
    	}

        // 웹푸시 메세지 검증
        if(StringUtils.isNotBlank(umsSendMsgBean.getWPUSH_MSG())) {
     		errorMsg = checkSendMsg(umsSendMsgBean.getWPUSH_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
        }
    	
    	// 알림톡 메세지 검증.
    	if( StringUtils.isNotBlank(umsSendMsgBean.getALLIMTALK_MSG()) ) {
     		errorMsg = checkSendMsg(umsSendMsgBean.getALLIMTALK_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
    	}
    	  	
        // 친구톡 메세지 검증
        if( StringUtils.isNotBlank(umsSendMsgBean.getFRIENDTOLK_MSG()) ) {
     		errorMsg = checkSendMsg(umsSendMsgBean.getFRIENDTOLK_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
        }
        
        // RCS 메시지 검증
        if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT()) && StringUtils.isNotBlank(umsSendMsgBean.getRCS_MSG()))  {
     		errorMsg = checkSendMsg(umsSendMsgBean.getRCS_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
        }
        // RCS 메시지 검증
        if(StringUtils.isNotBlank(umsSendMsgBean.getRCS_OBJECT()))  {
            errorMsg = checkSendMsg(umsSendMsgBean.getRCS_OBJECT(), umsSendMsgBean);
            if(errorMsg != null) throw new RequestErrException(errorMsg);
        }

        // SMS 메세지 검증 - #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리, 개인화 변수가 있으면 우선은 MMS로 발송처리 함. 단, MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(StringUtils.isNotBlank(umsSendMsgBean.getSMS_MSG())) {
     		errorMsg = checkSendMsg(umsSendMsgBean.getSMS_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
        }

        // 네이버 메세지 검증.
        if(StringUtils.isNotBlank(umsSendMsgBean.getNAVER_MSG()) ) {
     		errorMsg = checkSendMsg(umsSendMsgBean.getNAVER_MSG(), umsSendMsgBean);
     		if(errorMsg != null) throw new RequestErrException(errorMsg);
        }

    }
    private String checkSendMsg(String msg, UmsSendMsgBean umsSendMsgBean) throws RequestErrException {
    	String errorMsg = null;
    	
    	Map<String,List<String>> msgVarCheckMap = umsSendCommonService.getReplaceVars(msg);
		// 개별화 메세지가 존재 할 경우 개별화 메세지 검증.
		List<String> personalVars = msgVarCheckMap.get("personalVars");
		
		// 개인치환변수가 존재 시, 치환필드의 값을 확인한다.
		if(personalVars.size()>0) {
			if(StringUtils.isBlank(umsSendMsgBean.getREPLACE_VARS())) return "개별 메시지 치환에 필요할 치한필드 값이 누럭되습니다.";			
			try {
					// 요청 전문 치환변수관련 정보를 맵핑맵으로 변경
					Map<String, String> replaceVarMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
					// 메시지 내 개별화 치환변수들 중 치환변수 맵핑맵에 없다면 발송 요청 실패 처리
					for (String var : personalVars) {
						if (replaceVarMap.containsKey(var) == false) return "메세지 내용의 개별화정보가 치환필드(replace_vars)에 존재하지 않습니다." + var;
					}
					umsSendMsgBean.setREPLACE_VAR_MAP(replaceVarMap);
			}catch (JsonSyntaxException e){ return "치환필드(replace_vars) 값이 올바르지 않은 JSON데이타 스트링 입니다. "+e.getMessage();}
		}
		
		if(msgVarCheckMap.get("commonVars").size()>0){
			umsSendMsgBean.setReplaceMsg(true);
		}    		
    	
    	return errorMsg;
    }
		
    protected Map<String,Object> umsPushSend(Map<String,List<String>> regPushUserMap, Map<String,List<String>> notRegPushUserMap, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        int TOTAL_SEND_CNT = regPushUserMap.size()+notRegPushUserMap.size();
        
        boolean isDaecheSend = false;
        // 실패시 대체발송 채널여부 확인
        Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);

        // 푸시서비스 가입자 아닌 경우 대체발송 처리 하는 카운트 원장에 셋팅.
        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

        // STEP 7: 대체발송매체 별 발송카운트 초기 셋팅
        isDaecheSend = umsSendCommonService.setPushFailSendRetryCnt(umsSendMsgBean,failRetrySendTypeSet,notRegPushUserMap.size());

        if(isDaecheSend) {
            // 푸시발송카운트 + 푸시발송실패카운트 + 대체발송카운트
//            totalSendCnt =  regPushUserMap.size() + notRegPushUserMap.size()+ (notRegPushUserMap.size()*failRetrySendTypeSet.size());
            umsSendMsgBean.setPUSH_SEND_CNT(regPushUserMap.size()+ notRegPushUserMap.size()); //푸시 발송 유저 카운트셋팅
        }else{
            umsSendMsgBean.setPUSH_SEND_CNT(regPushUserMap.size()); //푸시 발송 유저 카운트 셋팅
            //umsSendMsgBean.setFAIL_CNT(notRegPushUserMap.size());
        }

        // STEP 8: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 총 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);

            // 공통 : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기가 가장 마지막 셋팅 부분임.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // STEP 9 : 발송처리 매니저에 총 발송정보 저장
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        
        // 치환변수 처리 - {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
        Map<String,String> replaceVarsMap = null;
        Map<String, Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
        	replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
        	
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            Set<String> cuidKeySet = regPushUserMap.keySet();
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }            
        }
        
        // STEP 10 : 푸시발송처리
        pushSendService.pushMapListSend(regPushUserMap,umsSendMsgBean, cuidVarMap, false);


        //푸시 서비스 가입이 되어 있는 않은 사용자(notRegPushUsers 대체발송을 확인하여 처리.
        if(isDaecheSend){
            Set<SendType> daecheSendTypes = umsSendMsgBean.getFAIL_RETRY_SENDTYPE();
            for(SendType sendType : daecheSendTypes){
                if(sendType==SendType.PUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                    continue;
                }
                umsSendCommonService.sendDaeche(umsSendMsgBean, notRegPushUserMap, sendType);
            }
            // 대체발송을 처리한 사용자는 푸시서비스가입 되지 않은 최종실패가 아닌 실패로(최종실패X) 처리한다. 푸시성공카운트 -1, 실패카운트 +1 이됨
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet) {
                PushFailProcessBean pushFailProcessBean = new PushFailProcessBean();
                pushFailProcessBean.setTRANS_TYPE(transType);
                pushFailProcessBean.setPROCESS_END(false);
                pushFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushFailProcessBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
                pushFailProcessBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
                pushFailProcessBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
                pushFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                pushFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());
                pushFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());

                if(replaceVarsMap!=null){
                    pushFailProcessBean.setMSG_VAR_MAP(replaceVarsMap);
                    pushFailProcessBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
                }

                pushFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 푸시서비스에 가입되어 있지 않은 사용자입니다.");

                List<String> userInfos = notRegPushUserMap.get(cuid); //["핸드폰번호","이름"]
                pushFailProcessBean.setCUID(cuid);
                if(userInfos!=null) {
                    if(userInfos.size()>1) {
                        pushFailProcessBean.setCNAME(userInfos.get(1));
                        pushFailProcessBean.setMOBILE_NUM(userInfos.get(0));
                    }
                }
                pushWorkerMgrPool.putWork(pushFailProcessBean);
            }
        }else{
            // 대체발송을 설정하지 않고 PUSH 서비스 가입자가 아닌 발송대상자는 ResultWorkMgr에 실패결과로 등록한다.(비동기)
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet){
                PushNotSendFailProcessBean pushNotSendFailProcessBean = new PushNotSendFailProcessBean();
                pushNotSendFailProcessBean.setTRANS_TYPE(transType);
                pushNotSendFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushNotSendFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushNotSendFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushNotSendFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushNotSendFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
                pushNotSendFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());// 발송자 아이디.
                pushNotSendFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());// 발송자를 그룹으로 관리 할 경우(조직도코드 또는 권한그룹코드)
                pushNotSendFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushNotSendFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 푸시서비스에 가입되어 있지 않은 사용자입니다.");

                if(replaceVarsMap!=null){
                    pushNotSendFailProcessBean.setMSG_VAR_MAP(replaceVarsMap);
                    pushNotSendFailProcessBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
                }

                List<String> userInfos = notRegPushUserMap.get(cuid); //["핸드폰번호","이름"]
                pushNotSendFailProcessBean.setCUID(cuid);
                if(userInfos!=null) {
                    if(userInfos.size()>1) {
                        pushNotSendFailProcessBean.setCNAME(userInfos.get(1));
                        pushNotSendFailProcessBean.setMOBILE_NUM(userInfos.get(0));
                    }
                }
                pushWorkerMgrPool.putWork(pushNotSendFailProcessBean);
            }
        }
        // 공통 : 채널별 발송 결과 카운트 셋팅처리. 실시간 발송 여기가 발송결과 셋팅 부분임.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }

    /**
     * 회원/비회원 웹푸시 발송 공통
     * @param regPushUserMap
     * @param notRegPushUserMap
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsWPushSend(Map<String,List<String>> regPushUserMap, Map<String,List<String>> notRegPushUserMap, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        int TOTAL_SEND_CNT = regPushUserMap.size()+notRegPushUserMap.size();

        boolean isDaecheSend = false;
        // 실패시 대체발송 채널여부 확인
        Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.WPUSH);

        // 푸시서비스 가입자 아닌 경우 대체발송 처리 하는 카운트 원장에 셋팅.
        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

        // STEP 7: 대체발송매체 별 발송카운트 초기 셋팅
        isDaecheSend = umsSendCommonService.setWPushFailSendRetryCnt(umsSendMsgBean,failRetrySendTypeSet,notRegPushUserMap.size());

        if(isDaecheSend) {
            // 푸시발송카운트 + 푸시발송실패카운트 + 대체발송카운트
//            totalSendCnt =  regPushUserMap.size() + notRegPushUserMap.size()+ (notRegPushUserMap.size()*failRetrySendTypeSet.size());
            umsSendMsgBean.setWPUSH_SEND_CNT(regPushUserMap.size()+ notRegPushUserMap.size()); //푸시 발송 유저 카운트셋팅
        }else{
            umsSendMsgBean.setWPUSH_SEND_CNT(regPushUserMap.size()); //푸시 발송 유저 카운트 셋팅
            //umsSendMsgBean.setFAIL_CNT(notRegPushUserMap.size());
        }

        // STEP 8: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);

            // 공통 : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기가 가장 마지막 셋팅 부분임.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        
        // 치환변수 처리 - {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
        Map<String, String> replaceVarsMap = null;
        Map<String, Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
        	replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
        	
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            Set<String> cuidKeySet = regPushUserMap.keySet();
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }            
        }
        

        // STEP 10 : 웹푸시발송처리
        wpushSendService.wpushMapListSend(regPushUserMap,umsSendMsgBean, cuidVarMap, false);


        //푸시 서비스 가입이 되어 있는 않은 사용자(notRegPushUsers 대체발송을 확인하여 처리.
        if(isDaecheSend){
            Set<SendType> daecheSendTypes = umsSendMsgBean.getFAIL_RETRY_SENDTYPE();
            for(SendType sendType : daecheSendTypes){
                if(sendType==SendType.WPUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                    continue;
                }
                umsSendCommonService.sendDaeche(umsSendMsgBean, notRegPushUserMap, sendType);
            }
            // 대체발송을 처리한 사용자는 푸시서비스가입 되지 않은 최종실패가 아닌 실패로(최종실패X) 처리한다. 푸시성공카운트 -1, 실패카운트 +1 이됨
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet) {
                PushFailProcessBean pushFailProcessBean = new PushFailProcessBean();
                pushFailProcessBean.setTRANS_TYPE(transType);
                pushFailProcessBean.setPROCESS_END(false);
                pushFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushFailProcessBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
                pushFailProcessBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
                pushFailProcessBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
                pushFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                pushFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());
                pushFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
                pushFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 웹푸시서비스에 가입되어 있지 않은 사용자입니다.");

                if(replaceVarsMap!=null){
                    pushFailProcessBean.setMSG_VAR_MAP(replaceVarsMap);
                    pushFailProcessBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
                }

                List<String> userInfos = notRegPushUserMap.get(cuid); //["핸드폰번호","이름"]
                pushFailProcessBean.setCUID(cuid);
                if(userInfos!=null) {
                    if(userInfos.size()>1) {
                        pushFailProcessBean.setCNAME(userInfos.get(1));
                        pushFailProcessBean.setMOBILE_NUM(userInfos.get(0));
                    }
                }
                wpushWorkerMgrPool.putWork(pushFailProcessBean);
            }
        }else{
            // 대체발송을 설정하지 않고 PUSH 서비스 가입자가 아닌 발송대상자는 ResultWorkMgr에 실패결과로 등록한다.(비동기)
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet){
                PushNotSendFailProcessBean pushNotSendFailProcessBean = new PushNotSendFailProcessBean();
                pushNotSendFailProcessBean.setTRANS_TYPE(transType);
                pushNotSendFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushNotSendFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushNotSendFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushNotSendFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushNotSendFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());

                //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
                pushNotSendFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());// 발송자 아이디.
                pushNotSendFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());// 발송자를 그룹으로 관리 할 경우(조직도코드 또는 권한그룹코드)
                pushNotSendFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushNotSendFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 웹푸시서비스에 가입되어 있지 않은 사용자입니다.");

                if(replaceVarsMap!=null){
                    pushNotSendFailProcessBean.setMSG_VAR_MAP(replaceVarsMap);
                    pushNotSendFailProcessBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
                }

                List<String> userInfos = notRegPushUserMap.get(cuid); //["핸드폰번호","이름"]
                pushNotSendFailProcessBean.setCUID(cuid);
                if(userInfos!=null) {
                    if(userInfos.size()>1) {
                        pushNotSendFailProcessBean.setCNAME(userInfos.get(1));
                        pushNotSendFailProcessBean.setMOBILE_NUM(userInfos.get(0));
                    }
                }
                wpushWorkerMgrPool.putWork(pushNotSendFailProcessBean);
            }
        }
        // 공통 : 채널별 발송 결과 카운트 셋팅처리. 실시간 발송 여기가 발송결과 셋팅 부분임.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }

    protected Map<String,Object> umsAltSend(Map<String, List<String>> sendUserObj, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 알림톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if(finalFailCuid!=null && finalFailCuid.size()>0){

        }
        
        int TOTAL_SEND_CNT = sendUserObj.size();
        umsSendMsgBean.setALLIMTOLK_CNT(TOTAL_SEND_CNT);

        // STEP 2: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.

        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);
            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        //개발화 치환 메세지 파라미터(REPLACE_VARS)가 존재 할 경우 {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}} 형태로 만들어서 처리
        Map<String,Map<String,String>> cuidVarMap = null;
        if(!"".equals(umsSendMsgBean.getREPLACE_VARS())){
            Map<String,Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(),Map.class);
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            Set<String> cuidKeySet = sendUserObj.keySet();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }
        }
        for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
    		BaseKkoAltSendService sendService = umsChannelProviderFactory.getKkoAltProviderService(provider); 
    		sendService.umsKkoAllimTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
    	}
        // umsSendCommonService.getKkoAltProviderService().umsKkoAllimTolkSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);

        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }

    protected Map<String,Object> umsFrtSend(Map<String, List<String>> sendUserObj, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception {
        Map<String, Object> returnResultMap = new HashMap<String, Object>();
        
        // 친구톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if (finalFailCuid != null && finalFailCuid.size() > 0) {

        }

        int TOTAL_SEND_CNT = sendUserObj.size();
        umsSendMsgBean.setFRIENDTOLK_CNT(TOTAL_SEND_CNT);

        // STEP 2: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }
        
        // 치환변수 처리 - {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
        Map<String, Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
        	Map<String, Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
        	
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            Set<String> cuidKeySet = sendUserObj.keySet();
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }            
        }

        // STEP 4 : 친구톡 처리 후 response 데이타 만듬.
        for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
    		BaseKkoFrtSendService sendService = umsChannelProviderFactory.getKkoFrtProviderService(provider); 
    		sendService.umsKkoFriendTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
    	}
        //umsSendCommonService.getKkoFrtProviderService().umsKkoFriendTolkSend(sendUserObj,umsSendMsgBean, cuidVarMap, false);

        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }
    
    protected Map<String,Object> umsRcsSend(Map<String, List<String>> sendUserObj, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 발송 실패 사용자 대체발송 로직 - 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정 필요
        if(finalFailCuid!=null && finalFailCuid.size()>0){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();
        SendType sendType;
        try {
        	sendType = SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND());
        	umsSendMsgBean.setChannelSendCount(sendType, sendUserObj.size());        	
        }catch (IllegalArgumentException e) { throw new Exception(umsSendMsgBean.getSTART_SEND_KIND() + " is not supported send type."); }

        // STEP 2: 예약 발송 일시 값이 존재 시 예약 발송 처리 - 예약발송 테이블 저장 >> UMS-AGENT의 의한 예약된 발송 재 요청.
        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(StringUtils.isNotBlank(umsSendMsgBean.getRESERVEDATE())){
            // 날짜 형식 검증
            if(umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm") == false ) throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            // 예약발송 입력 
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);
            //  응답 바디 전문 - 채널별 발송 카운트 셋팅처리`
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }

        // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
        umsDao.inUmsSendMsg(umsSendMsgBean);

        // 치환변수 처리 - {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
        Map<String, Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
        	Map<String, Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
        	
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            Set<String> cuidKeySet = sendUserObj.keySet();
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }            
        }
        // STEP 4 : RCS 발송 처리
    	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(sendType.toString(), mobileNum);
    		BaseRcsSendService sendService = umsChannelProviderFactory.getRcsProviderService(provider);
    		sendService.umsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
    	}
        //umsSendCommonService.getRcsProviderService().umsSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);
        
        // 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean, returnResultMap);
        return returnResultMap;
    }

    protected Map<String,Object> umsSmsSend(Map<String, List<String>> sendUserObj, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean, SendType sendType) throws Exception {
        Map<String, Object> returnResultMap = new HashMap<String, Object>();

        // SMS 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if (finalFailCuid != null && finalFailCuid.size() > 0) {

        }

        int TOTAL_SEND_CNT = sendUserObj.size();
        
        try {
            umsSendMsgBean.setChannelSendCount(sendType, TOTAL_SEND_CNT);
        }catch (IllegalArgumentException e) { throw new Exception(umsSendMsgBean.getSTART_SEND_KIND() + " is not supported send type."); }

        // STEP 6: 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // 치환변수 처리 - {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
        Map<String, Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
        	Map<String, Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), Map.class);
        	
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            Set<String> cuidKeySet = sendUserObj.keySet();
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }            
        }
        
        // STEP 8 : SMS 처리 후 response 데이타 만듬.

    	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		String provider = null;
    		if(sendType == SendType.SMS) {
        		provider = getProvider(sendType.toString(), mobileNum);
        		BaseSmsSendService sendService = umsChannelProviderFactory.getSmsProviderService(provider);
        		sendService.umsSmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);        			
    		}else {
    			provider = getProvider(sendType.toString(), mobileNum);
        		BaseMmsSendService sendService = umsChannelProviderFactory.getMmsProviderService(provider);
        		sendService.umsMmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
    		}
    	}
        //umsSendCommonService.getSmsProviderService().umsSmsSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);
    	//umsSendCommonService.getMmsProviderService().umsMmsSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);


        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }

    protected Map<String,Object> umsNaverSend(Map<String, List<String>> sendUserObj, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 네이버톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if(finalFailCuid!=null && finalFailCuid.size()>0){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();
        umsSendMsgBean.setNAVERT_CNT(TOTAL_SEND_CNT);

        // 발송카운트 초기 셋팅
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);
            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        //개발화 치환 메세지 파라미터(REPLACE_VARS)가 존재 할 경우 {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}} 형태로 만들어서 처리
        Map<String,Map<String,String>> cuidVarMap = null;
        if(!"".equals(umsSendMsgBean.getREPLACE_VARS())){
            Map<String,Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(),Map.class);
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            
            cuidVarMap = new HashMap<String, Map<String, String>>();
            Set<String> cuidKeySet = sendUserObj.keySet();
            for(String reqSendcuid : cuidKeySet){
                cuidVarMap.put(reqSendcuid,varMap);
            }
        }
    	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
    		BaseNaverSendService sendService = umsChannelProviderFactory.getNaverProviderService(provider);
    		sendService.umsNaverSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
    	}
        //umsSendCommonService.getNaverProviderService().umsNaverSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);

        // STEP 3 [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }
    
    private String getProvider(String channel, String identifyKey) { return umsSendCommonService.getAllotterManager().getProvider(channel, identifyKey); }
}
