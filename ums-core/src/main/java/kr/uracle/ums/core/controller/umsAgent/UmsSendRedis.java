package kr.uracle.ums.core.controller.umsAgent;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.service.UmsChannelProviderFactory;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.UmsSendMacroService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by Y.B.H(mium2) on 2021-03-18.
 * 해당 컨트롤은 대량발송메세지를 레디스에 등록하고 UMSAgent에서 일감을 가져와 다시 UMS API를 호출하는 발송 방식
 */
@SuppressWarnings("unchecked")
@RequestMapping(value="/api/agent")
@Controller
public class UmsSendRedis{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private PushSendService pushSendService;
    @Autowired(required = true)
    private WPushSendService wpushSendService;
    @Autowired(required = true)
    private UmsSendMacroService umsSendMacroService;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;

    @Autowired(required = true)
    private Gson gson;

    @Value("${SMS.PROVIDER:LGU}")
    private String SMS_PROVIDER;

    @RequestMapping(value = {"/umsSendApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String umsSendApi_ZK(Locale locale, HttpServletRequest request, HttpServletResponse response){
        try {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                //접근이 허용되지 않은 아이피일경우 실패처리함.
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(),"");
            }
            /////////////////////////////////////////////////////////////////////////////////////////
            // 해당 아래부분의 로직을 구현 하세요.
            /////////////////////////////////////////////////////////////////////////////////////////
            String sendPayLoad = request.getParameter("PAYLOAD");
            UmsSendMsgRedisBean umsSendMsgRedisBean = gson.fromJson(sendPayLoad, UmsSendMsgRedisBean.class);
            UmsSendMsgBean umsSendMsgBean = null;
            if(umsSendMsgRedisBean.getTRANS_TYPE().equals(TransType.BATCH.toString())){
                umsSendMsgBean = new UmsSendMsgBean(TransType.BATCH.toString());
            }else{
                umsSendMsgBean = new UmsSendMsgBean(TransType.REAL.toString());
            }
            //START_SEND_KIND는 SendType 중 하나임. (PUSH, KKOALT, KKOFRT, RCS_SMS, RCS_LMS, RCS_MMS, RCS_TMPLT, RCS_FB_SMS, RCS_FB_LMS, SMS, LMS, MMS, NAVERT)
            umsSendMsgBean.setSTART_SEND_KIND(umsSendMsgRedisBean.getSTART_SEND_KIND());
            umsSendMsgBean.setUMS_SEQNO(umsSendMsgRedisBean.getUMS_SEQNO());
            umsSendMsgBean.setRESERVE_SEQNO(umsSendMsgRedisBean.getRESERVE_SEQNO());
            umsSendMsgBean.setSENDERID(umsSendMsgRedisBean.getSENDERID());
            umsSendMsgBean.setSENDGROUPCODE(umsSendMsgRedisBean.getSENDGROUPCODE());
            umsSendMsgBean.setSEND_MACRO_CODE(umsSendMsgRedisBean.getSEND_MACRO_CODE());
            umsSendMsgBean.setSEND_MACRO_ORDER(umsSendMsgRedisBean.getSEND_MACRO_ORDER());
            umsSendMsgBean.setCUIDS(umsSendMsgRedisBean.getCUIDS());
            umsSendMsgBean.setMSG_TYPE(umsSendMsgRedisBean.getMSG_TYPE());
            
            umsSendMsgBean.setAPP_ID(umsSendMsgRedisBean.getAPP_ID());
            umsSendMsgBean.setPUSH_TYPE(umsSendMsgRedisBean.getPUSH_TYPE());
            umsSendMsgBean.setPUSH_MSG(umsSendMsgRedisBean.getPUSH_MSG());
            umsSendMsgBean.setSOUNDFILE(umsSendMsgRedisBean.getSOUNDFILE());
            umsSendMsgBean.setBADGENO(umsSendMsgRedisBean.getBADGENO());
            umsSendMsgBean.setPRIORITY(umsSendMsgRedisBean.getPRIORITY());
            umsSendMsgBean.setEXT(umsSendMsgRedisBean.getEXT());
            umsSendMsgBean.setSENDERCODE(umsSendMsgRedisBean.getSENDERCODE());
            umsSendMsgBean.setSERVICECODE(umsSendMsgRedisBean.getSERVICECODE());
            umsSendMsgBean.setTARGET_USER_TYPE(umsSendMsgRedisBean.getTARGET_USER_TYPE());
            umsSendMsgBean.setDB_IN(umsSendMsgRedisBean.getDB_IN());
            umsSendMsgBean.setPUSH_FAIL_SMS_SEND(umsSendMsgRedisBean.getPUSH_FAIL_SMS_SEND());

            umsSendMsgBean.setTITLE(umsSendMsgRedisBean.getTITLE());
            umsSendMsgBean.setATTACHFILE(umsSendMsgRedisBean.getATTACHFILE());
            umsSendMsgBean.setRESERVEDATE(umsSendMsgRedisBean.getRESERVEDATE());
            umsSendMsgBean.setORG_RESERVEDATE(umsSendMsgRedisBean.getORG_RESERVEDATE());

            //웹푸시
            umsSendMsgBean.setWPUSH_DOMAIN(umsSendMsgRedisBean.getWPUSH_DOMAIN());
            umsSendMsgBean.setWPUSH_TEMPL_ID(umsSendMsgRedisBean.getWPUSH_TEMPL_ID());
            umsSendMsgBean.setWPUSH_TITLE(umsSendMsgRedisBean.getWPUSH_TITLE());
            umsSendMsgBean.setWPUSH_MSG(umsSendMsgRedisBean.getWPUSH_MSG());
            umsSendMsgBean.setWPUSH_EXT(umsSendMsgRedisBean.getWPUSH_EXT());
            umsSendMsgBean.setWPUSH_ICON(umsSendMsgRedisBean.getWPUSH_ICON());
            umsSendMsgBean.setWPUSH_LINK(umsSendMsgRedisBean.getWPUSH_LINK());
            umsSendMsgBean.setWPUSH_BADGENO(umsSendMsgRedisBean.getWPUSH_BADGENO());

            umsSendMsgBean.setCALLBACK_NUM(umsSendMsgRedisBean.getCALLBACK_NUM());
            // 알림톡/친구톡
            umsSendMsgBean.setKKO_TITLE(umsSendMsgBean.getKKO_TITLE());
            umsSendMsgBean.setALLIMTOLK_TEMPLCODE(umsSendMsgRedisBean.getALLIMTOLK_TEMPLCODE());
            umsSendMsgBean.setALLIMTALK_MSG(umsSendMsgRedisBean.getALLIMTALK_MSG());
            umsSendMsgBean.setKKOALT_SVCID(umsSendMsgRedisBean.getKKOALT_SVCID());
            umsSendMsgBean.setREPLACE_VARS(umsSendMsgRedisBean.getREPLACE_VARS());
            umsSendMsgBean.setFRIENDTOLK_MSG(umsSendMsgRedisBean.getFRIENDTOLK_MSG());
            umsSendMsgBean.setKKOFRT_SVCID(umsSendMsgRedisBean.getKKOFRT_SVCID());
            umsSendMsgBean.setPLUS_ID(umsSendMsgRedisBean.getPLUS_ID());
            umsSendMsgBean.setFRT_TEMPL_ID(umsSendMsgRedisBean.getFRT_TEMPL_ID());
            umsSendMsgBean.setKKO_IMG_PATH(umsSendMsgRedisBean.getKKO_IMG_PATH());
            umsSendMsgBean.setKKO_IMG_LINK_URL(umsSendMsgRedisBean.getKKO_IMG_LINK_URL());
            umsSendMsgBean.setKKO_BTNS(umsSendMsgRedisBean.getKKO_BTNS());

            //SMS
            umsSendMsgBean.setSMS_TITLE(umsSendMsgRedisBean.getSMS_TITLE());
            umsSendMsgBean.setSMS_MSG(umsSendMsgRedisBean.getSMS_MSG());
            umsSendMsgBean.setSMS_TEMPL_ID(umsSendMsgRedisBean.getSMS_TEMPL_ID());
            umsSendMsgBean.setMMS_IMGURL(umsSendMsgRedisBean.getMMS_IMGURL());

            //RCS
            umsSendMsgBean.setRCS_TITLE(umsSendMsgRedisBean.getRCS_TITLE());
            umsSendMsgBean.setRCS_MSG(umsSendMsgRedisBean.getRCS_MSG());
            umsSendMsgBean.setRCS_MMS_INFO(umsSendMsgRedisBean.getRCS_MMS_INFO());
            umsSendMsgBean.setRCS_TYPE(umsSendMsgRedisBean.getRCS_TYPE());
            umsSendMsgBean.setFOOTER(umsSendMsgRedisBean.getFOOTER());
            if(StringUtils.isNotBlank(umsSendMsgRedisBean.getCOPY_ALLOWED()))umsSendMsgBean.setCOPY_ALLOWED(umsSendMsgRedisBean.getCOPY_ALLOWED());
            umsSendMsgBean.setEXPIRY_OPTION(umsSendMsgRedisBean.getEXPIRY_OPTION());
            
            umsSendMsgBean.setIMG_GROUP_KEY(umsSendMsgRedisBean.getIMG_GROUP_KEY());
            umsSendMsgBean.setIMG_GROUP_CNT(umsSendMsgRedisBean.getIMG_GROUP_CNT());
            umsSendMsgBean.setRCS_IMG_INSERT(umsSendMsgRedisBean.isRCS_IMG_INSERT());
            if(umsSendMsgRedisBean.getRCS_IMG_PATH()!=null && umsSendMsgRedisBean.getRCS_IMG_PATH().size()>0) {
                umsSendMsgBean.setRCS_IMG_PATH(umsSendMsgRedisBean.getRCS_IMG_PATH());
            }

            umsSendMsgBean.setRCS_TEMPL_ID(umsSendMsgRedisBean.getRCS_TEMPL_ID());
            umsSendMsgBean.setBRAND_ID(umsSendMsgRedisBean.getBRAND_ID());
            umsSendMsgBean.setRCS_MSGBASE_ID(umsSendMsgRedisBean.getRCS_MSGBASE_ID());
            umsSendMsgBean.setRCS_OBJECT(umsSendMsgRedisBean.getRCS_OBJECT());
            umsSendMsgBean.setBTN_OBJECT(umsSendMsgRedisBean.getBTN_OBJECT());
            umsSendMsgBean.setRCS_BTN_CNT(umsSendMsgRedisBean.getRCS_BTN_CNT());
            umsSendMsgBean.setRCS_BTN_TYPE(umsSendMsgRedisBean.getRCS_BTN_TYPE());
            umsSendMsgBean.setRCS_IMG_INSERT(umsSendMsgRedisBean.isRCS_IMG_INSERT());

            //네이버톡
            umsSendMsgBean.setNAVER_TEMPL_ID(umsSendMsgRedisBean.getNAVER_TEMPL_ID());
            umsSendMsgBean.setNAVER_MSG(umsSendMsgRedisBean.getNAVER_MSG());
            umsSendMsgBean.setNAVER_PROFILE(umsSendMsgRedisBean.getNAVER_PROFILE());
            umsSendMsgBean.setNAVER_BUTTONS(umsSendMsgRedisBean.getNAVER_BUTTONS());
            umsSendMsgBean.setNAVER_PARTNERKEY(umsSendMsgRedisBean.getNAVER_PARTNERKEY());
            umsSendMsgBean.setNAVER_IMGHASH(umsSendMsgRedisBean.getNAVER_IMGHASH());

            // 고객 거래식별고유키 셋팅.
            umsSendMsgBean.setCUST_TRANSGROUPKEY(umsSendMsgRedisBean.getCUST_TRANSGROUPKEY());
            umsSendMsgBean.setCUST_TRANSKEY(umsSendMsgRedisBean.getCUST_TRANSKEY());
            umsSendMsgBean.setMIN_START_TIME(umsSendMsgRedisBean.getMIN_START_TIME());
            umsSendMsgBean.setMAX_END_TIME(umsSendMsgRedisBean.getMAX_END_TIME());
            umsSendMsgBean.setFATIGUE_YN(umsSendMsgRedisBean.getFATIGUE_YN());

            umsSendMsgBean.setVAR1(umsSendMsgRedisBean.getVAR1());
            umsSendMsgBean.setVAR2(umsSendMsgRedisBean.getVAR2());
            umsSendMsgBean.setVAR3(umsSendMsgRedisBean.getVAR3());
            umsSendMsgBean.setVAR4(umsSendMsgRedisBean.getVAR4());
            umsSendMsgBean.setVAR5(umsSendMsgRedisBean.getVAR5());
            umsSendMsgBean.setVAR6(umsSendMsgRedisBean.getVAR6());
            umsSendMsgBean.setVAR7(umsSendMsgRedisBean.getVAR7());
            umsSendMsgBean.setVAR8(umsSendMsgRedisBean.getVAR8());
            umsSendMsgBean.setVAR9(umsSendMsgRedisBean.getVAR9());

            // 대량발송(UMS회원전체발송, 푸시회원전체발송, 조직도회원전체발송을 단껀으로 레디스에 넣음.
            Map<String,List<String>> putTargetUsers = new HashMap<>();
            if(umsSendMsgRedisBean.getTARGET_PHONEINFOS()==null || umsSendMsgRedisBean.getTARGET_PHONEINFOS().size()==0){
                putTargetUsers.put(umsSendMsgRedisBean.getTARGET_CUID(),null);
            }else{
                putTargetUsers.put(umsSendMsgRedisBean.getTARGET_CUID(),umsSendMsgRedisBean.getTARGET_PHONEINFOS());
            }
            umsSendMsgBean.setTARGET_USERS(putTargetUsers);

            Map<String,Map<String,String>> cuidVarMap = null;
            if(umsSendMsgRedisBean.getREPLACE_VAR_MAP().size()>0){
                cuidVarMap = new HashMap<>();
                cuidVarMap.put(umsSendMsgRedisBean.getTARGET_CUID(), umsSendMsgRedisBean.getREPLACE_VAR_MAP());
            }

            Map<String,Object> returnResultMap = new HashMap<>();

            // 매크로설정의 의해 최초발송 채널 정보 가져옴
            SendType sendType = SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND());

