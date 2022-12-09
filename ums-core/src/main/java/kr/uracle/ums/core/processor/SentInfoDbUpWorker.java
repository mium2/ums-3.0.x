package kr.uracle.ums.core.processor;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 15..
 */
public class SentInfoDbUpWorker extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final SentInfoDbUpManager sentInfoDbUpManager;
    private boolean isRun = true;

    public SentInfoDbUpWorker(String name, SentInfoDbUpManager _sentInfoDbUpManager) {
        super(name);
        this.sentInfoDbUpManager = _sentInfoDbUpManager;
    }

    public void run(){
        while(isRun){
            try {
                SentInfoBean sentInfoBean = sentInfoDbUpManager.takeWork();
                if (sentInfoBean != null) {
                    if (TransType.REAL == sentInfoBean.getTRANS_TYPE()) {
                        sentInfoDbUpManager.getSqlSessionTemplate().update("mybatis.ums.send.upUmsSendCountReal", sentInfoBean);
                    } else {
                        sentInfoDbUpManager.getSqlSessionTemplate().update("mybatis.ums.send.upUmsSendCountBatch", sentInfoBean);
                    }
                }
            }catch (Exception e){
                logger.error(e.toString());
            }
        }
        logger.info("### SentInfoDbUpWorker 종료됨.");
    }

    public void setRun(boolean _isRun){
        this.isRun = false;
    }
}
