package kr.uracle.ums.core.processor.kakao;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.AmKkoFrtProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoFrtProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoFrtProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.dao.kko.IKkoFrtDao;
import kr.uracle.ums.core.dao.kko.LgcnsKkoFrtDao;
import kr.uracle.ums.core.dao.kko.LotteKkoFrtDao;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.service.UmsChannelProviderFactory;
import kr.uracle.ums.core.service.UmsSendCheckService;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.queue.FileQueueData;
import kr.uracle.ums.core.service.queue.FileQueueHelper;
import kr.uracle.ums.core.service.queue.FileQueueHelperManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 * 친구톡 발송 요청을 하기 위해 KT 발송처리 테이블에 넣어야 하는데.. 속도를 위해 큐에 담아 여러개의 쓰래드로 처리 하도록 한다.
 */
@Service @Scope("prototype")
@SuppressWarnings("unchecked")
public class FriendtalkWorkerMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ConcurrentLinkedQueue<BaseProcessBean> friendtalkWorkQueue = new ConcurrentLinkedQueue<BaseProcessBean>();
    private final FriendtalkWorkerThread[] friendtalkWorkerThreads =  new FriendtalkWorkerThread[5];
    private int processCnt = 0;
    private long startMilSec = 0;
    @Autowired(required = true)
    SentInfoManager sentInfoManager;
	
    @Autowired(required = true)
    private LgcnsKkoFrtDao lgcnsKkoFrtDao;

    @Autowired(required = true)
    private LotteKkoFrtDao lotteKkoFrtDao;

    @Autowired(required = true)
    private UmsDao umsDao;

    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;

    @Autowired(required = true)
    private UmsSendCheckService umsSendCheckService;

    // 설정피로도 체크유무
    @Value("${UMS.USE_FATIGUE_YN:N}")
    private String USE_FATIGUE;

    private final Gson gson = new Gson();
    
    private String FILEDB_PATH;
    
    private boolean useFileDB = false;

    private FileQueueHelper Q_HELPER;

    public FriendtalkWorkerMgr(){}
    
    public void initailize(String fileDbUseYn, String fileDbPath, String fileDbName) {
        if(fileDbPath.endsWith(File.separator) == false)  fileDbPath = fileDbPath+File.separator;

        useFileDB = fileDbUseYn.trim().equalsIgnoreCase("Y");
        FILEDB_PATH = fileDbPath+fileDbName;
        Q_HELPER =  new FileQueueHelper(FILEDB_PATH);

    	//파일 DB 사용 여부
    	if(StringUtils.isBlank(FILEDB_PATH)) useFileDB = false;
    	try {
	    	if(useFileDB) {
		    	if(useFileDB) {  		
		    		useFileDB = Q_HELPER.open();
		    		if(useFileDB) FileQueueHelperManager.getInstance().enrollHelper(FILEDB_PATH, Q_HELPER);
		    		
		    	}	
	    	}
    	} catch (Exception e) {
    		logger.error("FRIENDTOK 파일 DB 파일 생성 중 에러 발생:"+e);
    		useFileDB = false;
    	}
    	
        // 리퀘스트 요청 처리 Work 쓰레드 구동
        for(int i=0; i< friendtalkWorkerThreads.length; i++){
            friendtalkWorkerThreads[i] = new FriendtalkWorkerThread("friendtalkWorkerThreads-"+i, this);
            friendtalkWorkerThreads[i].start();
        }
        logger.info("####################################");
        logger.info("# "+(useFileDB?fileDbName+" 파일큐 모드로":"")+" 친구톡 발송 Thread 구동!");
        logger.info("####################################");
    }

    public IKkoFrtDao getProviderDao(String providerName){
    	IKkoFrtDao dao = null;
    	if(StringUtils.isBlank(providerName))providerName = umsChannelProvierFactory.getKKO_PROVIDER();
    	switch(providerName.toUpperCase()) {
            case "LOTTE" :
                dao = lotteKkoFrtDao;
                break;
	    	default :
	    		dao = lgcnsKkoFrtDao;
	    		break;
    	}
        return dao;
    }

    protected synchronized void putWork(BaseProcessBean _work){
    	if(useFileDB) {
    		Q_HELPER.putData(_work);
    	}else {
    		friendtalkWorkQueue.offer(_work);    		
    	}
        TpsManager.getInstance().addInputCnt(TpsManager.TPSSERVERKIND.KKOFRT);
        notify();
    }

    public synchronized BaseProcessBean takeWork() throws InterruptedException {
    	BaseProcessBean baseBean= null;
        if(useFileDB) {
    		if(Q_HELPER.getDataCount() >0) {        			
    			FileQueueData qData = Q_HELPER.getData();
                String provider = qData.getHint();
                switch (provider){
                    case "AM":
                        baseBean = gson.fromJson(qData.getData(), AmKkoFrtProcessBean.class);
                        break;
                    case "LOTTE":
                        baseBean = gson.fromJson(qData.getData(), LotteKkoFrtProcessBean.class);
                        break;
                    case "LGCNS":
                        baseBean = gson.fromJson(qData.getData(), LgcnsKkoFrtProcessBean.class);
                        break;
                    default:
                        logger.warn("친구톡 파일큐에서 알수 없는 ProcessBean 탐색 됨");
                        break;
                }
    		}else {
    			try {
    				wait(60000);						
				} catch (InterruptedException e) {logger.warn("KKOFRT Work Interrupted call~!");}
    		}
        }else {
            while ((baseBean = friendtalkWorkQueue.poll())==null){
                try {
                    wait(60000);
                }catch (InterruptedException e){e.printStackTrace();}
            }	
        }
        if(baseBean !=null) baseBean.setTRANSACTION_KEY(TransactionUtil.getTransactionKey(baseBean.getTRANS_TYPE().toString(), baseBean.getSTART_SEND_TYPE()));
        infoPrint();
        return baseBean;
    }

    public synchronized void workNotify(){
        notifyAll();
    }

    public void destory(){
        try {
            if (friendtalkWorkerThreads != null) {
                for (int i = 0; i < friendtalkWorkerThreads.length; i++) {
                    if(friendtalkWorkerThreads[i]!=null){
                        friendtalkWorkerThreads[i].setRun(false);
                        friendtalkWorkerThreads[i].interrupt();
                    }
                }
            }
            if(Q_HELPER != null)Q_HELPER.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public UmsSendCommonService getUmsSendCommonService() {
        return umsSendCommonService;
    }

    public String getUSE_FATIGUE() {
        return USE_FATIGUE;
    }

    /**
     * 정보를 주기적으로 출력하는 것이기 때문 syncronize를 잡아 줄 필요 없음. 중요하지 않음.
     */
    public void infoPrint(){
        processCnt++;
        if(processCnt==1){
            startMilSec = System.currentTimeMillis();
        }
        if(processCnt%1000==0){
            logger.info("## process cnt : {}",processCnt);
        }
        if(processCnt==10000){
            processCnt = 0; //0으로 초기화
            long endMilSec = System.currentTimeMillis();
            logger.info("## FriendTalk send processing... Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
        }
    }

    public SentInfoManager getSentInfoManager() {
        return sentInfoManager;
    }

    public UmsDao getUmsDao() {
        return umsDao;
    }

    public UmsChannelProviderFactory getUmsChannelProvierFactory() {
        return umsChannelProvierFactory;
    }

    public UmsSendCheckService getUmsSendCheckService() {
        return umsSendCheckService;
    }

    public Gson getGson() {
        return gson;
    }

    public synchronized void fileQueueCommit(){
        try {
            if(Q_HELPER!=null) {
                Q_HELPER.commit("친구톡");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
