package kr.uracle.ums.core.service;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 1..
 * 해당 클래스는 UMS에 가입되어 있지 않는 타겟팅 대상자를 처리 하는 클래스이다. 따라서 반드시 핸드폰번호와 이름을 받아야 한다.
 * Target 구조는 JsonObject이다. 예 {"아이디1":["핸드폰번호1","이름1"], "아이디2":["핸드폰번호2","이름2"]...}
 */
@Service
@SuppressWarnings("unchecked")
public class UmsSendService extends UmsSendBase{

    @Value("${UMS.TEMPDIR:}")
    private String TEMPDIR;

    /**
     * [비회원] 푸시 발송.
     * 처리로직 :
     * 1. DB원장(예약발송원장/실시간발송원장) 정보 만듬.
     * 2. 푸시발송대상자 체크 후 푸시발송대상자가 아닌경우 대체발송 로직 구현
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsPushSend(UmsSendMsgBean umsSendMsgBean)  throws Exception{
       
        // STEP 1 : 발송메세지 채널별 검증
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2 : 푸시가입유저, 미가입 유저 분리.
//        Map<String,Object> pushUserChkMap = super.umsSendCommonService.chkPushUser(umsSendMsgBean); // 레디스를 이용하여 푸시가입자 필터
        Map<String,Object> pushUserChkMap = super.umsSendCommonService.chkPushUserNotUseRedis(umsSendMsgBean); // 푸시가입자 필터를 UPMC에 위임.

        Map<String,List<String>> regPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("regPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,List<String>> notRegPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("notRegPushUserObj"); //{"아이디":["핸드폰번호","이름"]}

        // STEP 3: 푸시발송처리
        Map<String,Object> returnResultMap = super.umsPushSend(regPushUserMap,notRegPushUserMap,umsSendMsgBean);

        return returnResultMap;
    }

    /**
     * [비회원] 웹푸시 발송.
     * 처리로직 :
     * 1. DB원장(예약발송원장/실시간발송원장) 정보 만듬.
     * 2. 웹푸시발송대상자 체크 후 푸시발송대상자가 아닌경우 대체발송 로직 구현
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsWPushSend(UmsSendMsgBean umsSendMsgBean)  throws Exception{

        // STEP 1 : 발송메세지 채널별 검증
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2 : 웹푸시가입유저, 미가입 유저 분리.
        Map<String,Object> wpushUserChkMap = super.umsSendCommonService.chkPushUserNotUseRedis(umsSendMsgBean);
        Map<String,List<String>> regPushUserMap = (Map<String,List<String>>)wpushUserChkMap.get("regPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,List<String>> notRegPushUserMap = (Map<String,List<String>>)wpushUserChkMap.get("notRegPushUserObj"); //{"아이디":["핸드폰번호","이름"]}

        // STEP 3: 웹푸시발송처리
        Map<String,Object> returnResultMap = super.umsWPushSend(regPushUserMap,notRegPushUserMap,umsSendMsgBean);

        return returnResultMap;
    }

    /**
     * [비회원] UMS 알림톡 발송
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsAltSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception{

        //STEP 1: 카톡 알림톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getALLIMTOLK_TEMPLCODE().trim())) {
            // STEP 1 : 발송메세지 채널별 검증
            super.checkSendMsg(umsSendMsgBean);

        }else{
            throw new RequestErrException(" ALLIMTOLK TEMPLEATE CODE is NULL");
        }

        // STEP 2: 알림톡 발송유저수 추출.
        Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();

        // STEP 3: 알림톡발송처리
        Map<String,Object> returnResultMap = super.umsAltSend(sendUserObj, null, umsSendMsgBean);

        return returnResultMap;
    }

    /**
     * [비회원] UMS 친구톡 발송
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsFrtSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception{

    	if(StringUtils.isBlank(umsSendMsgBean.getFRIENDTOLK_MSG())) {
    		throw new RequestErrException(" FRIENDTOLK TEMPLEATE CODE is NULL");    		
    	}
        
        // STEP 1 : 발송메세지 채널별 검증 - 친구톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
    	super.checkSendMsg(umsSendMsgBean);
    	
        // STEP 2: 친구톡 발송유저수 추출.
        Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();

        // STEP 3: 친구톡발송처리
        Map<String,Object> returnResultMap = super.umsFrtSend(sendUserObj, null, umsSendMsgBean);

        return returnResultMap;
    }
    
    /**
     * [비회원] UMS RCS 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsRcsSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

    	// STEP 1: 메시지 검증
    	if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT()) && StringUtils.isBlank(umsSendMsgBean.getRCS_MSG()))throw new RequestErrException(" RCS_MSG is NULL");
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2: 발송유저수 추출.
        Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();

        // STEP 3: 발송처리
        Map<String,Object> returnResultMap = super.umsRcsSend(sendUserObj, null, umsSendMsgBean);

        return returnResultMap;
    }


    /**
     * [비회원]UMS SMS 발송
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsSmsSend(UmsSendMsgBean umsSendMsgBean, Locale locale, SendType sendType) throws Exception{

        // STEP 1: [SMS 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다. 개인화 변수가 있으면 우선은 MMS로 발송처리하나.MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(!"".equals(umsSendMsgBean.getSMS_MSG())) {
            super.checkSendMsg(umsSendMsgBean);
        }else{
            throw new RequestErrException(" SMS MESSAGE is NULL");
        }

        // STEP 2: SMS 발송유저수 추출.
        Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();

        // STEP 3 : SMS 발송
        Map<String,Object> returnResultMap = super.umsSmsSend(sendUserObj, null, umsSendMsgBean, sendType);


        return returnResultMap;
    }

    /**
     * [비회원]UMS 네이버톡 발송
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsNaverSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception{
        // STEP 1 : 네이버톡 템플릿코드 검증
        if(!"".equals(umsSendMsgBean.getNAVER_TEMPL_ID().trim())) {
            // STEP 1 : 발송메세지 채널별 검증
            super.checkSendMsg(umsSendMsgBean);

        }else{
            throw new RequestErrException(" 네이버톡 TEMPLEATE CODE is NULL");
        }

        // STEP 2: 네이버톡 발송유저수 추출.
        Map<String, List<String>> sendUserObj = umsSendMsgBean.getTARGET_USERS();

        // STEP 3 : 네이버톡 발송
        Map<String,Object> returnResultMap = super.umsNaverSend(sendUserObj, null, umsSendMsgBean);


        return returnResultMap;
    }
}
