package kr.uracle.ums.core.processor.kakao;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.AmKkoAltProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoAltProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoAltProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.dao.kko.IKkoAltDao;
import kr.uracle.ums.core.dao.kko.LgcnsKkoAltDao;
import kr.uracle.ums.core.dao.kko.LotteKkoAltDao;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.service.UmsChannelProviderFactory;
import kr.uracle.ums.core.service.UmsSendCheckService;
import kr.uracle.ums.core.service.queue.FileQueueData;
import kr.uracle.ums.core.service.queue.FileQueueHelper;
import kr.uracle.ums.core.service.queue.FileQueueHelperManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;


@Service @Scope("prototype")
@SuppressWarnings("unchecked")
public class AllimtalkWorkerMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ConcurrentLinkedQueue<BaseProcessBean> allimtalkWorkQueue = new ConcurrentLinkedQueue<BaseProcessBean>();
    private final AllimtalkWorkerThread[] allimtalkWorkerThreads =  new AllimtalkWorkerThread[5];
    private int processCnt = 0;
    private long startMilSec = 0;
    @Autowired(required = true)
    private SentInfoManager sentInfoManager;

    @Autowired(required = true)
    private LgcnsKkoAltDao lgcnsKkoAltDao;

    @Autowired(required = true)
    private LotteKkoAltDao lotteKkoAltDao;

    @Autowired(required = true)
    private UmsDao umsDao;

    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;

    @Autowired(required = true)
    private UmsSendCheckService umsSendCheckService;

    private final Gson gson = new Gson();
        
    private String FILEDB_PATH;
    
    private boolean useFileDB = false;
    
    private FileQueueHelper Q_HELPER;

    public AllimtalkWorkerMgr(){

    }
    
    public void initailize(String fileDbUseYn, String fileDbPath, String fileDbName) {
        if(fileDbPath.endsWith(File.separator) == false)  fileDbPath = fileDbPath+File.separator;

        useFileDB = fileDbUseYn.trim().equalsIgnoreCase("Y");
        FILEDB_PATH = fileDbPath+fileDbName;
        Q_HELPER =  new FileQueueHelper(FILEDB_PATH);

    	//파일 DB 사용 여부
    	if(StringUtils.isBlank(FILEDB_PATH)) useFileDB = false;
    	try {
	    	if(useFileDB) {  		
	    		useFileDB = Q_HELPER.open();
	    		if(useFileDB) FileQueueHelperManager.getInstance().enrollHelper(FILEDB_PATH, Q_HELPER);
	    	}
    	} catch (Exception e) {
    		logger.error("파일 DB 파일 생성 중 에러 발생:"+e);
    		useFileDB = false;
    	}
    	
         // 리퀘스트 요청 처리 Work 쓰레드 구동
        for(int i=0; i< allimtalkWorkerThreads.length; i++){
            allimtalkWorkerThreads[i] = new AllimtalkWorkerThread("AllimtalkWorkerThread-"+i, this);
            allimtalkWorkerThreads[i].start();
        }

        logger.info("####################################");
        logger.info("# "+(useFileDB ? fileDbName+" 파일큐 모드로":"")+" 알림톡 발송 Thread 구동!");
        logger.info("####################################");
    }
    
    public IKkoAltDao getProviderDao(String providerName){
    	IKkoAltDao dao = null;
    	if(StringUtils.isBlank(providerName))providerName = umsChannelProvierFactory.getKKO_PROVIDER();
    	switch(providerName.toUpperCase()) {
            case "LOTTE" :
                dao = lotteKkoAltDao;
                break;
	    	default :
	    		dao = lgcnsKkoAltDao;
	    		break;
    	}
        return dao;
    }

    protected synchronized void putWork(BaseProcessBean _work){
    	if(useFileDB) {
    		Q_HELPER.putData(_work);
    	}else {
    		allimtalkWorkQueue.offer(_work);    		
    	}
        TpsManager.getInstance().addInputCnt(TpsManager.TPSSERVERKIND.KKOALT);
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
                        baseBean = gson.fromJson(qData.getData(), AmKkoAltProcessBean.class);
                        break;
                    case "LOTTE":
                        baseBean = gson.fromJson(qData.getData(), LotteKkoAltProcessBean.class);
                        break;
                    case "LGCNS":
                        baseBean = gson.fromJson(qData.getData(), LgcnsKkoAltProcessBean.class);
                        break;
                    default:
                        logger.warn("알림톡 파일큐에서 알수 없는 ProcessBean 탐색 됨");
                        break;
                }
    			
    		}else {
    			try {
    				wait(60000);						
				} catch (InterruptedException e) {logger.warn("KKOALT Work Interrupted call~!");}
    		}
        }else {
            while ((baseBean = allimtalkWorkQueue.poll())==null){
                try {
                    wait(60000);
                }catch (InterruptedException ie){ie.printStackTrace();}
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
            if (allimtalkWorkerThreads != null) {
                for (int i = 0; i < allimtalkWorkerThreads.length; i++) {
                    if(allimtalkWorkerThreads[i]!=null){
                        allimtalkWorkerThreads[i].setRun(false);
                        allimtalkWorkerThreads[i].interrupt();
                    }
                }
            }
            
            if(Q_HELPER != null)Q_HELPER.close();
        }catch(Exception e){
            e.printStackTrace();
        }
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
            logger.info("## AllimTalk send processing. Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
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
                Q_HELPER.commit("알림톡");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
