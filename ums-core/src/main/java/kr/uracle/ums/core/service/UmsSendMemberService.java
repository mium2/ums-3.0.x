package kr.uracle.ums.core.service;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 12..
 * 해당 클래스는 UMS에 가입된 회원정보를 이용하여 타켓아이디만을 이용하여 발송 역할을 수행 하는 클래스이다.
 * 처리는 타겟 아이디를 이용하여 REDIS UMS_MEMBER이라는 키테이블을 조회하여 발송정보(핸드폰번호, 이름)을 구해온다.
 */
@Service
@SuppressWarnings("unchecked")
public class UmsSendMemberService extends UmsSendBase{
    @Autowired(required = true)
    private RedisTemplate redisTemplate;
    @Autowired
    private Gson gson;
    @Value("${UMS.TEMPDIR:}")
    private String TEMPDIR;

    /**
     * UMS 등록회원 푸시 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserPushSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception {
    
        //STEP 1 : 발송메세지 채널별 검증
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2: 푸시가입유저, 미가입 유저 분리. REDIS UMS_MEMBER 키테이블을 이용, 해당 사용자의 정보를 가져온다.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 7 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        // STEP 8 : 푸시 대상 사용자 정보를 조회한다.
        List<Object> pushRegUsers = redisTemplate.opsForHash().multiGet(umsSendMsgBean.getAPP_ID() + Constants.REDIS_CUID_TABLE, targetCuids);

        Map<String, List<String>> regPushUserMap = new HashMap<String, List<String>>();
        Map<String, List<String>> notRegPushUserMap = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 9 : 푸시 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object pushUserInfoObj = pushRegUsers.get(i);
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            if (pushUserInfoObj != null) {
                // 푸시 발송 가능 대상자 Map<String,List<String>> 형태의 데이타로 셋팅.
                regPushUserMap.put(cuid, null);
                if (umsUserInfoObj != null) {
                    List<String> hpNameList = new ArrayList<String>();
                    try {
                        List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                        hpNameList.add(umsUserInfos.get(0));
                        hpNameList.add(umsUserInfos.get(1));
                        regPushUserMap.put(cuid, hpNameList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 푸시 발송 가능 대상자가 아닌 경우. 대체발송이 있고, UMS 가입대상 정보가 있는 경우에마 대체발송정보에 담고 나머지는 실패처리
                // 실패시 대체발송 채널여부 확인
                Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);
                // 푸시서비스 가입자 아닌 경우 대체발송 처리 하는 카운트 원장에 셋팅.
                umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

                if (umsSendMsgBean.getFAIL_RETRY_SENDTYPE() != null && umsSendMsgBean.getFAIL_RETRY_SENDTYPE().size()>0 && umsUserInfoObj != null) {
                    // 대체발송 정보에 담음
                    notRegPushUserMap.put(cuid, null);
                    List<String> hpNameList = new ArrayList<String>();
                    try {
                        List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                        hpNameList.add(umsUserInfos.get(0));
                        hpNameList.add(umsUserInfos.get(1));
                        notRegPushUserMap.put(cuid, hpNameList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 최종실패처리
                    finalFailCuid.add(cuid);
                }
            }
        }

        // STEP 3: 푸시발송처리
        Map<String,Object> returnResultMap = super.umsPushSend(regPushUserMap,notRegPushUserMap,umsSendMsgBean);
        return returnResultMap;
    }

    /**
     * UMS 등록회원 웹푸시 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserWPushSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception {

        //STEP 1 : 발송메세지 채널별 검증
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2: 웹푸시가입유저, 미가입 유저 분리. REDIS UMS_MEMBER 키테이블을 이용, 해당 사용자의 정보를 가져온다.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 7 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        // STEP 8 : 웹푸시 대상 사용자 정보를 조회한다.
        List<Object> pushRegUsers = redisTemplate.opsForHash().multiGet(umsSendMsgBean.getWPUSH_DOMAIN() + Constants.REDIS_CUID_TABLE, targetCuids);

        Map<String, List<String>> regPushUserMap = new HashMap<String, List<String>>();
        Map<String, List<String>> notRegPushUserMap = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 9 : 웹푸시 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object pushUserInfoObj = pushRegUsers.get(i);
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            if (pushUserInfoObj != null) {
                // 푸시 발송 가능 대상자 Map<String,List<String>> 형태의 데이타로 셋팅.
                regPushUserMap.put(cuid, null);
                if (umsUserInfoObj != null) {
                    List<String> hpNameList = new ArrayList<String>();
                    try {
                        List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                        hpNameList.add(umsUserInfos.get(0));
                        hpNameList.add(umsUserInfos.get(1));
                        regPushUserMap.put(cuid, hpNameList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 웹푸시 발송 가능 대상자가 아닌 경우. 대체발송이 있고, UMS 가입대상 정보가 있는 경우에마 대체발송정보에 담고 나머지는 실패처리
                // 실패시 대체발송 채널여부 확인
                Set<SendType> failRetrySendTypeSet = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.WPUSH);
                // 푸시서비스 가입자 아닌 경우 대체발송 처리 하는 카운트 원장에 셋팅.
                umsSendMsgBean.setFAIL_RETRY_SENDTYPE(failRetrySendTypeSet);

                if (umsSendMsgBean.getFAIL_RETRY_SENDTYPE() != null && umsSendMsgBean.getFAIL_RETRY_SENDTYPE().size()>0 && umsUserInfoObj != null) {
                    // 대체발송 정보에 담음
                    notRegPushUserMap.put(cuid, null);
                    List<String> hpNameList = new ArrayList<String>();
                    try {
                        List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                        hpNameList.add(umsUserInfos.get(0));
                        hpNameList.add(umsUserInfos.get(1));
                        notRegPushUserMap.put(cuid, hpNameList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 최종실패처리
                    finalFailCuid.add(cuid);
                }
            }
        }

        // STEP 3: 웹푸시발송처리
        Map<String,Object> returnResultMap = super.umsWPushSend(regPushUserMap,notRegPushUserMap,umsSendMsgBean);
        return returnResultMap;
    }

    /**
     * UMS 등록회원 알림톡 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserAltSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception {
       
        //STEP 1: 카톡 알림톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getALLIMTOLK_TEMPLCODE().trim())) {
            // STEP 1 : 발송메세지 채널별 검증
            super.checkSendMsg(umsSendMsgBean);

        }else{
            throw new RequestErrException(" ALLIMTOLK TEMPLEATE CODE is NULL");
        }

        // STEP 2: 알림톡 발송유저수 추출.
        // REDIS UMS_MEMBER 키테이블을 이용, 해당 사용자의 정보를 가져온다.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 5 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 6 : 푸시 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            // 쓰레드에서 실패처리를 할 수 있도록 아이디는 등록하고 핸드폰번호는 null로 등록한다.
            sendUserObj.put(cuid, null);
            // UMS 가입대상 정보가 있는 경우에 알림톡 발송정보에 핸드폰번호 담음.
            if (umsUserInfoObj != null) {
                List<String> hpNameList = new ArrayList<String>();
                try {
                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                    hpNameList.add(umsUserInfos.get(0));
                    hpNameList.add(umsUserInfos.get(1));
                    sendUserObj.put(cuid, hpNameList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 최종실패처리
                finalFailCuid.add(cuid);
            }
        }

        // STEP 3: 알림톡발송처리
        Map<String,Object> returnResultMap = super.umsAltSend(sendUserObj, finalFailCuid, umsSendMsgBean);
        return returnResultMap;
    }

    /**
     * UMS 등록된 회원 친구톡 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserFrtSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception {

        //STEP 1: 친구톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getFRIENDTOLK_MSG().trim())) {
            // STEP 1 : 발송메세지 채널별 검증
            super.checkSendMsg(umsSendMsgBean);

        }else{
            throw new RequestErrException(" FRIENDTOLK TEMPLEATE CODE is NULL");
        }

        // STEP 2: 친구톡 발송유저수 추출.
        // STEP 4: 친구톡 발송유저수 추출.
        // STEP 4: REDIS UMS_MEMBER 키테이블을 이용, 해당 사용자의 정보를 가져온다.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 5 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 6 : 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            // 쓰레드에서 실패처리를 할 수 있도록 아이디는 등록하고 핸드폰번호는 null로 등록한다.
            sendUserObj.put(cuid, null);
            // UMS 가입대상 정보가 있는 경우에 알림톡 발송정보에 핸드폰번호 담음.
            if (umsUserInfoObj != null) {
                List<String> hpNameList = new ArrayList<String>();
                try {
                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                    hpNameList.add(umsUserInfos.get(0));
                    hpNameList.add(umsUserInfos.get(1));
                    sendUserObj.put(cuid, hpNameList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 최종실패처리
                finalFailCuid.add(cuid);
            }
        }

        // STEP 3: 친구톡발송처리
        Map<String,Object> returnResultMap = super.umsFrtSend(sendUserObj, finalFailCuid, umsSendMsgBean);

        return returnResultMap;
    }
    
    /**
     * UMS 등록회원 RCS 발송
     *
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserRCSSend(UmsSendMsgBean umsSendMsgBean) throws Exception {
               
    	// STEP 1: 메시지 검증
        super.checkSendMsg(umsSendMsgBean);

        // STEP 2: 발송 대상 추출 - 기본 리스트, 에러시 단건
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) { targetCuids.add(umsSendMsgBean.getCUIDS()); }
        
        // STEP 3 : 발송 대상 정보 추출 - REDIS UMS_MEMBER 키테이블을 이용
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 4 : 발송 대상자 정보 조립
        for (int i = 0; i < targetCuids.size(); i++) {
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            
        	//멤버 발송은 CUID 없을 시 제외, 중복 발송 수신자 제외
        	if(StringUtils.isBlank(cuid) || sendUserObj.containsKey(cuid)) continue;
   
            // 프로세서에서 실패 처리 DB 입력 대상
            sendUserObj.put(cuid, null);
            
            // UMS 가입대상 정보가 있는 경우에 알림톡 발송정보에 핸드폰번호 담음.
            if (umsUserInfoObj != null) {
                try {
                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                    sendUserObj.put(cuid, umsUserInfos);
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                finalFailCuid.add(cuid);
            }
        }

        // STEP 3: 알림톡발송처리
        Map<String,Object> returnResultMap = super.umsRcsSend(sendUserObj, finalFailCuid, umsSendMsgBean);
        return returnResultMap;
    }
    

    /**
     * UMS SMS 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserSmsSend(UmsSendMsgBean umsSendMsgBean, Locale locale, SendType sendType) throws Exception {
       
        // STEP 1: [SMS 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다. 개인화 변수가 있으면 우선은 MMS로 발송처리하나.MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(!"".equals(umsSendMsgBean.getSMS_MSG())) {
            super.checkSendMsg(umsSendMsgBean);
        }else{
            throw new RequestErrException(" SMS MESSAGE is NULL");
        }

        // STEP 2: SMS 발송유저수 추출.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 4 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 5 : 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            // UMS 가입대상 정보가 있는 경우에 알림톡 발송정보에 핸드폰번호 담음.
            if (umsUserInfoObj != null) {
            	// 쓰레드에서 실패처리를 할 수 있도록 아이디는 등록하고 핸드폰번호는 null로 등록한다.
            	sendUserObj.put(cuid, null);
                List<String> hpNameList = new ArrayList<String>();
                try {
                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                    hpNameList.add(umsUserInfos.get(0));
                    hpNameList.add(umsUserInfos.get(1));
                    sendUserObj.put(cuid, hpNameList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 최종실패처리
                finalFailCuid.add(cuid);
            }
        }

        // STEP 3 : SMS 발송
        Map<String,Object> returnResultMap = super.umsSmsSend(sendUserObj, finalFailCuid, umsSendMsgBean, sendType);

        return returnResultMap;
    }

    /**
     * UMS 등록회원 네이버톡 발송
     *
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String, Object> umsMemberUserNaverSend(UmsSendMsgBean umsSendMsgBean, Locale locale) throws Exception {

        //STEP 1: 네이버톡은 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getNAVER_TEMPL_ID().trim())) {
            // STEP 1 : 발송메세지 채널별 검증
            super.checkSendMsg(umsSendMsgBean);

        }else{
            throw new RequestErrException(" 네이버 템플릿아이디 is NULL");
        }

        // STEP 2: 네이버톡 발송유저수 추출.
        // REDIS UMS_MEMBER 키테이블을 이용, 해당 사용자의 정보를 가져온다.
        List<String> targetCuids = new ArrayList<String>();
        try {
            String targetUserJsonStr = umsSendMsgBean.getCUIDS();
            targetCuids = gson.fromJson(targetUserJsonStr, List.class);
        } catch (Exception e) {
            // 타켓팅 아이디가 jsons 스트링이 아닌 경우 단건으로 처리.
            targetCuids.add(umsSendMsgBean.getCUIDS());
        }
        // STEP 5 : REDIS UMS_MEMBER 키테이블에서 해당 유저의 발송정보를 가져온다.
        List<Object> umsUsers = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, targetCuids);

        Map<String, List<String>> sendUserObj = new HashMap<String, List<String>>();
        List<String> finalFailCuid = new ArrayList<String>();

        // STEP 6 : 발송 대상자에 UMS_MEMBER 키테이블을 이용 핸드폰번호와 이름을 넣는다
        for (int i = 0; i < targetCuids.size(); i++) {
            Object umsUserInfoObj = umsUsers.get(i);
            String cuid = targetCuids.get(i);
            // 쓰레드에서 실패처리를 할 수 있도록 아이디는 등록하고 핸드폰번호는 null로 등록한다.
            sendUserObj.put(cuid, null);
            // UMS 가입대상 정보가 있는 경우에 알림톡 발송정보에 핸드폰번호 담음.
            if (umsUserInfoObj != null) {
                List<String> hpNameList = new ArrayList<String>();
                try {
                    List<String> umsUserInfos = gson.fromJson(umsUserInfoObj.toString(), List.class);
                    hpNameList.add(umsUserInfos.get(0));
                    hpNameList.add(umsUserInfos.get(1));
                    sendUserObj.put(cuid, hpNameList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 최종실패처리
                finalFailCuid.add(cuid);
            }
        }

        // STEP 3: 네이버발송처리
        Map<String,Object> returnResultMap = super.umsNaverSend(sendUserObj, finalFailCuid, umsSendMsgBean);
        return returnResultMap;
    }
}
