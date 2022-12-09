package kr.uracle.ums.core.service.send.thread;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.UmsSendMacroService;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseThreadRedis extends Thread{
@SuppressWarnings("unchecked")
	
	protected Gson gson = new Gson();
    protected UmsSendMsgBean umsSendMsgBean;
    protected SentInfoManager sentInfoManager;
    protected UmsDao umsDao;
    protected UmsSendCommonService umsSendCommonService;
    protected UmsSendMacroService umsSendMacroService;

    protected RedisTemplate redisTemplate;
    protected Map<String,String> varMap = new HashMap<String,String>();
    protected boolean isReplaceVars = false;
    
    protected Map<String,Object> returnResultMap = new HashMap<String,Object>();
    
    protected boolean isStop = false;
    protected int TOTAL_SEND_CNT = 0;
    protected int LIMITSECOND = 0;
    protected boolean isLimitSend = false;
    
    protected String REDISTYPE = "1"; //1: 레디스 1대(마스터로 설치), 2 : 레디스 2대이상(센티넬 설치), 3: 클러스터로 설치
    
    public BaseThreadRedis(UmsSendMsgBean umsSendMsgBean){
        this.umsSendCommonService = UmsInitListener.wContext.getBean(UmsSendCommonService.class);
        this.umsSendMacroService = UmsInitListener.wContext.getBean(UmsSendMacroService.class);
        this.sentInfoManager = UmsInitListener.wContext.getBean(SentInfoManager.class);
        this.REDISTYPE = UmsInitListener.webProperties.getProperty("redis.type","1");
        this.umsSendMsgBean = umsSendMsgBean;
    }
    
    public abstract Map<String,Object> processInfo() throws  Exception;
    protected abstract void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception;
    protected abstract int send() ;
    
    
    @Override
    public void destroy() {
    	isStop = true;
    }
    
    @Override
	public void run() {
		int rslt = send();
	}
    protected void init() throws Exception{

        //예약날짜 올바른 형식의 날자인지 검증
        if(StringUtils.isNotBlank(umsSendMsgBean.getRESERVEDATE())){
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
        }

        // STEP 1: [푸시 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
        Map<String, List<String>> msgVarCheckMap = umsSendCommonService.getReplaceVars(umsSendMsgBean.getPUSH_MSG());
        if(msgVarCheckMap.get("commonVars").size()>0){
            umsSendMsgBean.setReplaceMsg(true);
        }

        //STEP 2 : 카톡 알림톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(StringUtils.isNotBlank(umsSendMsgBean.getALLIMTOLK_TEMPLCODE().trim())) {
            umsSendCommonService.chkAltMsg(umsSendMsgBean);
        }

        // STEP 3: [친구톡 메세지 검증] 친구톡 발송내용이 있는지 검증, #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리
        if(StringUtils.isNotBlank(umsSendMsgBean.getFRIENDTOLK_MSG())) {
            umsSendCommonService.chkFrtMsg(umsSendMsgBean);
        }

        // STEP 4: [SMS 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다. 개인화 변수가 있으면 우선은 MMS로 발송처리하나.MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(StringUtils.isNotBlank(umsSendMsgBean.getSMS_MSG())) {
            umsSendCommonService.chkSmsMsg(umsSendMsgBean);
        }

        // 알림톡 개별화 치환변수 처리
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
            Map<String,Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(),Map.class);
            Set<String> replaceVarsKey = replaceVarsMap.keySet();
            for(String varKey : replaceVarsKey){
                varMap.put(varKey, replaceVarsMap.get(varKey).toString());
            }
            this.isReplaceVars = true;
        }
    }


    protected UmsSendMsgRedisBean makeUmsSendMsgRedisBean(UmsSendMsgBean umsSendMsgBean) throws Exception{
        return makeUmsSendMsgRedisBean(umsSendMsgBean, null);
    }

    protected UmsSendMsgRedisBean makeUmsSendMsgRedisBean(UmsSendMsgBean umsSendMsgBean, String msgVars) throws Exception{
        UmsSendMsgRedisBean umsSendMsgRedisBean = new UmsSendMsgRedisBean();
        umsSendMsgRedisBean.setTRANS_TYPE(umsSendMsgBean.getTRANS_TYPE());
        umsSendMsgRedisBean.setSTART_SEND_KIND(umsSendMsgBean.getSTART_SEND_KIND());
        umsSendMsgRedisBean.setUMS_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        umsSendMsgRedisBean.setRESERVE_SEQNO(""+umsSendMsgBean.getRESERVE_SEQNO());
        umsSendMsgRedisBean.setSENDERID(umsSendMsgBean.getSENDERID());
        umsSendMsgRedisBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        umsSendMsgRedisBean.setSEND_MACRO_CODE(umsSendMsgBean.getSEND_MACRO_CODE());
        umsSendMsgRedisBean.setSEND_MACRO_ORDER(umsSendMsgBean.getSEND_MACRO_ORDER());
        umsSendMsgRedisBean.setCUIDS(umsSendMsgBean.getCUIDS());
        umsSendMsgRedisBean.setMSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        
        umsSendMsgRedisBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
        umsSendMsgRedisBean.setPUSH_TYPE(umsSendMsgBean.getPUSH_TYPE());
        umsSendMsgRedisBean.setPUSH_MSG(umsSendMsgBean.getPUSH_MSG());
        umsSendMsgRedisBean.setSOUNDFILE(umsSendMsgBean.getSOUNDFILE());
        umsSendMsgRedisBean.setBADGENO(umsSendMsgBean.getBADGENO());
        umsSendMsgRedisBean.setPRIORITY(umsSendMsgBean.getPRIORITY());
        umsSendMsgRedisBean.setEXT(umsSendMsgBean.getEXT());
        umsSendMsgRedisBean.setSENDERCODE(umsSendMsgBean.getSENDERCODE());
        umsSendMsgRedisBean.setSERVICECODE(umsSendMsgBean.getSERVICECODE());
        umsSendMsgRedisBean.setTARGET_USER_TYPE(umsSendMsgBean.getTARGET_USER_TYPE());
        umsSendMsgRedisBean.setDB_IN(umsSendMsgBean.getDB_IN());
        umsSendMsgRedisBean.setPUSH_FAIL_SMS_SEND(umsSendMsgBean.getPUSH_FAIL_SMS_SEND());

        umsSendMsgRedisBean.setTITLE(umsSendMsgBean.getTITLE());
        umsSendMsgRedisBean.setATTACHFILE(umsSendMsgBean.getATTACHFILE());
        umsSendMsgRedisBean.setRESERVEDATE(umsSendMsgBean.getRESERVEDATE());
        umsSendMsgRedisBean.setORG_RESERVEDATE(umsSendMsgBean.getORG_RESERVEDATE());

        //웹푸시 정보셋팅
        umsSendMsgRedisBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
        umsSendMsgRedisBean.setWPUSH_TITLE(umsSendMsgBean.getWPUSH_TITLE());
        umsSendMsgRedisBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
        umsSendMsgRedisBean.setWPUSH_TEMPL_ID(umsSendMsgBean.getWPUSH_TEMPL_ID());
        umsSendMsgRedisBean.setWPUSH_EXT(umsSendMsgBean.getWPUSH_EXT());
        umsSendMsgRedisBean.setWPUSH_ICON(umsSendMsgBean.getWPUSH_ICON());
        umsSendMsgRedisBean.setWPUSH_LINK(umsSendMsgBean.getWPUSH_LINK());
        umsSendMsgRedisBean.setWPUSH_BADGENO(umsSendMsgBean.getWPUSH_BADGENO());

        //알림톡/친구톡/RCS/SMS 공통
        umsSendMsgRedisBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        //알림톡
        umsSendMsgRedisBean.setALLIMTOLK_TEMPLCODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
        umsSendMsgRedisBean.setALLIMTALK_MSG(umsSendMsgBean.getALLIMTALK_MSG());
        umsSendMsgRedisBean.setKKOALT_SVCID(umsSendMsgBean.getKKOALT_SVCID());
        umsSendMsgRedisBean.setREPLACE_VARS(umsSendMsgBean.getREPLACE_VARS());
        umsSendMsgRedisBean.setREPLACE_VAR_MAP(umsSendMsgBean.getREPLACE_VAR_MAP());
        if(msgVars!=null && !"".equals(msgVars)){
            umsSendMsgRedisBean.setREPLACE_VARS(msgVars);
            Map<String, String> magVarMap = gson.fromJson(msgVars, Map.class);
            umsSendMsgRedisBean.setREPLACE_VAR_MAP(magVarMap);
        }
        //친구톡
        umsSendMsgRedisBean.setFRIENDTOLK_MSG(umsSendMsgBean.getFRIENDTOLK_MSG());
        umsSendMsgRedisBean.setKKOFRT_SVCID(umsSendMsgBean.getKKOFRT_SVCID());
        umsSendMsgRedisBean.setPLUS_ID(umsSendMsgBean.getPLUS_ID());
        umsSendMsgRedisBean.setFRT_TEMPL_ID(umsSendMsgBean.getFRT_TEMPL_ID());
        umsSendMsgRedisBean.setKKO_IMG_PATH(umsSendMsgBean.getKKO_IMG_PATH());
        umsSendMsgRedisBean.setKKO_IMG_LINK_URL(umsSendMsgBean.getKKO_IMG_LINK_URL());
        //알림톡/친구톡 공통
        umsSendMsgRedisBean.setKKO_BTNS(umsSendMsgBean.getKKO_BTNS());

        //SMS
        umsSendMsgRedisBean.setSMS_TITLE(umsSendMsgBean.getSMS_TITLE());
        umsSendMsgRedisBean.setSMS_MSG(umsSendMsgBean.getSMS_MSG());
        umsSendMsgRedisBean.setSMS_TEMPL_ID(umsSendMsgBean.getSMS_TEMPL_ID());
        umsSendMsgRedisBean.setMMS_IMGURL(umsSendMsgBean.getMMS_IMGURL());
        //RCS
        umsSendMsgRedisBean.setRCS_TITLE(umsSendMsgBean.getRCS_TITLE());
        umsSendMsgRedisBean.setRCS_MSG(umsSendMsgBean.getRCS_MSG());
        umsSendMsgRedisBean.setIMG_GROUP_KEY(umsSendMsgBean.getIMG_GROUP_KEY());
        umsSendMsgRedisBean.setIMG_GROUP_CNT(umsSendMsgBean.getIMG_GROUP_CNT());
        umsSendMsgRedisBean.setRCS_IMG_INSERT(umsSendMsgBean.isRCS_IMG_INSERT());
        umsSendMsgRedisBean.setRCS_IMG_PATH(umsSendMsgBean.getRCS_IMG_PATH());
        umsSendMsgRedisBean.setRCS_TEMPL_ID(umsSendMsgBean.getRCS_TEMPL_ID());
        umsSendMsgRedisBean.setBRAND_ID(umsSendMsgBean.getBRAND_ID());
        umsSendMsgRedisBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        umsSendMsgRedisBean.setRCS_TYPE(umsSendMsgBean.getRCS_TYPE());
        umsSendMsgRedisBean.setRCS_OBJECT(umsSendMsgBean.getRCS_OBJECT());
        umsSendMsgRedisBean.setBTN_OBJECT(umsSendMsgBean.getBTN_OBJECT());
        umsSendMsgRedisBean.setRCS_BTN_CNT(umsSendMsgBean.getRCS_BTN_CNT());
        umsSendMsgRedisBean.setRCS_BTN_TYPE(umsSendMsgBean.getRCS_BTN_TYPE());

        // 네이버톡
        umsSendMsgRedisBean.setNAVER_TEMPL_ID(umsSendMsgBean.getNAVER_TEMPL_ID());
        umsSendMsgRedisBean.setNAVER_MSG(umsSendMsgBean.getNAVER_MSG());
        umsSendMsgRedisBean.setNAVER_PROFILE(umsSendMsgBean.getNAVER_PROFILE());
        umsSendMsgRedisBean.setNAVER_BUTTONS(umsSendMsgBean.getNAVER_BUTTONS());
        umsSendMsgRedisBean.setNAVER_PARTNERKEY(umsSendMsgBean.getNAVER_PARTNERKEY());
        umsSendMsgRedisBean.setNAVER_IMGHASH(umsSendMsgBean.getNAVER_IMGHASH());

        // 고객 거래식별고유키 셋팅.
        umsSendMsgRedisBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
        umsSendMsgRedisBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
        umsSendMsgRedisBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
        umsSendMsgRedisBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
        umsSendMsgRedisBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

        umsSendMsgRedisBean.setVAR1(umsSendMsgBean.getVAR1());
        umsSendMsgRedisBean.setVAR2(umsSendMsgBean.getVAR2());
        umsSendMsgRedisBean.setVAR3(umsSendMsgBean.getVAR3());
        umsSendMsgRedisBean.setVAR4(umsSendMsgBean.getVAR4());
        umsSendMsgRedisBean.setVAR5(umsSendMsgBean.getVAR5());
        umsSendMsgRedisBean.setVAR6(umsSendMsgBean.getVAR6());
        umsSendMsgRedisBean.setVAR7(umsSendMsgBean.getVAR7());
        umsSendMsgRedisBean.setVAR8(umsSendMsgBean.getVAR8());
        umsSendMsgRedisBean.setVAR9(umsSendMsgBean.getVAR9());

        return umsSendMsgRedisBean;
    }

    /**
     * 최초발송 채널 발송카운트 셋팅
     * @param sendType
     * @param TOTAL_SEND_CNT
     */
    protected void setChannelSendCnt(SendType sendType, int TOTAL_SEND_CNT){
        umsSendMsgBean.setSEND_CNT(TOTAL_SEND_CNT);
        switch (sendType){
            case PUSH:
                umsSendMsgBean.setPUSH_SEND_CNT(TOTAL_SEND_CNT);
                break;
            case WPUSH:
                umsSendMsgBean.setWPUSH_SEND_CNT(TOTAL_SEND_CNT);
                break;
            case KKOALT:
                umsSendMsgBean.setALLIMTOLK_CNT(TOTAL_SEND_CNT);
                break;
            case KKOFRT:
                umsSendMsgBean.setFRIENDTOLK_CNT(TOTAL_SEND_CNT);
                break;
            case SMS:
                umsSendMsgBean.setSMS_CNT(TOTAL_SEND_CNT);
                break;
            case LMS:
                umsSendMsgBean.setLMS_CNT(TOTAL_SEND_CNT);
                break;
            case MMS:
                umsSendMsgBean.setMMS_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_SMS:
                umsSendMsgBean.setRCS_SMS_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_LMS:
                umsSendMsgBean.setRCS_LMS_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_MMS:
                umsSendMsgBean.setRCS_MMS_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_FREE:
                umsSendMsgBean.setRCS_FREE_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_CELL:
                umsSendMsgBean.setRCS_CELL_CNT(TOTAL_SEND_CNT);
                break;
            case RCS_DESC:
                umsSendMsgBean.setRCS_DESC_CNT(TOTAL_SEND_CNT);
                break;
            case NAVERT:
                umsSendMsgBean.setNAVERT_CNT(TOTAL_SEND_CNT);
                break;
        }
    }
    

}
