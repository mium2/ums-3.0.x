package kr.uracle.ums.core.processor.push;

import kr.uracle.ums.codec.redis.enums.TransType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PushWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private PushWorkerMgr realPushWorkerMgr;
    private PushWorkerMgr batchPushWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_PUSH_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_PUSH_BATCH.UQ";

    public void init(){
    }

    public void startPushWorker(){
        this.realPushWorkerMgr = ctx.getBean(PushWorkerMgr.class);
        this.batchPushWorkerMgr = ctx.getBean(PushWorkerMgr.class);

        this.realPushWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchPushWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopPushWorker(){
        if(this.realPushWorkerMgr!=null){
            this.realPushWorkerMgr.fileQueueCommit();
            this.realPushWorkerMgr.destory();
        }
        if(this.batchPushWorkerMgr!=null){
            this.batchPushWorkerMgr.fileQueueCommit();
            this.batchPushWorkerMgr.destory();
        }
    }

    public void putWork(PushBasicProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchPushWorkerMgr.putWork(_work);
        }else{
            this.realPushWorkerMgr.putWork(_work);
        }
    }
}
