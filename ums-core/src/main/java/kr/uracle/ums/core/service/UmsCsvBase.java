package kr.uracle.ums.core.service;

import au.com.bytecode.opencsv.CSVReader;
import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.common.Constants;
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
import kr.uracle.ums.core.service.send.thread.DelaySendThreadRedis;
import kr.uracle.ums.core.service.send.wpush.WPushSendService;
import kr.uracle.ums.core.util.StringUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 15..
 */
@SuppressWarnings("unchecked")
public class UmsCsvBase {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    protected RedisTemplate redisTemplate;
    @Autowired(required = true)
    protected UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    protected UmsChannelProviderFactory umsChannelProviderFactory;
    @Autowired(required = true)
    protected UmsSendMacroService umsSendMacroService;
    @Autowired(required = true)
    private UmsDao umsDao;
    @Autowired(required = true)
    protected SentInfoManager sentInfoManager;
    @Autowired (required = true)
    protected PushSendService pushSendService;
    @Autowired (required = true)
    protected WPushSendService wpushSendService;
    @Autowired(required = true)
    protected PushWorkerMgrPool pushWorkerMgrPool;
    @Autowired(required = true)
    protected WPushWorkerMgrPool wpushWorkerMgrPool;

    @Value("${UMS.TEMPDIR:}")
    protected String TEMPDIR;

    @Autowired(required = true)
    protected Gson gson;

    /**
     * 발송메세지 채널별 검증
     * @param umsSendMsgBean
     * @throws Exception
     */
    protected void checkSendMsg(final UmsSendMsgBean umsSendMsgBean, Map<String,Integer> varsIndexMap) throws Exception{
    	String macroCode = umsSendMsgBean.getSEND_MACRO_CODE();
    	
    	// 푸시 메세지 검증
    	if(umsSendMacroService.isMacroSendChannel(macroCode, SendType.PUSH)) {
    		Map<String,List<String>> msgVarCheckMap = umsSendCommonService.getReplaceVars(umsSendMsgBean.getPUSH_MSG());
    		// 개별화 메세지가 존재 할 경우 개별화 메세지 검증.
    		List<String> personalVars = msgVarCheckMap.get("personalVars");
    		if(personalVars.size()>0){
    			umsSendMsgBean.setReplaceMsg(true);
    			for(String personVar : personalVars){
    				if(!varsIndexMap.containsKey(personVar)){
    					throw new Exception("푸시 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다."+personVar);
    				}
    			}
    			umsSendMsgBean.setReplaceMsg(true);
    		}
    		if(msgVarCheckMap.get("commonVars").size()>0){
    			umsSendMsgBean.setReplaceMsg(true);
    		}    		
    	}

        // 웹푸시 메세지 검증
        if(umsSendMacroService.isMacroSendChannel(macroCode, SendType.WPUSH)) {
            Map<String,List<String>> msgVarCheckMap = umsSendCommonService.getReplaceVars(umsSendMsgBean.getWPUSH_MSG());
            // 개별화 메세지가 존재 할 경우 개별화 메세지 검증.
            List<String> personalVars = msgVarCheckMap.get("personalVars");
            if(personalVars.size()>0){
                umsSendMsgBean.setReplaceMsg(true);
                for(String personVar : personalVars){
                    if(!varsIndexMap.containsKey(personVar)){
                        throw new Exception("웹푸시 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다."+personVar);
                    }
                }
                umsSendMsgBean.setReplaceMsg(true);
            }
            if(msgVarCheckMap.get("commonVars").size()>0){
                umsSendMsgBean.setReplaceMsg(true);
            }
        }
    	
    	// 알림톡 메세지 검증.
    	if( umsSendMacroService.isMacroSendChannel(macroCode, SendType.KKOALT) && StringUtils.isNotBlank(umsSendMsgBean.getALLIMTALK_MSG()) ) {
   			umsSendCommonService.chkAltMsgCsv(umsSendMsgBean, varsIndexMap);
    	}

        // 친구톡 메세지 검증
        if( umsSendMacroService.isMacroSendChannel(macroCode, SendType.KKOFRT) && StringUtils.isNotBlank(umsSendMsgBean.getFRIENDTOLK_MSG()) ) {
            umsSendCommonService.chkFrtMsgCsv(umsSendMsgBean, varsIndexMap);
        }

        // RCS 메시지 검증
        if( umsSendMacroService.isMacroSendChannel(macroCode, SendType.RCS_SMS) && (StringUtils.isNotBlank(umsSendMsgBean.getRCS_MSG()) || StringUtils.isNotBlank(umsSendMsgBean.getRCS_OBJECT()))) {
            umsSendCommonService.chkRcsMsgCsv(umsSendMsgBean, varsIndexMap);
        }        
        
        // SMS 메세지 검증 - #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리, 개인화 변수가 있으면 우선은 MMS로 발송처리 함. 단, MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(umsSendMacroService.isMacroSendChannel(macroCode, SendType.SMS) && StringUtils.isNotBlank(umsSendMsgBean.getSMS_MSG())) {
            umsSendCommonService.chkSmsMsgCsv(umsSendMsgBean, varsIndexMap);
        }

