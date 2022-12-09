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

import java.util.List;
import java.util.Map;

/**
 * 해당 클래스는 UMS회원 전체 정보를 가져와 매크로에 정의된 첫번째 발송채널 정보를 이용하여 레디스에 개별비회원 발송등록 처리만 한다.
 */
@SuppressWarnings("unchecked")
public class AllUmsUserToRedisSender extends BaseThreadRedis {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Logger reqSendLogger = LoggerFactory.getLogger("sentRedisLogger");
    
    private int MULTIKEY_SIZE = 1000;
    private int putTotalCnt = 0;
    private SendType sendType;

    public AllUmsUserToRedisSender(UmsSendMsgBean umsSendMsgBean, String limitSecond, String limitCnt){
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
        if(LIMITSECOND>0)isLimitSend = true;
        
    }



    @Override
    public Map<String,Object> processInfo() throws  Exception{
        super.init();

        sendType = SendType.valueOf(super.umsSendMsgBean.getSTART_SEND_KIND());

        TOTAL_SEND_CNT = Long.valueOf(redisTemplate.opsForHash().size(Constants.REDIS_UMS_MEMBER_TABLE)).intValue();
        if(TOTAL_SEND_CNT ==0){
            throw new NotExistUserException("UMS에 등록된 사용자가 없습니다.");
        }

        // 원장 총 요청 건수 지정
        super.umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        if(StringUtils.isNotBlank(super.umsSendMsgBean.getRESERVEDATE())){
            umsDao.inUmsSendReserveMsg(super.umsSendMsgBean);
        }else {
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(super.umsSendMsgBean);

        	TransType transType = TransType.valueOf(super.umsSendMsgBean.getTRANS_TYPE());
//            sentInfoManager.setInitTotalSendCnt(transType, umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
            //프로그래스바를 위해서 발송카운트 셋팅
            sentInfoManager.setInitThreadSendCnt(transType, "T_"+super.umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
            returnResultMap.put("PROGRESS_SEQNO","T_"+super.umsSendMsgBean.getUMS_SEQNO());
        }

        //[공통필수] : Response 발송수 공통로직으로 채널별 발송카운트 UmsSendMsgBean에 셋팅. 원장테이블 발송카운트는 Process에서 처리한다.
        super.setChannelSendCnt(sendType, TOTAL_SEND_CNT);

        //[공통필수] : 채널별 발송 카운트 셋팅 및 발송원장 시퀀스 셋팅
        umsSendCommonService.setRspChannelSendCnt(super.umsSendMsgBean,returnResultMap);

        return returnResultMap;
    }

    @Override
    protected int send() {
        
        String REDIS_KEY_TABLE = Constants.REDIS_UMS_MEMBER_TABLE;
        RedisConnection redisConnection = null;
        JedisCommands jedisCommands = null;
        try {            
            if ("3".equals(REDISTYPE)) {	//레디스 클러스트로 설치
                redisConnection = redisTemplate.getConnectionFactory().getClusterConnection();
            } else {
                redisConnection = redisTemplate.getConnectionFactory().getConnection();
            }
            jedisCommands = (JedisCommands) redisConnection.getNativeConnection();

            int loopCnt = 0, fetchCnt = 0, processEndCnt = 0;
            ScanParams scanParams = new ScanParams();
            scanParams.count(MULTIKEY_SIZE);
            ScanResult<Map.Entry<String, String>> scanResult;
            String nextCursor = "0";
            do {
            	scanResult = jedisCommands.hscan(REDIS_KEY_TABLE, nextCursor, scanParams);
            	nextCursor = scanResult.getStringCursor();
            	List<Map.Entry<String, String>> resultDatas = scanResult.getResult();
            	int size = resultDatas.size();
            	fetchCnt = fetchCnt+ size;
            	 
            	int index = 0;
            	for (Map.Entry<String, String> mapEntry : resultDatas) {
            		index += 1;
            		String cuid = mapEntry.getKey();
            		String HPJsonInfos = mapEntry.getValue();
            		List<String> HpInfos = null;
            		try {
            			if(HPJsonInfos!=null) HpInfos = gson.fromJson(HPJsonInfos,List.class);
            		} catch (Exception e) { logger.error("{}",e.toString()); }
            		
            		super.umsSendMsgBean.setRCS_IMG_INSERT(false);
            		if(nextCursor.equals("0") && (size == index)) super.umsSendMsgBean.setRCS_IMG_INSERT(true);
            		sendTaskToRedis(cuid, HpInfos, super.umsSendMsgBean.getREPLACE_VARS());
            		
            		processEndCnt++;
            	}

            	loopCnt++;
            	logger.debug("[Cursor loop Count]:" + loopCnt + ", NextCursor:"+nextCursor+", Total fetchCnt:"+fetchCnt);
			} while (!nextCursor.equals("0"));
            
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
            logger.error("UMS 전체회원 발송 실패 : cause : {}",e.toString());
        }
        if(redisConnection!=null){
            redisConnection.close();
        }
		return TOTAL_SEND_CNT;
    }
        
    protected void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception{
        UmsSendMsgRedisBean umsSendMsgRedisBean = super.makeUmsSendMsgRedisBean(super.umsSendMsgBean, msgVars);
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
