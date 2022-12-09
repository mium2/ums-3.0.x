package kr.uracle.ums.core.processor.naver;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class NaverWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private NaverWorkerMgr realNaverWorkerMgr;
    private NaverWorkerMgr batchNaverWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_NAVER_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_NAVER_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realNaverWorkerMgr = ctx.getBean(NaverWorkerMgr.class);
        this.batchNaverWorkerMgr = ctx.getBean(NaverWorkerMgr.class);

        this.realNaverWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchNaverWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realNaverWorkerMgr!=null){
            this.realNaverWorkerMgr.fileQueueCommit();
            this.realNaverWorkerMgr.destory();
        }
        if(this.batchNaverWorkerMgr!=null){
            this.batchNaverWorkerMgr.fileQueueCommit();
            this.batchNaverWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchNaverWorkerMgr.putWork(_work);
        }else{
            this.realNaverWorkerMgr.putWork(_work);
        }
    }
}