        // 네이버톡 메세지 검증.
        if( umsSendMacroService.isMacroSendChannel(macroCode, SendType.NAVERT) && StringUtils.isNotBlank(umsSendMsgBean.getNAVER_MSG()) ) {
            umsSendCommonService.chkNaverMsgCsv(umsSendMsgBean, varsIndexMap);
        }
    }

    public boolean delaySmsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend){
        boolean isDelaySmsSend = false;
        // 이곳에서 SPLIT_MSG_CNT,DELAY_SECOND를 체크하고 DELAY_SECOND가 0이 아니고 SPLIT_MSG_CNT보다 pushUserMaps사이즈가 클경우 쓰레드를 만들어 레디스에 담도록 처리.
        int delaySecond = 0;
        int splitCnt = 0;
        try {
            delaySecond = Integer.parseInt(umsSendMsgBean.getDELAY_SECOND().trim());
            splitCnt = Integer.parseInt(umsSendMsgBean.getSPLIT_MSG_CNT().trim());
        }catch (Exception e){
            logger.error("지연시간 또는 분할발송 카운트가 올바르지 않아 지연발송이 적용되지 않았습니다.");
        }
        // 대체발송이 아니면서 지연발송일 경우만....
        if(!isDaeCheSend && delaySecond>0 && users.size()>splitCnt){
            // 이곳에서 지연발송 처리하는 쓰레드를 만들어 레디스큐에 담는다.
            DelaySendThreadRedis delaySendThreadReadis = new DelaySendThreadRedis(users,umsSendMsgBean,cuidVarMap,delaySecond,splitCnt);
            delaySendThreadReadis.start();
            isDelaySmsSend = true;
        }
        return isDelaySmsSend;
    }

    /**
     * CSV 푸시발송
     */
    protected Map<String,Object> umsPushCsvSend(Map<String,List<String>> regPushUserMap, Map<String,List<String>> notRegPushUserMap, Map<String,Map<String,String>> cuidVarMap,Map<String,Map<String,String>> notPushCuidVarMap,UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        int TOTAL_SEND_CNT = regPushUserMap.size()+notRegPushUserMap.size();

        boolean isDaecheSend = false;
        // STEP 1 : 실패시 대체발송 채널여부 확인
        Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);

        // STEP 2 : 실패시 대체발송 채널 셋팅
        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

        // STEP 3: 대체발송매체 별 발송카운트 초기 셋팅
        isDaecheSend = umsSendCommonService.setPushFailSendRetryCnt(umsSendMsgBean,failRetrySendTypeSet,notRegPushUserMap.size());

        // STEP 4: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 예약발송 체크하여 예약발송 테이블에 등록한다. UMS_SEQNO는 UMS-AGENT가 예약발송테이블을 감시하다 보낼때 값이 0이 아니다.
        // 푸시 예약발송처리는 UMS-AGENT에서 처리 할 수 있도록 T_UMS_SEND 테이블에 값을 생성하고 STATUS를 'R'로 셋팅 후 T_UMS_RESERVE에 등록한다.

        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // 공통 : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기가 가장 마지막 셋팅 부분임.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }
        
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());

        // STEP 6 : 푸시발송처리
        pushSendService.pushMapListSend(regPushUserMap,umsSendMsgBean,cuidVarMap, false);


        // STEP 7 : 푸시서비스 미가입 사용자 대체발송 처리
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

                if(notPushCuidVarMap.containsKey(cuid)){
                    Map<String,String> varMap = notPushCuidVarMap.get(cuid);
                    pushFailProcessBean.setMSG_VAR_MAP(varMap);
                    pushFailProcessBean.setMSG_VARS(gson.toJson(varMap));
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
            // STEP 8 : 대체발송을 설정하지 않고  PUSH 서비스 가입자가 아닌 발송대상자는 ResultWorkMgr에 최종 실패결과로 등록한다.(비동기)
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet){
                PushNotSendFailProcessBean pushNotSendFailProcessBean = new PushNotSendFailProcessBean();
                pushNotSendFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushNotSendFailProcessBean.setTRANS_TYPE(transType);
                pushNotSendFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushNotSendFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushNotSendFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushNotSendFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                

                if(notPushCuidVarMap.containsKey(cuid)){
                    Map<String,String> varMap = notPushCuidVarMap.get(cuid);
                    pushNotSendFailProcessBean.setMSG_VAR_MAP(varMap);
                    pushNotSendFailProcessBean.setMSG_VARS(gson.toJson(varMap));
                }

                //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
                pushNotSendFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());// 발송자 아이디.
                pushNotSendFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());// 발송자를 그룹으로 관리 할 경우(조직도코드 또는 권한그룹코드)
                pushNotSendFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushNotSendFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 푸시서비스에 가입되어 있지 않은 사용자입니다.");

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
        
        if(isDaecheSend) {
            // 푸시발송카운트 + 푸시발송실패카운트 + 대체발송카운트
//            totalSendCnt =  regPushUserMap.size() + notRegPushUserMap.size()+ (notRegPushUserMap.size()*failRetrySendTypeSet.size());
            umsSendMsgBean.setPUSH_SEND_CNT(regPushUserMap.size()); //푸시 발송 유저 카운트셋팅
        }else{
            umsSendMsgBean.setPUSH_SEND_CNT(regPushUserMap.size()); //푸시 발송 유저 카운트 셋팅
            umsSendMsgBean.setFAIL_CNT(notRegPushUserMap.size());
        }
        
        // 공통 : 채널별 발송 결과 카운트 셋팅처리. 실시간 발송 여기가 발송결과 셋팅 부분임.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }

    /**
     * CSV 웹푸시발송 (회원CSV, 비회원CSV 공통)
     */
    protected Map<String,Object> umsWPushCsvSend(Map<String,List<String>> regPushUserMap, Map<String,List<String>> notRegPushUserMap, Map<String,Map<String,String>> cuidVarMap,Map<String,Map<String,String>> notPushCuidVarMap,UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        int TOTAL_SEND_CNT = regPushUserMap.size()+notRegPushUserMap.size();

        boolean isDaecheSend = false;
        // STEP 1 : 실패시 대체발송 채널여부 확인
        Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.WPUSH);

        // STEP 2 : 실패시 대체발송 채널 셋팅
        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

        // STEP 3: 웹푸시 대체발송매체 별 발송카운트 초기 셋팅
        isDaecheSend = umsSendCommonService.setWPushFailSendRetryCnt(umsSendMsgBean,failRetrySendTypeSet,notRegPushUserMap.size());

        if(isDaecheSend) {
            // 웹푸시푸시발송카운트 + 웹푸시푸시발송실패카운트 + 대체발송카운트
//            totalSendCnt =  regPushUserMap.size() + notRegPushUserMap.size()+ (notRegPushUserMap.size()*failRetrySendTypeSet.size());
            umsSendMsgBean.setWPUSH_SEND_CNT(regPushUserMap.size()); //웹푸시 발송 유저 카운트셋팅
        }else{
            umsSendMsgBean.setWPUSH_SEND_CNT(regPushUserMap.size()); //웹푸시 발송 유저 카운트 셋팅
            umsSendMsgBean.setFAIL_CNT(notRegPushUserMap.size());
        }

        // STEP 4: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 예약발송 체크하여 예약발송 테이블에 등록한다. UMS_SEQNO는 UMS-AGENT가 예약발송테이블을 감시하다 보낼때 값이 0이 아니다.
        // 푸시 예약발송처리는 UMS-AGENT에서 처리 할 수 있도록 T_UMS_SEND 테이블에 값을 생성하고 STATUS를 'R'로 셋팅 후 T_UMS_RESERVE에 등록한다.
        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // 공통 : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기가 가장 마지막 셋팅 부분임.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        
        // STEP 6 : 웹푸시발송처리
        wpushSendService.wpushMapListSend(regPushUserMap,umsSendMsgBean,cuidVarMap,false);


        // STEP 7 : 푸시서비스 미가입 사용자 대체발송 처리
        if(isDaecheSend){
            Set<SendType> daecheSendTypes = umsSendMsgBean.getFAIL_RETRY_SENDTYPE();
            for(SendType sendType : daecheSendTypes){
                if(sendType==SendType.WPUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                    continue;
                }
                umsSendCommonService.sendDaeche(umsSendMsgBean, notRegPushUserMap, sendType);
            }
            // 대체발송을 처리한 사용자는 푸시서비스가입 되지 않은 최종실패가 아닌 실패로(최종실패X) 처리한다. 웹푸시성공카운트 -1, 실패카운트 +1 이됨
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
            // STEP 8 : 대체발송을 설정하지 않고  PUSH 서비스 가입자가 아닌 발송대상자는 ResultWorkMgr에 최종 실패결과로 등록한다.(비동기)
            Set<String> cuidSet = notRegPushUserMap.keySet();
            for(String cuid : cuidSet){
                PushNotSendFailProcessBean pushNotSendFailProcessBean = new PushNotSendFailProcessBean();
                pushNotSendFailProcessBean.setTRANS_TYPE(transType);
                pushNotSendFailProcessBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                pushNotSendFailProcessBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
                pushNotSendFailProcessBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
                pushNotSendFailProcessBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
                
                pushNotSendFailProcessBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                pushNotSendFailProcessBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                pushNotSendFailProcessBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                pushNotSendFailProcessBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
                pushNotSendFailProcessBean.setSENDERID(umsSendMsgBean.getSENDERID());// 발송자 아이디.
                pushNotSendFailProcessBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());// 발송자를 그룹으로 관리 할 경우(조직도코드 또는 권한그룹코드)
                pushNotSendFailProcessBean.setERRCODE(ErrorManager.ERR_4400);
                pushNotSendFailProcessBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", 웹푸시서비스에 가입되어 있지 않은 사용자입니다.");

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

    /**
     * CSV 알림톡 발송.
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsAltCsvSend(Map<String, List<String>> sendUserObj, Map<String,Map<String,String>> sendCuidVarsMap, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception {
    	
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 알림톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        // UMS 회원의 핸드폰번호는 필수값이므로 해당 프로세스는 필요 없을 것 같음.
        if(ObjectUtils.isNotEmpty(finalFailCuid)){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();

        // STEP 2: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.

        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // STEP 4 : 알림톡 발송카운트 셋팅.
        umsSendMsgBean.setALLIMTOLK_CNT(TOTAL_SEND_CNT);

        // STEP 5 : 알림톡 발송처리
    	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
    		BaseKkoAltSendService sendService = umsChannelProviderFactory.getKkoAltProviderService(provider); 
    		sendService.umsKkoAllimTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
    	}
        //umsSendCommonService.getKkoAltProviderService().umsKkoAllimTolkSend(sendUserObj,umsSendMsgBean,sendCuidVarsMap, false);
    	
        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }

    /**
     * CSV 친구톡 발송.
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsFrtCsvSend(Map<String, List<String>> sendUserObj, Map<String,Map<String,String>> sendCuidVarsMap, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception {
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 친톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if(finalFailCuid!=null && finalFailCuid.size()>0){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();

        // STEP 2: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // STEP 4 : 친구톡 발송카운트 셋팅.
        umsSendMsgBean.setFRIENDTOLK_CNT(TOTAL_SEND_CNT);

        // 2022.08.01 SMS 분할발송 로직 추가.
        boolean isDelaySend = delaySmsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
        if(!isDelaySend) {
            // STEP 5 : 친구톡 발송처리
            for (Entry<String, List<String>> e : sendUserObj.entrySet()) {
                String userId = e.getKey();
                List<String> userInfos = e.getValue();
                String mobileNum = userInfos.get(0);

                String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
                BaseKkoFrtSendService sendService = umsChannelProviderFactory.getKkoFrtProviderService(provider);
                sendService.umsKkoFriendTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
            }
            //umsSendCommonService.getKkoFrtProviderService().umsKkoFriendTolkSend(sendUserObj,umsSendMsgBean,sendCuidVarsMap, false);
        }

        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }
    
    /**
     * CSV RCS 발송.
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsRcsCsvSend(Map<String, List<String>> sendUserObj, Map<String,Map<String,String>> sendCuidVarsMap, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception {
    	
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 알림톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        // 전체 무결성 원칙을 적용하여 본발송 이상 시 발송 제외
        if(ObjectUtils.isNotEmpty(finalFailCuid)){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();
        
        try {
        	SendType sendType = SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND());
        	umsSendMsgBean.setChannelSendCount(sendType, sendUserObj.size());        	
        }catch (IllegalArgumentException e) { throw new Exception(umsSendMsgBean.getSTART_SEND_KIND() + " is not supported send type."); }

        // STEP 2: 예약 발송 일시 값이 존재 시 예약 발송 처리 - 예약발송 테이블 저장 >> UMS-AGENT의 의한 예약된 발송 재 요청.
        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(StringUtils.isNotBlank(umsSendMsgBean.getRESERVEDATE())){
            // 날짜 형식 검증
            if( umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm") == false) throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            // 예약발송 재처리 시 CSV 사용을 위한 전달
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean, csvSaveReturnMap);

            //  응답 바디 전문 - 채널별 발송 카운트 셋팅처리
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean, returnResultMap);
            return returnResultMap;
        }
        
        // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
        umsDao.inUmsSendMsg(umsSendMsgBean);

        // 2022.08.01 SMS 분할발송 로직 추가.
        boolean isDelaySend = delaySmsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
        if(!isDelaySend) {
            // STEP 5 : RCS 발송처리
            for (Entry<String, List<String>> e : sendUserObj.entrySet()) {
                String userId = e.getKey();
                List<String> userInfos = e.getValue();
                String mobileNum = userInfos.get(0);

                String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
                BaseRcsSendService sendService = umsChannelProviderFactory.getRcsProviderService(provider);
                sendService.umsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
            }
            //umsSendCommonService.getRcsProviderService().umsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
        }
        
        // 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean, returnResultMap);
        return returnResultMap;
    }    


    /**
     * CSV SMS 발송.
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsSmsCsvSend(Map<String, List<String>> sendUserObj, Map<String,Map<String,String>> sendCuidVarsMap, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean, SendType sendType) throws Exception {
        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // SMS 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        if(finalFailCuid!=null && finalFailCuid.size()>0){

        }

        int TOTAL_SEND_CNT = sendUserObj.size();

        // STEP 6: 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // 2022.08.01 SMS 분할발송 로직 추가.
        boolean isDelaySend = delaySmsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
        if(!isDelaySend){
            //지연 분할 발송이 아닐경우 단건 처리
            // STEP 8 : SMS 처리 후 response 데이타 만듬.
            if(sendType==SendType.SMS){
                umsSendMsgBean.setSMS_CNT(TOTAL_SEND_CNT);
                for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = getProvider(sendType.toString(), mobileNum);
                    BaseSmsSendService sendService = umsChannelProviderFactory.getSmsProviderService(provider);
                    sendService.umsSmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
                }
                //umsSendCommonService.getSmsProviderService().umsSmsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
            }else {
                if(sendType==SendType.MMS){
                    umsSendMsgBean.setMMS_CNT(TOTAL_SEND_CNT);
                }else{
                    umsSendMsgBean.setLMS_CNT(TOTAL_SEND_CNT);
                }
                for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = getProvider(sendType.toString(), mobileNum);
                    BaseMmsSendService sendService = umsChannelProviderFactory.getMmsProviderService(provider);
                    sendService.umsMmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
                }
                //umsSendCommonService.getMmsProviderService().umsMmsSend(sendUserObj, umsSendMsgBean, sendCuidVarsMap, false);
            }
        }

        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }


    /**
     * CSV 네이버톡 발송.
     * @return
     * @throws Exception
     */
    protected Map<String,Object> umsNaverCsvSend(Map<String, List<String>> sendUserObj, Map<String,Map<String,String>> sendCuidVarsMap, List<String> finalFailCuid, UmsSendMsgBean umsSendMsgBean) throws Exception {

        Map<String,Object> returnResultMap = new HashMap<String,Object>();

        // 네이버톡 전화번호없는 사용자 대체발송이 푸시 일 경우 대체발송 처리 로직 구현해야함. 대체발송처리 할 경우 setInitTotalSendCnt 총카운트수 조정해야함.
        // UMS 회원의 핸드폰번호는 필수값이므로 해당 프로세스는 필요 없을 것 같음.
        if(ObjectUtils.isNotEmpty(finalFailCuid)){
        }

        int TOTAL_SEND_CNT = sendUserObj.size();

        // STEP 2: 발송매체 별 발송카운트 초기 셋팅 후 원장에 저장. 0 : 신규발송. 0이 아니면 : UMS-AGENT의 의한 예약된 푸시발송 재 호출.
        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
        }

        // STEP 4 : 네이톡 발송카운트 셋팅.
        umsSendMsgBean.setNAVERT_CNT(TOTAL_SEND_CNT);

        // STEP 5 : 네이버톡 발송처리
      	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
    		String userId = e.getKey();
    		List<String> userInfos = e.getValue(); 
    		String mobileNum =userInfos.get(0);
    		
    		String provider = getProvider(umsSendMsgBean.getSTART_SEND_KIND(), mobileNum);
    		BaseNaverSendService sendService = umsChannelProviderFactory.getNaverProviderService(provider);
    		sendService.umsNaverSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, sendCuidVarsMap, false);
    	}
        //umsSendCommonService.getNaverProviderService().umsNaverSend(sendUserObj,umsSendMsgBean, sendCuidVarsMap, false);

        // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
        return returnResultMap;
    }

    protected Map<String,Object> confirmCsvFile(MultipartFile multipartCsvFile) throws Exception{
        //STEP 1 : CSV파일 점검.
        if(multipartCsvFile==null){
            throw new RequestErrException("발송할 CSV 파일이 존재하지 않습니다.");
        }
        String csvCharSet = StringUtil.getCharSet(multipartCsvFile.getInputStream());
        BufferedReader in;
        if("EUC-KR".equals(csvCharSet)) {
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("ISO-8859")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("CP949")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("UTF-8")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "UTF-8"));
        }else{
            throw new Exception("처리 할수 없는 파일 인코딩 입니다. :"+csvCharSet);
        }
        CSVReader reader = new CSVReader(in);

        String[] headFilds = reader.readNext();
        if(headFilds.length<3){
            throw new RequestErrException("필수정보 누락 필드갯수: #{아이디},#{이름},#{핸드폰번호}");
        }
        //STEP 1 : 반드시 포함되어야 하는 #{아이디}, #{이름}, #{핸드폰번호}가 CSV파일 헤더에 있는지 검증.
        boolean isCUID = false;
        boolean isCNAME = false;
        boolean isMOBILENUM = false;
        Map<String,Integer> varsIndexMap = new HashMap<String, Integer>();
        for(int i=0; i<headFilds.length; i++){
            String compairHeadFild = headFilds[i].trim();
            if("#{아이디}".equals(compairHeadFild)){
                isCUID = true;
            }
            if("#{이름}".equals(compairHeadFild)){
                isCNAME = true;
            }
            if("#{핸드폰번호}".equals(compairHeadFild)){
                isMOBILENUM = true;
            }
            varsIndexMap.put(compairHeadFild,i);
        }