            //타입별 발송처리. 이곳은 무조건 단건메세지임.
            Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();
            Set<SendType> daeCheSendTypeSet = new HashSet<>();
            switch (sendType){
                case PUSH :
                    // STEP 4 : 푸시 일 경우 UPMC 결과에러 또는 UPMC 통신에러시를 위해 대체발송채널 셋팅한다.
                   daeCheSendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);
                    if(daeCheSendTypeSet!=null && daeCheSendTypeSet.size()>0){
                        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(daeCheSendTypeSet);
                    }
                    pushSendService.pushMapListSend(sendUserObj,umsSendMsgBean, cuidVarMap, false); //푸시단건발송. 타겟팅 정보. TARGET_USERS : {"아이디":["핸드폰번호","이름"]}
                    break;
                case WPUSH:
                    daeCheSendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.WPUSH);
                    if(daeCheSendTypeSet!=null && daeCheSendTypeSet.size()>0){
                        umsSendMsgBean.setFAIL_RETRY_SENDTYPE(daeCheSendTypeSet);
                    }
                    wpushSendService.wpushMapListSend(sendUserObj,umsSendMsgBean, cuidVarMap, false); //웹푸시단건발송. 타겟팅 정보. TARGET_USERS : {"아이디":["핸드폰번호","이름"]}
                    break;
                case KKOALT:
                	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseKkoAltSendService sendService = umsChannelProvierFactory.getKkoAltProviderService(provider); 
                		sendService.umsKkoAllimTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                	//umsSendCommonService.getKkoAltProviderService().umsKkoAllimTolkSend(sendUserObj, umsSendMsgBean, cuidVarMap, false);
                    break;
                case KKOFRT:
                	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseKkoFrtSendService sendService = umsChannelProvierFactory.getKkoFrtProviderService(provider); 
                		sendService.umsKkoFriendTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                    //umsSendCommonService.getKkoFrtProviderService().umsKkoFriendTolkSend(sendUserObj,umsSendMsgBean, null, false);
                    break;
                case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseRcsSendService sendService = umsChannelProvierFactory.getRcsProviderService(provider);
                		sendService.umsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                	//umsSendCommonService.getRcsProviderService().umsSend(sendUserObj, umsSendMsgBean, cuidVarMap,false);
                    break;
                case SMS:
                	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseSmsSendService sendService = umsChannelProvierFactory.getSmsProviderService(provider);
                		sendService.umsSmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                    //umsSendCommonService.getSmsProviderService().umsSmsSend(sendUserObj, umsSendMsgBean, null, false);
                    break;
                case LMS: case MMS:
                  	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseMmsSendService sendService = umsChannelProvierFactory.getMmsProviderService(provider);
                		sendService.umsMmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                	//umsSendCommonService.getMmsProviderService().umsMmsSend(sendUserObj,umsSendMsgBean, null, false);
                	break;
                case NAVERT:
                  	for(Entry<String, List<String>> e : sendUserObj.entrySet()) {
                		String userId = e.getKey();
                		List<String> userInfos = e.getValue(); 
                		String mobileNum =userInfos.get(0);
                		
                		String provider = getProvider(sendType.toString(), mobileNum);
                		BaseNaverSendService sendService = umsChannelProvierFactory.getNaverProviderService(provider);
                		sendService.umsNaverSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, false);
                	}
                    // umsSendCommonService.getNaverProviderService().umsNaverSend(sendUserObj, umsSendMsgBean, cuidVarMap,false);
                    break;
            }

            // 발송처리 프로세스 호출
            return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), returnResultMap, request.getRequestURI(), sendPayLoad);

            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), request.getRequestURI(), "");
        }
    }
    private String getProvider(String channel, String identifyKey) { return umsSendCommonService.getAllotterManager().getProvider(channel, identifyKey); }
}
