package kr.uracle.ums.core.processor.redis;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.common.UmsInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Y.B.H(mium2) on 2021-03-17.
 */
public class RedisPushManager {
    private final Logger logger = LoggerFactory.getLogger(RedisPushManager.class);
    private static RedisPushManager instance = null;
    private RedisPushThread[] redisPushThreads;

    private Queue<Object> pushQueue = new LinkedList<Object>();
    private int MAXQUEUE_CNT = 100000; //10만껀

    private boolean isShutDown = false;

    private RedisTemplate redisTemplate;
    private RedisTemplate redisTemplate_vo;

    public final static String UMSSEND_QUEUE = "UMS_SEND_QUEUE";

    private Gson gson = new Gson();

    private int REDIS_THREAD_CNT = 20;
    private RedisPushManager(){};

    public static RedisPushManager getInstance(){
        if(instance==null){
            instance = new RedisPushManager();
        }
        return instance;
    }

    public void startRedisPushThread(){
        this.redisTemplate = (RedisTemplate)UmsInitListener.wContext.getBean("redisTemplate");
        this.redisTemplate_vo = (RedisTemplate)UmsInitListener.wContext.getBean("redisTemplate_vo");

        redisPushThreads = new RedisPushThread[this.REDIS_THREAD_CNT];
        for(int i=0; i<(redisPushThreads.length); i++){
            redisPushThreads[i] = new RedisPushThread(this, "RedisPushThread-"+i);
            redisPushThreads[i].start();
        }
    }

    public void stopRedisPushThread(){
        if(redisPushThreads!=null && redisPushThreads.length>0){
            for(int i=0; i<(redisPushThreads.length); i++){
                try {
                    redisPushThreads[i].setRun(false);
                    redisPushThreads[i].interrupt();
                }catch (Exception e){
                    logger.error("RedisPushThreads 종료 에러 :"+e.toString());
                }
            }
        }
    }

    public synchronized void putWork(UmsSendMsgRedisBean _work){
        if(pushQueue.size()>MAXQUEUE_CNT){
            try {
                wait();
            } catch (InterruptedException e) {
                logger.debug("깨어남");
            }
        }
        pushQueue.offer(_work);
        notifyAll();
    }

    public synchronized Object takeWork() throws InterruptedException {
        // 보내는 쓰레드가 일을 하고 있을때는 Send 테이블에 인설트를 하고 있는데.. 업데트를 하면 발송 속도저하가 우려됨에 따라 발송할때는 대기하도록 처리
        while(pushQueue.size()==0){
            wait();
        }
        Object xWork = pushQueue.poll();
        notifyAll();
        return xWork;
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public RedisTemplate getRedisTemplate_vo() {
        return redisTemplate_vo;
    }

    public Gson getGson() {
        return gson;
    }
}

