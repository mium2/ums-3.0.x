package kr.uracle.ums.core.service.send.thread;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.exception.NotExistUserException;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;

/**
 * 해당 클래스는 푸시서비스에 가입된 정보를 가져와 매크로에 정의된 첫번째 발송채널 정보를 이용하여 레디스에 개별비회원 발송등록 처리만 한다.
 * [매우중요] 확인: 해당 발송은 정의된 매크로코드 MACRO_001로 무조건 셋팅한다. 최초발송이 무조건 푸시부턱 발송되도록 하여야 한다.
 *
 */
@SuppressWarnings("unchecked")
public class AllPushUserToRedisSender extends BaseThreadRedis {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Logger reqSendLogger = LoggerFactory.getLogger("sentRedisLogger");
    
    private int MULTIKEY_SIZE = 1000;
    private int putTotalCnt = 0;

    private int TOTAL_SEND_CNT = 0;
    private int UMS_TOTAL_SEND_CNT = 0;
  
    private Set<SendType> daecheChannelSet;

    private String REDIS_APPID_KEY = "";

    private SendType sendType = SendType.PUSH;

    public AllPushUserToRedisSender(UmsSendMsgBean umsSendMsgBean, String limitSecond, String limitCnt){
        super(umsSendMsgBean);
        super.redisTemplate = (RedisTemplate) UmsInitListener.wContext.getBean("redisTemplate");
        this.umsDao = UmsInitListener.wContext.getBean(UmsDao.class);
        
        try {
        	this.LIMITSECOND = Integer.parseInt(limitSecond);
        	int size = Integer.parseInt(limitCnt);
        	this.MULTIKEY_SIZE = size ==0? MULTIKEY_SIZE : size;
        }catch (Exception e) {
        	LIMITSECOND = 0;
		}
        
        this.REDIS_APPID_KEY = umsSendMsgBean.getAPP_ID();
    }



