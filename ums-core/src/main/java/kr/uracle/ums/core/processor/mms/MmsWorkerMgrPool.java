package kr.uracle.ums.core.processor.mms;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MmsWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private MmsWorkerMgr realMmsWorkerMgr;
    private MmsWorkerMgr batchMmsWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_MMS_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_MMS_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realMmsWorkerMgr = ctx.getBean(MmsWorkerMgr.class);
        this.batchMmsWorkerMgr = ctx.getBean(MmsWorkerMgr.class);

        this.realMmsWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchMmsWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realMmsWorkerMgr!=null){
            this.realMmsWorkerMgr.fileQueueCommit();
            this.realMmsWorkerMgr.destory();
        }
        if(this.batchMmsWorkerMgr!=null){
            this.batchMmsWorkerMgr.fileQueueCommit();
            this.batchMmsWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchMmsWorkerMgr.putWork(_work);
        }else{
            this.realMmsWorkerMgr.putWork(_work);
        }
    }
}