//        logger.debug("varsIndexMap:"+gson.toJson(varsIndexMap));
        if(!isCUID || !isCNAME || !isMOBILENUM){
            throw new RequestErrException("필수정보 누락 : #{아이디},#{이름},#{핸드폰번호}");
        }
        Map<String,Object> returnMap = new HashMap<String,Object>();
        returnMap.put("csvReader", reader);
        returnMap.put("varIdxMap", varsIndexMap);
        return returnMap;
    }

    protected Map<String,Object> confirmMemberCsvFile(MultipartFile multipartCsvFile) throws Exception{
        //STEP 1 : CSV파일 점검.
        String csvCharSet = StringUtil.getCharSet(multipartCsvFile.getInputStream());

        // 로직추가 : 2021.07.02 작성자 : R.S.W  내용 : 인코딩 감지가 불가능할 경우 byte[]를 이용하여 EUC-KR 문자셋 확인 부분 추가
        if (csvCharSet == null || csvCharSet.length() == 0) {
            final String csvHeader = "#{아이디}";
            final byte[] csvHeaderEuckr = csvHeader.getBytes("EUC-KR");
            byte[] uploadCsvHeader = new byte[csvHeaderEuckr.length];

            try {
                System.arraycopy(multipartCsvFile.getBytes(), 0, uploadCsvHeader, 0, csvHeaderEuckr.length);
                if (Arrays.equals(uploadCsvHeader, csvHeaderEuckr)) {
                    csvCharSet = "EUC-KR";
                } else {
                    csvCharSet = "Unknown";
                }
            } catch (Exception e){
                csvCharSet = "Unknown";
                logger.error("Unknown type csv file!!!");
            }
        }

        BufferedReader in;
        if("EUC-KR".equals(csvCharSet)) {
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("ISO-8859")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("CP949")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "EUC-KR"));
        }else if(csvCharSet.startsWith("UTF-8")){
            in = new BufferedReader(new InputStreamReader(multipartCsvFile.getInputStream(), "UTF-8"));
        }else{
            throw new Exception("처리 할수 없는 파일 인코딩 입니다. :"+csvCharSet);
        }
        CSVReader reader = new CSVReader(in);
        String[] headFilds = reader.readNext();
        if(headFilds.length<1){
            throw new RequestErrException("필수정보 누락 필드갯수: #{아이디}");
        }
        //STEP 1 : 반드시 포함되어야 하는 #{아이디}가 CSV파일 헤더에 있는지 검증.
        boolean isCUID = false;
        Map<String,Integer> varsIndexMap = new HashMap<String, Integer>();
        for(int i=0; i<headFilds.length; i++){
            if("#{아이디}".equals(headFilds[i])){
                isCUID = true;
            }
            varsIndexMap.put(headFilds[i].trim(),i);
        }
