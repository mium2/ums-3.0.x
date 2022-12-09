package kr.uracle.ums.core.processor.rcs;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RcsWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private RcsWorkerMgr realRCSWorkerMgr;
    private RcsWorkerMgr batchRCSWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_RCS_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_RCS_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realRCSWorkerMgr = ctx.getBean(RcsWorkerMgr.class);
        this.batchRCSWorkerMgr = ctx.getBean(RcsWorkerMgr.class);

        this.realRCSWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchRCSWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realRCSWorkerMgr!=null){
            this.realRCSWorkerMgr.fileQueueCommit();
            this.realRCSWorkerMgr.destory();
        }
        if(this.batchRCSWorkerMgr!=null){
            this.batchRCSWorkerMgr.fileQueueCommit();
            this.batchRCSWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchRCSWorkerMgr.putWork(_work);
        }else{
            this.realRCSWorkerMgr.putWork(_work);
        }
    }
}