    public Map<String,Object> processInfo() throws  Exception{
    	// 요청정보 검토 및 초기화 작덥.
        init(); 

        sendType = SendType.valueOf(super.umsSendMsgBean.getSTART_SEND_KIND());
        if(sendType==SendType.WPUSH){
            REDIS_APPID_KEY = super.umsSendMsgBean.getWPUSH_DOMAIN().trim();
        }else{
            REDIS_APPID_KEY = super.umsSendMsgBean.getAPP_ID().trim();
        }

        TOTAL_SEND_CNT = Long.valueOf(redisTemplate.opsForHash().size(REDIS_APPID_KEY+ Constants.REDIS_CUID_TABLE)).intValue();
        
        // 푸시실패시 대체발송 채널이 있을 경우. UMS 회원정보가 있으면 해당 정보에서 핸드폰번호를 가져오기 위해.
        UMS_TOTAL_SEND_CNT = Long.valueOf(redisTemplate.opsForHash().size(Constants.REDIS_UMS_MEMBER_TABLE)).intValue();
        daecheChannelSet = super.umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, sendType);
        if(TOTAL_SEND_CNT ==0){
            throw new NotExistUserException("웹푸시서비스에 가입된 사용자가 존재하지 않습니다.");
        }

        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(StringUtils.isNotBlank(super.umsSendMsgBean.getRESERVEDATE())){
            umsDao.inUmsSendReserveMsg(super.umsSendMsgBean);
        }else{
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(super.umsSendMsgBean);
            TransType transType = TransType.valueOf(super.umsSendMsgBean.getTRANS_TYPE());
            // 이 부분은 의미가 없어 보임. 이유는 발송카운트 프로세스에서 Total은 사용하지 않는 것으로 변경됨.
//            sentInfoManager.setInitTotalSendCnt(transType, umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);

            //프로그래스바를 위해서 발송카운트 셋팅
            sentInfoManager.setInitThreadSendCnt(transType, "T_"+super.umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
            returnResultMap.put("PROGRESS_SEQNO", "T_"+super.umsSendMsgBean.getUMS_SEQNO());
        }

//        //[공통필수] : Response 발송수 공통로직으로 채널별 발송카운트 UmsSendMsgBean에 셋팅. 원장테이블 발송카운트는 Process에서 처리한다.
        super.setChannelSendCnt(sendType, TOTAL_SEND_CNT);

        //[공통필수] : 채널별 발송결과 응답 카운트 셋팅 및 발송원장 시퀀스 셋팅
        umsSendCommonService.setRspChannelSendCnt(super.umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }

    @Override
    protected int send() {

        String REDIS_KEY_TABLE = REDIS_APPID_KEY+ Constants.REDIS_CUID_TABLE;
        int loopCnt = 0;
        int fetchCnt = 0;
        int processEndCnt = 0;
        RedisConnection redisConnection = null;
        JedisCommands jedisCommands = null;
        try {
            boolean isEnd = false;
            ScanParams scanParams = new ScanParams();
            scanParams.count(MULTIKEY_SIZE);
            ScanResult<Map.Entry<String, String>> scanResult;
            String nextCursor = "0";

            if ("3".equals(REDISTYPE)) {	//레디스 클러스트로 설치
                redisConnection = redisTemplate.getConnectionFactory().getClusterConnection();
            } else {
                redisConnection = redisTemplate.getConnectionFactory().getConnection();
            }
            jedisCommands = (JedisCommands) redisConnection.getNativeConnection();

            while (!isEnd) {

                scanResult = jedisCommands.hscan(REDIS_KEY_TABLE, nextCursor, scanParams);
                nextCursor = scanResult.getStringCursor();
                List<Map.Entry<String, String>> resultDatas = scanResult.getResult();
                fetchCnt = fetchCnt+resultDatas.size();

                // 조회 푸시키 정보
                List<String> schPushKeyList = new ArrayList<>();
                //발송처리할 데이타 만들기
                Map<String,List<String>> sendTargetUsers = new HashMap<String, List<String>>();
                for (Map.Entry<String, String> mapEntry : resultDatas) {
                    sendTargetUsers.put(mapEntry.getKey(), null);
                    try {
                        List<String> pushKeyList = gson.fromJson(mapEntry.getValue(), List.class);
                        if (pushKeyList != null && pushKeyList.size() > 0) {
                            schPushKeyList.add(pushKeyList.get(0));
                        }
                    }catch (Exception ex){
                        logger.error("푸시키 Json 데이타  List로 변환중 오류 :"+mapEntry.getValue());
                    }
                }
                try {

                    // 대체발송이 없을경우. 사용자이름과 핸드폰번호를 REDIS_USERINOFO에서 가져온다.
                    String keyPushTable = REDIS_APPID_KEY+Constants.REDIS_PUSHUSERINFO;
                    List<Object> pushUserInfos = redisTemplate.opsForHash().multiGet(keyPushTable, schPushKeyList);
                    for (int k = 0; k < pushUserInfos.size(); k++) {
                        try {
                            Object pushUserInfoJsonObj = pushUserInfos.get(k);
                            if (pushUserInfoJsonObj != null) {
                                List<String> memberInfos = new ArrayList<>();
                                Map<String,String> pushUserInfoMap = gson.fromJson(pushUserInfoJsonObj.toString(), Map.class);
                                memberInfos.add(null); // 푸시가입정보에는 핸드폰번호가 없으므로 null로 넣
                                memberInfos.add(pushUserInfoMap.get("CNAME")); // 이름 정보 넣음.
                                sendTargetUsers.put(pushUserInfoMap.get("CUID"), memberInfos);
                            }
                        } catch (Exception e) {
                            logger.error("푸시가입유저에서 레디스에서 이름 가져오는중 에러 : "+e.toString());
                            e.printStackTrace();
                            continue;
                        }
                    }

                    //대체발송 처리일 경우
                    if(daecheChannelSet.size()>0) {
                        List<String> cuidList = new ArrayList<String>();
                        cuidList.addAll(sendTargetUsers.keySet());
                        List<Object> umsMemberInfos = redisTemplate.opsForHash().multiGet(Constants.REDIS_UMS_MEMBER_TABLE, cuidList);

                        for (int k = 0; k < cuidList.size(); k++) {
                            try {
                                Object umsMemberInfoObj = umsMemberInfos.get(k);
                                if (umsMemberInfoObj != null) {
                                    List<String> memberInfos = new ArrayList<>();
                                    memberInfos = gson.fromJson(umsMemberInfos.get(k).toString(), List.class);
                                    sendTargetUsers.put(cuidList.get(k), memberInfos);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error("전체발송 HSCAN 에러 , NextCursor:{}, fetchCnt:{}, cause:{}", nextCursor, fetchCnt, e.getMessage());
                    continue;
                }

                //sendTargetUsers 들어간 형식 :  {"AAA":["핸드폰번호","이름"],"BBB":null,"CCC":["핸드폰번호","이름"]...}
                //껀껀이 레디스 발송큐에 담는다
                Set<Map.Entry<String,List<String>>> sendTargetEntrySet =sendTargetUsers.entrySet();

                for(Map.Entry<String,List<String>> entry : sendTargetEntrySet){
                    sendTaskToRedis(entry.getKey(), entry.getValue(), umsSendMsgBean.getREPLACE_VARS());
                }
                sendTargetUsers.clear();

                processEndCnt++;
                loopCnt++;
                logger.debug("[Cursor loop Count]:" + loopCnt + ", NextCursor:"+nextCursor+", Total fetchCnt:"+fetchCnt);
                if (nextCursor.equals("0")) {
                    isEnd = true;
                }
            }

            int diffSentCnt = TOTAL_SEND_CNT-processEndCnt;
            if(diffSentCnt>0){
                // 이렇게 하는 이유는 초기에 가져온 발송전체카운트가 푸시삭제유저가 발생하여 발송수가 줄 경우 ProgressBar가 사라지지 않는 버그를 발생시키지 않기 위해.
                for(int i=0; i<diffSentCnt; i++){
                	TransType transType = TransType.valueOf(super.umsSendMsgBean.getTRANS_TYPE());
                    sentInfoManager.addSent_T(transType, "T_"+super.umsSendMsgBean.getUMS_SEQNO());
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            logger.error("푸시전체발송 실패 : fetchCnt:{}, cause:{}",fetchCnt, e.getMessage());
        }
        if(redisConnection!=null){
            try {
                redisConnection.close();
            }catch (Exception e){}
        }

        
		return processEndCnt;
    }

    protected void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception{

        UmsSendMsgRedisBean umsSendMsgRedisBean = super.makeUmsSendMsgRedisBean(super.umsSendMsgBean, msgVars);
        // RCS MMS이미지가 있을경우 저장된 NAS경로정보 저장
        umsSendMsgRedisBean.setTARGET_CUID(CUID);
        if(HPInfos!=null){
            umsSendMsgRedisBean.setTARGET_PHONEINFOS(HPInfos);
        }
        //레디스 발송처리 로그저장
        reqSendLogger.info(umsSendMsgRedisBean.toString());

        RedisPushManager.getInstance().putWork(umsSendMsgRedisBean);
        TransType transType = TransType.valueOf(super.umsSendMsgBean.getTRANS_TYPE());
        sentInfoManager.addSent_T(transType, "T_"+super.umsSendMsgBean.getUMS_SEQNO());
        putTotalCnt++;
        //속도제한 처리
        if(isLimitSend) {
            try {
                if(putTotalCnt%MULTIKEY_SIZE==0) {
                    logger.info("" + LIMITSECOND + "초 동안 멈춤");
                    sleep(LIMITSECOND * 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
