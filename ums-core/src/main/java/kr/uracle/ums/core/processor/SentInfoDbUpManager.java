package kr.uracle.ums.core.processor;

import kr.uracle.ums.core.processor.bean.SentInfoBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 15..
 */
@Service
public class SentInfoDbUpManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Queue<SentInfoBean> sentInfoUpdateQueue = new LinkedList<SentInfoBean>();
    private final SentInfoDbUpWorker[] sentInfoDbUpWorkers =  new SentInfoDbUpWorker[1];
    @Autowired(required = true)
    SqlSessionTemplate sqlSessionTemplate;

    private boolean isRun = true;

    public SentInfoDbUpManager(){
        for(int i=0; i<sentInfoDbUpWorkers.length; i++){
            sentInfoDbUpWorkers[i] = new SentInfoDbUpWorker("SentInfoUpdateThread-"+i, this);
        }
        startWorkers();
    }

    public void startWorkers(){
        // 푸쉬 수신확인 처리 Work 쓰레드 구동
        for(int i=0; i<sentInfoDbUpWorkers.length; i++){
            sentInfoDbUpWorkers[i].start();
        }
    }

    public synchronized void putWork(SentInfoBean sentInfoBean){
        sentInfoUpdateQueue.offer(sentInfoBean);
        notifyAll();
    }

    public synchronized SentInfoBean takeWork(){
        // 보내는 쓰레드가 일을 하고 있을때는 Send 테이블에 인설트를 하고 있는데.. 업데트를 하면 발송 속도저하가 우려됨에 따라 발송할때는 대기하도록 처리
        while(isRun && sentInfoUpdateQueue.size()==0){
            try{
                logger.trace("## [SentInfoDbUpManager WORK THREAD] NAME : ["+Thread.currentThread().getName()+"]  WAITING!");
                wait(10000);
            }catch(InterruptedException e){
                logger.info("!!!sentInfoDbUpWorkers InterruptedException에 의한 종료");
            }
        }
        return sentInfoUpdateQueue.poll();
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void destory(){
        this.isRun = false;
        if(sentInfoDbUpWorkers!=null && sentInfoDbUpWorkers.length>0) {
            for (int i = 0; i < sentInfoDbUpWorkers.length; i++) {
                try {
                    if (sentInfoDbUpWorkers[i] != null) {
                        sentInfoDbUpWorkers[i].setRun(false);
                        sentInfoDbUpWorkers[i].interrupt();
                    }
                }catch (Exception e){}
            }
        }
    }
}
