package kr.uracle.ums.core.batch;

import kr.uracle.ums.core.ehcache.PreventIdCacheMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 하루에 한번식 수신거부 DB사용자  캐시에 동기화
 */
@Service
public class PreventCacheBatch {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired(required = true)
    private PreventIdCacheMgr preventIdCacheMgr;

    private ThreadPoolTaskScheduler scheduler;

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### PreventCacheBatch STOP!");
    }
    public void startScheduler() {
        logger.info("##########################################");
        logger.info("# PreventCacheBatch startScheduler");
        logger.info("##########################################");
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("PreventCacheBatch DB 동기화 구동");
                    preventIdCacheMgr.initializeDbSync();
                }catch (Exception e){
                    logger.error(e.toString());
                }
            }
        };
    }
    private Trigger getTrigger() { // 작업 주기 설정
        Trigger periodicTrigger = new CronTrigger("0 0 5 * * *");
//        PeriodicTrigger periodicTrigger = new PeriodicTrigger(60, TimeUnit.MINUTES);
//        PeriodicTrigger periodicTrigger = new PeriodicTrigger(10, TimeUnit.SECONDS);
//        periodicTrigger.setInitialDelay(60);
        return periodicTrigger;
    }
}
