package kr.uracle.ums.core.processor.kakao;

import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class FriendtalkWorkerMgrPool {
    @Autowired
    private ApplicationContext ctx;
    @Value("${FILEDB.PATH}")
    private String path;
    @Value("${FILEDB.USEYN:N}")
    private String useYn;
    private FriendtalkWorkerMgr realFriendtalkWorkerMgr;
    private FriendtalkWorkerMgr batchFriendtalkWorkerMgr;

    private final String REAL_FILEDB_NAME="UMS_KKOFRT_REAL.UQ";
    private final String BATCH_FILEDB_NAME="UMS_KKOFRT_BATCH.UQ";

    public void init(){
    }

    public void startWorker(){
        this.realFriendtalkWorkerMgr = ctx.getBean(FriendtalkWorkerMgr.class);
        this.batchFriendtalkWorkerMgr = ctx.getBean(FriendtalkWorkerMgr.class);

        this.realFriendtalkWorkerMgr.initailize(useYn, path, REAL_FILEDB_NAME);
        this.batchFriendtalkWorkerMgr.initailize(useYn, path, BATCH_FILEDB_NAME);
    }

    public void stopWorker(){
        if(this.realFriendtalkWorkerMgr!=null){
            this.realFriendtalkWorkerMgr.fileQueueCommit();
            this.realFriendtalkWorkerMgr.destory();
        }
        if(this.batchFriendtalkWorkerMgr!=null){
            this.batchFriendtalkWorkerMgr.fileQueueCommit();
            this.batchFriendtalkWorkerMgr.destory();
        }
    }

    public void putWork(BaseProcessBean _work){
        if(_work.getTRANS_TYPE()== TransType.BATCH){
            this.batchFriendtalkWorkerMgr.putWork(_work);
        }else{
            this.realFriendtalkWorkerMgr.putWork(_work);
        }
    }
}
