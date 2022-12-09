package kr.uracle.ums.core.processor.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Y.B.H(mium2) on 2021-03-17.
 */
@SuppressWarnings("unchecked")
public class RedisPushThread extends Thread{

    private final Logger logger = LoggerFactory.getLogger(RedisPushThread.class);
    private RedisPushManager redisPushManager;
    private String tName = "";
    private boolean isRun = true;
    private String hostName = "";

    public RedisPushThread(RedisPushManager redisPushManager, String threadName) {
        this.redisPushManager = redisPushManager;
        this.tName = threadName;
    }

    public void run(){
        while(isRun){
            ///push 전송 로직
            try {
                // 전체발송 개별화 메세지를 푸시발송 레디스에 넣는다.
                Object workObj = redisPushManager.takeWork();
                if(workObj==null){
                    continue;
                }
//                String umsSendJson = redisPushManager.getGson().toJson(umsSendMsgZKBean);
                redisPushManager.getRedisTemplate_vo().opsForList().rightPush(RedisPushManager.UMSSEND_QUEUE,workObj);

            }catch(InterruptedException ex){
                logger.info("["+tName+"] InterruptedException에 의한 종료");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
            }

        }
    }

    public boolean isRun() {
        return isRun;
    }

    public void setRun(boolean isRun) {
        this.isRun = isRun;
    }
}
