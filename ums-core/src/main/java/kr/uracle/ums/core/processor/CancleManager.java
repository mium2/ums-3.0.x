package kr.uracle.ums.core.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 해당 매니저는 레디스 발송취소요청 큐테이블을 주기적으로 감시하여 맵에 담고 10시간
 */
@Service
@SuppressWarnings("unchecked")
public class CancleManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public final static String UMS_CANCLE_HASHES= "UMS_CANCLE_HASHES";
    @Autowired(required = true)
    private RedisTemplate redisTemplate;
    // 발송취소 메세지원장 관리맵 키 : UMS_SEQNO, 값: 삭제처리할 만료 시간
    private Map<String,Long> chkCancelSendMap = new HashMap<String, Long>();
    private ThreadPoolTaskScheduler redisChkscheduler;
    private ThreadPoolTaskScheduler memoryChkscheduler;
    private long redisDefaultExpireTimeMillis = 60000; //60초
    private long keyDefaultExpireTimeMillis = 36000000; //10시간

    public void putCancleSend(String UMS_SEQNO){
        //chkCancelSendMap에 취소요청 UMS_SEQNO를 등록하여 발송프로세스가 해당 시퀀스를 체크하여 발송하지 못하도록 처리한다.
        long curSystemTimeMillis = System.currentTimeMillis();
        long redisExpireTimeMillis = curSystemTimeMillis+redisDefaultExpireTimeMillis; // 1분뒤 만료
        long cancleKeyExpireTimeMillis = curSystemTimeMillis+keyDefaultExpireTimeMillis; //10시간뒤 삭제처리
        //레디스에 취소요청 등록. 다른 UMS에서도 취소요청을 통지하기 위해
        redisTemplate.opsForHash().put(UMS_CANCLE_HASHES,UMS_SEQNO,""+redisExpireTimeMillis);
        synchronized (this) {
            //발송프로세스가 발송시 취소정보를 확인하는 메모리에 등록.
            chkCancelSendMap.put(UMS_SEQNO, cancleKeyExpireTimeMillis);
        }
    }

    public boolean isCancleUmsSeqno(String umsSeqNo){
        boolean isCanCle = false;
        if(chkCancelSendMap.size()>0){
            synchronized (this){
                if(chkCancelSendMap.containsKey(umsSeqNo)){
                    return true;
                }
            }
        }
        return isCanCle;
    }

    public void stopScheduler() {
        redisChkscheduler.shutdown();
        memoryChkscheduler.shutdown();
        logger.info("### CancleManager STOP!");
    }

    public void startScheduler() {
        logger.info("##########################################");
        logger.info("# CancleManager startScheduler");
        logger.info("##########################################");

        //레디스 취소요청 정보 등록 및 만료처리 스케즐러
        redisChkscheduler = new ThreadPoolTaskScheduler();
        redisChkscheduler.setPoolSize(1);
        redisChkscheduler.initialize();
        redisChkscheduler.schedule(getRunnable(), getTrigger());

        //메모리 취소정보 만료처리 스케줄러.
        memoryChkscheduler = new ThreadPoolTaskScheduler();
        memoryChkscheduler.initialize();
        memoryChkscheduler.schedule(getRunnable2(), getTrigger2());
    }

    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    // 주기적으로 레디스에서 취소요청 정보를 가져와 chkCancelSendMap에 등록하고 만료된 정보는 삭제처리한다.
                    Map<String, String> umsCancleMap = redisTemplate.opsForHash().entries(UMS_CANCLE_HASHES);
                    Set<Map.Entry<String, String>> umsCancleMapSet = umsCancleMap.entrySet();
                    for (Map.Entry<String, String> mapEntry : umsCancleMapSet) {
                        String umsSeqNo = mapEntry.getKey().toString();
                        if (!chkCancelSendMap.containsKey(umsSeqNo)) {
                            // 취소요청 확인 메모리맵에 등록되어 있지 않다면 등록.
                            long curSystemTimeMillis = System.currentTimeMillis() + keyDefaultExpireTimeMillis;
                            synchronized (this) {
                                //발송프로세스가 발송시 취소정보를 확인하는 메모리에 등록.
                                chkCancelSendMap.put(umsSeqNo, curSystemTimeMillis);
                            }
                        } else {
                            long redisExpireTimeMillis = 0;
                            if (mapEntry.getValue() != null) {
                                redisExpireTimeMillis = Long.parseLong(mapEntry.getValue());
                            }
                            if (System.currentTimeMillis() > redisExpireTimeMillis) {
                                redisTemplate.opsForHash().delete(UMS_CANCLE_HASHES, umsSeqNo);
                            }
                        }
                    }
                }catch (RedisConnectionFailureException rcf){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }catch (Exception e){
                    logger.error(e.toString());
                }
            }
        };
    }
    private Trigger getTrigger() { // 작업 주기 설정
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(2, TimeUnit.SECONDS);
        return periodicTrigger;
    }

    private Runnable getRunnable2() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    // 주기적으로 취소요청 Map 정보를 가져와  만료된 정보는 삭제처리한다.
                    if(chkCancelSendMap.size()>0) {
                        Set<String> delUmsSeqNos = new HashSet<>();
                        Set<Map.Entry<String, Long>> cancleMapSet = chkCancelSendMap.entrySet();
                        for(Map.Entry<String,Long> entry : cancleMapSet){
                            String umsSeqNo = entry.getKey();
                            Long expireTime = entry.getValue();
                            if(System.currentTimeMillis()>expireTime){
                                delUmsSeqNos.add(umsSeqNo);
                            }
                        }

                        if(delUmsSeqNos.size()>0){
                            for(String delUmsSeqNo : delUmsSeqNos){
                                synchronized (this) {
                                    chkCancelSendMap.remove(delUmsSeqNo);
                                }
                            }
                        }
                    }
                }catch (Exception e){
                    logger.error("발송요청취소맵 만료처리 로직수행 에러발생 : {}",e.toString());
                }

            }
        };
    }
    private Trigger getTrigger2() { // 작업 주기 설정
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(60, TimeUnit.SECONDS);
        return periodicTrigger;
    }
}
