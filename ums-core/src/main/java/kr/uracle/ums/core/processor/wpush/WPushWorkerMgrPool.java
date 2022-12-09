package kr.uracle.ums.core.processor.wpush;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.processor.push.PushBasicProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class WPushWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private WPushWorkerMgr realWPushWorkerMgr;
    private WPushWorkerMgr batchWPushWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_WPUSH_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_WPUSH_BATCH.UQ";

    public void init(){
    }

    public void startPushWorker(){
        this.realWPushWorkerMgr = ctx.getBean(WPushWorkerMgr.class);
        this.batchWPushWorkerMgr = ctx.getBean(WPushWorkerMgr.class);

        this.realWPushWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchWPushWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopPushWorker(){
        if(this.realWPushWorkerMgr!=null){
            this.realWPushWorkerMgr.fileQueueCommit();
            this.realWPushWorkerMgr.destory();
        }
        if(this.batchWPushWorkerMgr!=null){
            this.batchWPushWorkerMgr.fileQueueCommit();
            this.batchWPushWorkerMgr.destory();
        }
    }

    public void putWork(PushBasicProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchWPushWorkerMgr.putWork(_work);
        }else{
            this.realWPushWorkerMgr.putWork(_work);
        }
    }
}
