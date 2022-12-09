package kr.uracle.ums.core.processor.sms;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SmsWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private SmsWorkerMgr realSmsWorkerMgr;
    private SmsWorkerMgr batchSmsWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_SMS_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_SMS_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realSmsWorkerMgr = ctx.getBean(SmsWorkerMgr.class);
        this.batchSmsWorkerMgr = ctx.getBean(SmsWorkerMgr.class);

        this.realSmsWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchSmsWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realSmsWorkerMgr!=null){
            this.realSmsWorkerMgr.fileQueueCommit();
            this.realSmsWorkerMgr.destory();
        }
        if(this.batchSmsWorkerMgr!=null){
            this.batchSmsWorkerMgr.fileQueueCommit();
            this.batchSmsWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchSmsWorkerMgr.putWork(_work);
        }else{
            this.realSmsWorkerMgr.putWork(_work);
        }
    }
}
