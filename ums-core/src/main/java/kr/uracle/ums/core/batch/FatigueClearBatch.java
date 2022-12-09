package kr.uracle.ums.core.batch;

import kr.uracle.ums.core.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FatigueClearBatch {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired(required = true)
    private RedisTemplate redisTemplate_vo;

    @Value("${redis.type:1}")
    private String REDISTYPE;

    private ThreadPoolTaskScheduler scheduler;

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### FatigueClearBatch STOP!");
    }
    public void startScheduler() {
        logger.info("##########################################");
        logger.info("# FatigueClearBatch startScheduler");
        logger.info("##########################################");
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (!MasterRollChecker.getInstance().isMaster()) return;
                    logger.info("레디스 피로도 정보 정리시작");

                    String REDIS_KEY_TABLE = Constants.REDIS_FATIGUE;
                    int loopCnt = 0;
                    int fetchCnt = 0;
                    int MULTIKEY_SIZE = 300;

                    RedisConnection redisConnection = null;
                    JedisCommands jedisCommands = null;
                    try {
                        boolean isEnd = false;
                        ScanParams scanParams = new ScanParams();
                        scanParams.count(MULTIKEY_SIZE);
                        ScanResult<Map.Entry<String, String>> scanResult;
                        String nextCursor = "0";

                        if ("3".equals(REDISTYPE)) {	//레디스 클러스트로 설치
                            redisConnection = redisTemplate_vo.getConnectionFactory().getClusterConnection();
                        } else {
                            redisConnection = redisTemplate_vo.getConnectionFactory().getConnection();
                        }
                        jedisCommands = (JedisCommands) redisConnection.getNativeConnection();

                        while (!isEnd) {

                            scanResult = jedisCommands.hscan(REDIS_KEY_TABLE, nextCursor, scanParams);
                            nextCursor = scanResult.getStringCursor();
                            List<Map.Entry<String, String>> resultDatas = scanResult.getResult();
                            fetchCnt = fetchCnt+resultDatas.size();

                            for (Map.Entry<String, String> mapEntry : resultDatas) {
                                String cuid = mapEntry.getKey();
                                String fatigueInfoStr = mapEntry.getValue();
                                String[] fatigueInfoStrArr = fatigueInfoStr.split(",");
                                String[] dayInfoArr = fatigueInfoStrArr[0].split(":");
                                String[] weekInfoArr = fatigueInfoStrArr[1].split(":");
                                String[] monthInfoArr = fatigueInfoStrArr[2].split(":");

                                long dExpire = Long.parseLong(dayInfoArr[0]);
                                long wExpire = Long.parseLong(weekInfoArr[0]);
                                long mExpire = Long.parseLong(monthInfoArr[0]);

                                long nowCurTimeMillis = System.currentTimeMillis();
                                if(dExpire < nowCurTimeMillis && wExpire < nowCurTimeMillis && mExpire<nowCurTimeMillis){
                                    // 레디스 삭제처리
                                    redisTemplate_vo.opsForHash().delete(REDIS_KEY_TABLE, cuid);
                                }
                            }

                            loopCnt++;
                            logger.debug("[Cursor loop Count]:" + loopCnt + ", NextCursor:"+nextCursor+", Total fetchCnt:"+fetchCnt);
                            if (nextCursor.equals("0")) {
                                isEnd = true;
                            }
                        }

                        logger.info("레디스 피로도 정리 성공");


                    }catch (Exception e){
                        e.printStackTrace();
                        logger.error("레디스 피로도 정보 정리 실패 : fetchCnt:{}, cause:{}",fetchCnt, e.getMessage());
                    }
                    if(redisConnection!=null){
                        try {
                            redisConnection.close();
                        }catch (Exception e){}
                    }

                }catch (Exception e){
                    logger.error(e.toString());
                }
            }
        };
    }
    private Trigger getTrigger() { // 작업 주기 설정
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(10, TimeUnit.MINUTES);
//        PeriodicTrigger periodicTrigger = new PeriodicTrigger(10, TimeUnit.SECONDS);
        periodicTrigger.setInitialDelay(10);
        return periodicTrigger;

//        return new CronTrigger("0 0 4 * * *");
    }
}