//        logger.debug("varsIndexMap:"+gson.toJson(varsIndexMap));
        if(!isCUID){
            throw new RequestErrException("필수정보 누락 : #{아이디}");
        }
        Map<String,Object> returnMap = new HashMap<String,Object>();
        returnMap.put("csvReader", reader);
        returnMap.put("varIdxMap", varsIndexMap);
        return returnMap;
    }

    /**
     * 웹푸시유저와 푸시유저 공통으로 체크. appid 값으로 구분.
     * @param targetUsers
     * @param varsIndexMap
     * @param appid
     * @return
     * @throws Exception
     */
    protected Map<String,Object> chkCsvPushUser(final List<String[]> targetUsers, final Map<String,Integer> varsIndexMap, final String appid, boolean isRedisCheck) throws Exception{
        Map<String, Object> returnDataMap = new HashMap<String, Object>();
        
        Map<String, List<String>> regPushUserObj = new HashMap<String, List<String>>();
        Map<String, List<String>> notRegPushUserObj = new HashMap<String, List<String>>();
        Map<String,Map<String,String>> pushCuidVarsMap = new HashMap<String, Map<String, String>>();
        Map<String,Map<String,String>> notPushCuidVarsMap = new HashMap<String, Map<String, String>>();
        try {
            
            for(int i=0; i<targetUsers.size(); i++){
                String[] targetUserArr = targetUsers.get(i);
                String CUID = "";
                String CNAME = "";
                String MOBILENUM = "";
                Set<String> varsKey = varsIndexMap.keySet();
                Map<String,String> porsonalVarMap = new HashMap<String, String>();
                try {
                    //공백라인은 처리 하지 않는다.
                    if("".equals(targetUserArr[0].trim())){
                        continue;
                    }
                    for (String var : varsKey) {
                        if ("#{아이디}".equals(var)) {
                            CUID = targetUsers.get(i)[varsIndexMap.get(var)];
                        } else if ("#{이름}".equals(var)) {
                            CNAME = targetUsers.get(i)[varsIndexMap.get(var)];
                        } else if ("#{핸드폰번호}".equals(var)) {
                            MOBILENUM = targetUsers.get(i)[varsIndexMap.get(var)];
                        } else {
                            porsonalVarMap.put(var, targetUsers.get(i)[(varsIndexMap.get(var))]);
                        }
                    }
                }catch (Exception e){
                    logger.error("!!![csv file line number {}] {} cause : {} ",i, Arrays.toString(targetUsers.get(i)), e.toString());
                    e.printStackTrace();
                    continue;
                }
                List<String> nameMobileList = new ArrayList<String>();
                nameMobileList.add(MOBILENUM);
                nameMobileList.add(CNAME);
                regPushUserObj.put(CUID,nameMobileList); // 우선, 레디스에 조회하기 전까지는 모든 사용자를 푸시가입자로 셋팅해 둔다.
                pushCuidVarsMap.put(CUID,porsonalVarMap); //CUID별로 개별화 치환정보가 들어있다. ex){"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}

                // 레디스에서 푸시가입여부 확인
//                if(isRedisCheck) {
//                    redisMultiKeys.add(CUID); // Redis에서 조회 할 정보 set에 추가.
//                    if (redisMultiKeys.size() % MULTIKEY_SIZE == 0) {  // CUID 10000개가 모이면 레디스에서 조회.
//                        List<String> redis_pushUsers = redisTemplate.opsForHash().multiGet(appid + Constants.REDIS_CUID_TABLE, redisMultiKeys);
//                        for (int j = 0; j < redisMultiKeys.size(); j++) {
//                            if (redis_pushUsers.get(j) == null) {
//                                //푸시 가입되지 않은 사용자는 regPushUserObj에서 정보를 가져와 notRegPushUserObj에 넣고 삭제 한다.
//                                List<String> tmpNameMobiles = regPushUserObj.get(redisMultiKeys.get(j));
//                                notRegPushUserObj.put(redisMultiKeys.get(j), tmpNameMobiles);
//                                regPushUserObj.remove(redisMultiKeys.get(j));
//
//                                // cuid별 개인화 메세지 정보 저장맵 처리
//                                Map<String, String> tmpPersonalVarMap = pushCuidVarsMap.get(redisMultiKeys.get(j));
//                                notPushCuidVarsMap.put(redisMultiKeys.get(j), tmpPersonalVarMap);
//                                pushCuidVarsMap.remove(redisMultiKeys.get(j));
//                            }
//                        }
//                        redisMultiKeys.clear();
//                    }
//                }
            }

            // 레디스에서 푸시가입여부 확인
//            if(isRedisCheck) {
//                if (redisMultiKeys.size() > 0) {
//                    List<String> redis_pushUsers = redisTemplate.opsForHash().multiGet(appid + Constants.REDIS_CUID_TABLE, redisMultiKeys);
//                    for (int j = 0; j < redisMultiKeys.size(); j++) {
//                        if (redis_pushUsers.get(j) == null) {
//                            //푸시 가입되지 않은 사용자는 regPushUserObj에서 정보를 가져와 notRegPushUserObj에 넣고 삭제 한다.
//                            List<String> tmpNameMobiles = regPushUserObj.get(redisMultiKeys.get(j));
//                            notRegPushUserObj.put(redisMultiKeys.get(j), tmpNameMobiles);
//                            regPushUserObj.remove(redisMultiKeys.get(j));
//
//                            // cuid별 개인화 메세지 정보 저장맵 처리
//                            Map<String, String> tmpPersonalVarMap = pushCuidVarsMap.get(redisMultiKeys.get(j));
//                            notPushCuidVarsMap.put(redisMultiKeys.get(j), tmpPersonalVarMap);
//                            pushCuidVarsMap.remove(redisMultiKeys.get(j));
//                        }
//                    }
//                    redisMultiKeys.clear();
//                }
//            }

            returnDataMap.put("regPushUserObj", regPushUserObj);
            returnDataMap.put("notRegPushUserObj", notRegPushUserObj);
            returnDataMap.put("pushCuidVarMap",pushCuidVarsMap);
            returnDataMap.put("notPushCuidVarMap",notPushCuidVarsMap);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }
    // 푸시/웹푸시 공통
    protected Map<String,Object> chkCsvMemberPushUser(final List<String[]> targetUsers, final Map<String,Integer> varsIndexMap, UmsSendMsgBean umsSendMsgBean) throws Exception{
        Map<String, Object> returnDataMap = new HashMap<String, Object>();
        int MULTIKEY_SIZE = 1000;
        Map<String, List<String>> regPushUserMap = new HashMap<String, List<String>>();
        Map<String, List<String>> notRegPushUserMap = new HashMap<String, List<String>>();
        Map<String,Map<String,String>> pushCuidVarsMap = new HashMap<String, Map<String, String>>();
        Map<String,Map<String,String>> notPushCuidVarsMap = new HashMap<String, Map<String, String>>();
        try {
            List<String> redisMultiKeys = new ArrayList<String>();
            for(int i=0; i<targetUsers.size(); i++){
                String[] targetUserArr = targetUsers.get(i);
                String CUID = "";
                String CNAME = "";
                String MOBILENUM = "";
                Set<String> varsKey = varsIndexMap.keySet();
                Map<String,String> porsonalVarMap = new HashMap<String, String>();
                //공백라인은 처리 하지 않는다.
                if("".equals(targetUserArr[0].trim())){
                    continue;
                }
                for(String var : varsKey){
                    if("#{아이디}".equals(var)){
                        CUID = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else if("#{이름}".equals(var)){
                        CNAME = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else if("#{핸드폰번호}".equals(var)){
                        MOBILENUM = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else{
                        porsonalVarMap.put(var,targetUsers.get(i)[(varsIndexMap.get(var))]);
                    }
                }
                // STEP 1 : 아이디별로 치환정보를 모두 담아둔다.
                pushCuidVarsMap.put(CUID,porsonalVarMap); //CUID별로 개별화 치환정보가 들어있다. ex){"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
                // 레디스에서 푸시가입여부 확인
                redisMultiKeys.add(CUID); // Redis에서 조회 할 정보 set에 추가.
                if (redisMultiKeys.size() % MULTIKEY_SIZE == 0) {  // CUID 10000개가 모이면 레디스에서 조회.
                    String REDIS_APPID_KEY = umsSendMsgBean.getAPP_ID();
                    if (SendType.WPUSH == SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND())) {
                        REDIS_APPID_KEY = umsSendMsgBean.getWPUSH_DOMAIN();
                    }
                    List<String> redis_pushUsers = redisTemplate.opsForHash().multiGet(REDIS_APPID_KEY + Constants.REDIS_CUID_TABLE, redisMultiKeys);
                    List<String> redis_umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, redisMultiKeys);

                    for (int j = 0; j < redisMultiKeys.size(); j++) {
                        Object pushUserInfoObj = redis_pushUsers.get(j);
                        Object umsUserInfoObj = redis_umsUsers.get(j);
                        String cuid = redisMultiKeys.get(j);
                        if (pushUserInfoObj == null) {
                            // 대체발송이 있을 경우. 우선 무조건 핸드폰 번호를 null로 넣어 둔다. 이유는 해당 대체발송에서 실패처리 할 수 있도록하기 위해.
                            notRegPushUserMap.put(cuid, null);
                            //대체발송 개인화 변수맵에 담음.
                            notPushCuidVarsMap.put(cuid, pushCuidVarsMap.get(cuid));
                            // 푸시 개인화 변수맵에서 삭제처리.
                            pushCuidVarsMap.remove(cuid);

                            // 대체발송이 있는경우.redis_umsUsers에 등록되어 있는 유저인지 확인.
                            if (umsUserInfoObj != null) {
                                // 대체발송이 있으므로 redis_umsUsers에 등록되어 있는 사용자인지 확인하여 핸드폰번호 넣음.
                                List<String> hpNameList = new ArrayList<String>();
                                try {
                                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                                    hpNameList.add(umsUserInfos.get(0));
                                    hpNameList.add(umsUserInfos.get(1));
                                    //대체발송 처리 맵에 타켓유저담음
                                    notRegPushUserMap.put(cuid, hpNameList);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            // 푸시서비스 가입이 되어 있는 사용자일 경우.
                            regPushUserMap.put(cuid, null);
                            // 푸시 발송 유저정보에 핸드폰정보 담음.
                            if (umsUserInfoObj != null) {
                                List<String> hpNameList = new ArrayList<String>();
                                try {
                                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                                    hpNameList.add(umsUserInfos.get(0));
                                    hpNameList.add(umsUserInfos.get(1));
                                    //대체발송 처리 맵에 타켓유저담음
                                    regPushUserMap.put(cuid, hpNameList);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    redisMultiKeys.clear();
                }
            }
            // 레디스에서 푸시가입여부 확인
            if (redisMultiKeys.size() > 0) {
                List<String> redis_pushUsers = redisTemplate.opsForHash().multiGet(umsSendMsgBean.getAPP_ID() + Constants.REDIS_CUID_TABLE, redisMultiKeys);
                List<String> redis_umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, redisMultiKeys);

                for (int j = 0; j < redisMultiKeys.size(); j++) {
                    Object pushUserInfoObj = redis_pushUsers.get(j);
                    Object umsUserInfoObj = redis_umsUsers.get(j);
                    String cuid = redisMultiKeys.get(j);
                    if (pushUserInfoObj == null) {
                        // 푸시서비스 가입이 안 되어 있는 유저일 경우. 대체발송이 있는 경우 와 대체발송이 없는 경우로 구분
                        notRegPushUserMap.put(cuid, null);
                        //대체발송 개인화 변수맵에 담음.
                        notPushCuidVarsMap.put(cuid, pushCuidVarsMap.get(cuid));
                        // 푸시 개인화 변수맵에서 삭제처리.
                        pushCuidVarsMap.remove(cuid);

                        // 대체발송이 있는경우.redis_umsUsers에 등록되어 있는 유저인지 확인.
                        if (umsUserInfoObj != null) {
                            // 대체발송이 있으므로 redis_umsUsers에 등록되어 있는 사용자인지 확인하여 핸드폰번호 넣음.
                            List<String> hpNameList = new ArrayList<String>();
                            try {
                                List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                                hpNameList.add(umsUserInfos.get(0));
                                hpNameList.add(umsUserInfos.get(1));
                                //대체발송 처리 맵에 타켓유저담음
                                notRegPushUserMap.put(cuid, hpNameList);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        // 푸시서비스 가입이 되어 있는 사용자일 경우.
                        regPushUserMap.put(cuid, null);
                        // 푸시 발송 유저정보에 핸드폰정보 담음.
                        if (umsUserInfoObj != null) {
                            List<String> hpNameList = new ArrayList<String>();
                            try {
                                List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                                hpNameList.add(umsUserInfos.get(0));
                                hpNameList.add(umsUserInfos.get(1));
                                //대체발송 처리 맵에 타켓유저담음
                                regPushUserMap.put(cuid, hpNameList);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                redisMultiKeys.clear();
            }

            returnDataMap.put("regPushUserObj", regPushUserMap);
            returnDataMap.put("notRegPushUserObj", notRegPushUserMap);
            returnDataMap.put("pushCuidVarMap",pushCuidVarsMap);
            returnDataMap.put("notPushCuidVarMap",notPushCuidVarsMap);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }
    /**
     * CSV파일정보를 이용하여 {"아이디",["핸드폰번호","이름"]} sendUserObj맵과 {"아이디",{"VAR1","so"}} sendCuidVarsMap읆 만든다.
     * @param targetUsers
     * @param varsIndexMap
     * @return
     * @throws Exception
     */
    protected Map<String,Object> makeSendUserMap(final List<String[]> targetUsers,final Map<String,Integer> varsIndexMap) throws Exception{
        Map<String, Object> returnDataMap = new HashMap<String, Object>();
        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        Map<String,Map<String,String>> sendCuidVarsMap = new HashMap<String, Map<String, String>>();
        try {
            for(int i=0; i<targetUsers.size(); i++){
                String[] targetUserArr = targetUsers.get(i);
                String CUID = "";
                String CNAME = "";
                String MOBILENUM = "";
                Set<String> varsKey = varsIndexMap.keySet();
                Map<String,String> porsonalVarMap = new HashMap<String, String>();
                //공백라인은 처리 하지 않는다.
                if("".equals(targetUserArr[0].trim())){
                    continue;
                }
                for(String var : varsKey){
                    if("#{아이디}".equals(var)){
                        CUID = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else if("#{이름}".equals(var)){
                        CNAME = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else if("#{핸드폰번호}".equals(var)){
                        MOBILENUM = targetUsers.get(i)[varsIndexMap.get(var)];
                    }else{
                        porsonalVarMap.put(var,targetUsers.get(i)[(varsIndexMap.get(var))]);
                    }
                }
                List<String> nameMobileList = new ArrayList<String>();
                nameMobileList.add(MOBILENUM);
                nameMobileList.add(CNAME);
                sendUserObj.put(CUID,nameMobileList); // 우선, 레디스에 조회하기 전까지는 모든 사용자를 푸시가입자로 셋팅해 둔다.
                sendCuidVarsMap.put(CUID,porsonalVarMap); //CUID별로 개별화 치환정보가 들어있다. ex){"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
            }

            returnDataMap.put("sendUserObj", sendUserObj);
            returnDataMap.put("sendCuidVarsMap",sendCuidVarsMap);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    // 실패 된 사용자는 무조건 DB에 남기도록 수정해야함. 레거시 요청건수와 DB 건수가 일치하도록!!!
    protected Map<String,Object> makeSendMemberUserMap(final List<String[]> targetUsers, final Map<String,Integer> varsIndexMap) throws Exception{
    	// 발송 필요 데이터 맵 - 하위 1, 2, 3 포함 
        Map<String, Object> returnDataMap 				 = new HashMap<String, Object>();
        // 1. 수신자별 기본 변수(핸드폰번호, 이름)-리스트 정보
        Map<String, List<String>> sendUserObj			 = new HashMap<String, List<String>>();
        // 2. 수신자별 개인화 치환변수매핑 맵 정보
        Map<String, Map<String,String>> sendCuidVarsMap	 = new HashMap<String, Map<String, String>>();
        // 3. 실패처리된 수신자 리스트
        List<String> finalFailCuid = new ArrayList<String>();
        
        // 레디즈 조회 배치 카운트
        int MULTIKEY_SIZE = 10000;
        try {
            List<String> redisMultiKeys = new ArrayList<String>();
            Set<String> varsKey = varsIndexMap.keySet();
            for(int i=0; i<targetUsers.size(); i++){
            	
            	String[] targetUserArr = targetUsers.get(i);

                String CUID = "";
                String CNAME = "";
                String MOBILENUM = "";                
                Map<String,String> porsonalVarMap = new HashMap<String, String>();
                
                for(String var : varsKey){
                	// 치환변수 인덱스 추출
                	int index = varsIndexMap.get(var);
                	// 치환변수 값 추출
                	String value = targetUserArr[index];
                	switch(var) {
	                	case "#{아이디}":
	                		CUID = value;
	                		break;
	                	case "#{이름}":
	                		CNAME = value;
	                		break;
	                	case "#{핸드폰번호}":
	                		MOBILENUM = value;
	                		break;
	                	default :
	                		porsonalVarMap.put(var, value);
	                		break;
                	}
                }
                
            	//멤버 발송은 CUID 없을 시 제외, 중복 발송 수신자 제외 
            	if(StringUtils.isBlank(CUID) || sendUserObj.containsKey(CUID)) continue;
                
                // 프로세서에서 실패 처리 DB 입력 대상
                sendUserObj.put(CUID, null);
                
                // CUID별 개별화 치환정보- {"USER1":{"#{금액}":"1000","#{날짜}":"2019-04-02"}}
                sendCuidVarsMap.put(CUID, porsonalVarMap); 
                
                // REDIS 조회 CUID 리스트에 추가
                redisMultiKeys.add(CUID); 

                // 조회 배치카운트 도달 시 REDIS 조회
                if(redisMultiKeys.size()%MULTIKEY_SIZE == 0){
                	// 레디스 등록 멤버 조회
                    List<String> redis_umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, redisMultiKeys);
                    
                    for(int j=0; j<redisMultiKeys.size(); j++){
         
                        Object umsUserInfoObj = redis_umsUsers.get(j);
                        String cuid = redisMultiKeys.get(j);
                        
                        // REDIS 등록 여부 확인
                        if(umsUserInfoObj != null){
                            try {
                            	// 전화번호, 이름 정보 리스트
                                List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                                // 발송 수신자 맵에 담기
                                sendUserObj.put(cuid, umsUserInfos);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else{
                            finalFailCuid.add(cuid);
                        }
                    }
                    redisMultiKeys.clear();
                }
            }
            
            // 배치카운트 도달 하지 않은 경우 - 잔존 데이터 처리
            if(redisMultiKeys.size()>0){
            	// 레디스 등록 멤버 조회
                List<String> redis_umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, redisMultiKeys);

                for(int j=0; j<redisMultiKeys.size(); j++){
                	
                    Object umsUserInfoObj = redis_umsUsers.get(j);
                    String cuid = redisMultiKeys.get(j);
                    
                    // REDIS 등록 여부 확인
                    if(umsUserInfoObj!=null){
                        try {
                        	// 전화번호, 이름 정보 리스트
                            List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                            // 발송 수신자 맵에 담기
                            sendUserObj.put(cuid, umsUserInfos);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else{
                        finalFailCuid.add(cuid);
                    }
                }
                redisMultiKeys.clear();
            }

            returnDataMap.put("sendUserObj", sendUserObj);
            returnDataMap.put("sendCuidVarsMap",sendCuidVarsMap);
            returnDataMap.put("finalFailCuid",finalFailCuid);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }
    
    protected Map<String,String> saveCsv(MultipartFile CSV_FILE) throws Exception{
        TEMPDIR = TEMPDIR.trim();
        String chkLastStr = TEMPDIR.substring(TEMPDIR.length()-1);

        UUID csvUUID = UUID.randomUUID();
        String csvFileName = csvUUID.toString();
        String fullFileName = TEMPDIR + csvFileName+".csv";
        if(!"/".equals(chkLastStr)){
            fullFileName = TEMPDIR + "/" + csvFileName+".csv";
        }

        File file = new File(fullFileName);
        CSV_FILE.transferTo(file);
        String orgFileName = CSV_FILE.getOriginalFilename();
        Map<String,String> csvSaveReturnMap = new HashMap<String, String>();
        csvSaveReturnMap.put("saveCsvFileAbsSrc", file.getAbsolutePath());
        csvSaveReturnMap.put("orgCsvFileName", orgFileName);

        return csvSaveReturnMap;
    }
    
    private String getProvider(String channel, String identifyKey) { return umsSendCommonService.getAllotterManager().getProvider(channel, identifyKey); }
}
