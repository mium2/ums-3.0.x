package kr.uracle.ums.core.processor.naver;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.naver.MtsNaverProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.dao.naver.INaverDao;
import kr.uracle.ums.core.dao.naver.MtsNaverDao;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.processor.CancleManager;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service @Scope("prototype")
public class NaverWorkerMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ConcurrentLinkedQueue<BaseProcessBean> naverWorkQueue = new ConcurrentLinkedQueue<BaseProcessBean>();
    private final NaverWorkerThread[] naverWorkerThreads =  new NaverWorkerThread[5];
    private int processCnt = 0;
    private long startMilSec = 0;
    @Autowired(required = true)
    private SentInfoManager sentInfoManager;
    
    @Autowired(required = true)
    private MtsNaverDao mtsNaverDao;

    @Autowired(required = true)
    private UmsDao umsDao;

    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;

    @Autowired(required = true)
    private PreventMobileCacheMgr preventMobileCacheMgr;

    @Autowired(required = true)
    private CancleManager cancleManager;

    @Autowired(required = true)
    private UmsSendCheckService umsSendCheckService;

    @Value("${NAVER.PROVIDER:MTS}")
    private String NAVER_PROVIDER;
    
    private final Gson gson = new Gson();
    
    private String FILEDB_PATH;
    
    private boolean useFileDB = false;
        
    private FileQueueHelper Q_HELPER;

    public NaverWorkerMgr(){}
    
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
    		logger.error("NAVERT 파일 DB 파일 생성 중 에러 발생:"+e);
    		useFileDB = false;
    	}
    	
        // 리퀘스트 요청 처리 Work 쓰레드 구동
        for(int i=0; i< naverWorkerThreads.length; i++){
            naverWorkerThreads[i] = new NaverWorkerThread("NaverWorkerThread-"+i, this);
            naverWorkerThreads[i].start();
        }

        logger.info("####################################");
        logger.info("# "+(useFileDB?fileDbName+" 파일큐 모드로":"")+" 네이버 발송 Thread 구동!");
        logger.info("####################################");
    }
    
    public INaverDao getProviderDao(String providerName){
    	INaverDao dao = null;
    	if(StringUtils.isBlank(providerName))providerName = umsChannelProvierFactory.getNAVER_PROVIDER();
    	switch(providerName.toUpperCase()) {
	    	default :
	    		dao = mtsNaverDao;
	    		break;
    	}
        return dao;
    }

    protected synchronized void putWork(BaseProcessBean _work){
    	if(useFileDB) {
    		Q_HELPER.putData(_work);
    	}else {
    		naverWorkQueue.offer(_work);    		
    	}
        TpsManager.getInstance().addInputCnt(TpsManager.TPSSERVERKIND.NAVER);
        notify();
    }

    public synchronized BaseProcessBean takeWork() throws InterruptedException {
    	BaseProcessBean baseBean= null;
        if(useFileDB) {
        	if(Q_HELPER.getDataCount() >0) {        			
    			FileQueueData qData = Q_HELPER.getData();
    			if(qData.getHint().equals("MTS"))baseBean = gson.fromJson(qData.getData(), MtsNaverProcessBean.class);
    		}else {
    			try {
    				wait(60000);						
				} catch (InterruptedException e) {logger.warn("NAVER Work Interrupted call~!");}
    		}
        }else {
        	while ((baseBean = naverWorkQueue.poll())==null){
        		try {
        			wait(60000);
        			continue;
        		}catch (InterruptedException ie){}
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
            if (naverWorkerThreads != null) {
                for (int i = 0; i < naverWorkerThreads.length; i++) {
                    if(naverWorkerThreads[i]!=null){
                        naverWorkerThreads[i].setRun(false);
                        naverWorkerThreads[i].interrupt();
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
            logger.info("## NAVER send processing. Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
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

    public PreventMobileCacheMgr getPreventMobileCacheMgr() {
        return preventMobileCacheMgr;
    }

    public CancleManager getCancleManager() {
        return cancleManager;
    }

    public UmsSendCheckService getUmsSendCheckService() {
        return umsSendCheckService;
    }

    public synchronized void fileQueueCommit(){
        try {
            if(Q_HELPER!=null) {
                Q_HELPER.commit("NAVER");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
