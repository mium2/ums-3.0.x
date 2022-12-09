package kr.uracle.ums.core.processor.kakao;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AllimtalkWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private AllimtalkWorkerMgr realAllimtalkWorkerMgr;
    private AllimtalkWorkerMgr batchAllimtalkWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_KKOALT_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_KKOALT_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realAllimtalkWorkerMgr = ctx.getBean(AllimtalkWorkerMgr.class);
        this.batchAllimtalkWorkerMgr = ctx.getBean(AllimtalkWorkerMgr.class);

        this.realAllimtalkWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchAllimtalkWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realAllimtalkWorkerMgr!=null){
            this.realAllimtalkWorkerMgr.fileQueueCommit();
            this.realAllimtalkWorkerMgr.destory();
        }
        if(this.batchAllimtalkWorkerMgr!=null){
            this.batchAllimtalkWorkerMgr.fileQueueCommit();
            this.batchAllimtalkWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchAllimtalkWorkerMgr.putWork(_work);
        }else{
            this.realAllimtalkWorkerMgr.putWork(_work);
        }
    }
}
