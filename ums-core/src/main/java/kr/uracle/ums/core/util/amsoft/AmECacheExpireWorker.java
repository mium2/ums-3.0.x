package kr.uracle.ums.core.util.amsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

public class AmECacheExpireWorker {
    private final static Logger logger = LoggerFactory.getLogger(AmECacheExpireWorker.class);
    private ThreadPoolTaskScheduler scheduler;

    private static AmECacheExpireWorker INSTANCE = new AmECacheExpireWorker();

    private AmECacheExpireWorker(){}

    public static AmECacheExpireWorker getINSTANCE() {
        return INSTANCE;
    }

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### AmECacheExpireWorker STOP!");
    }
    public void startScheduler() {
        logger.info("##########################################");
        logger.info("# AmECacheExpireWorker startScheduler");
        logger.info("##########################################");
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                AmSendMsgECache.getInstance().evictExpiredElements();
            }
        };
    }
    private Trigger getTrigger() { // 작업 주기 설정
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(10, TimeUnit.SECONDS);
        return periodicTrigger;
    }
}
