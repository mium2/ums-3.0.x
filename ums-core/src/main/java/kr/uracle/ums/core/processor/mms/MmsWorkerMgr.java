package kr.uracle.ums.core.processor.mms;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.codec.redis.vo.*;
import kr.uracle.ums.codec.redis.vo.mms.*;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.dao.mms.*;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.processor.CancleManager;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 20..
 */
@Service @Scope("prototype")
@SuppressWarnings("unchecked")
public class MmsWorkerMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ConcurrentLinkedQueue<BaseProcessBean> mmsSendWorkQueue = new ConcurrentLinkedQueue<BaseProcessBean>();
    private final MmsWorkerThread[] mmsWorkerThreads =  new MmsWorkerThread[5];
    private int processCnt = 0;
    private long startMilSec = 0;
    @Autowired(required = true)
    SentInfoManager sentInfoManager;
    
    @Autowired (required = true)
    private KtMmsDao ktMmsDao;

    @Autowired (required = true)
    private LotteMmsDao lotteMmsDao;

    @Autowired (required = true)
    private LguMmsDao lguMmsDao;

    @Autowired (required = true)
    private ImoMmsDao imoMmsDao;

    @Autowired(required = true)
    private UmsDao umsDao;

    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;

    @Autowired(required = true)
    private PreventMobileCacheMgr preventMobileCacheMgr;

    @Autowired (required = true)
    private RedisTemplate redisTemplate;

    @Autowired(required = true)
    private CancleManager cancleManager;

    @Autowired(required = true)
    private UmsSendCheckService umsSendCheckService;

    // 설정피로도 체크유무
    @Value("${UMS.USE_FATIGUE_YN:N}")
    private String USE_FATIGUE;

    @Value("${SMS.PROVIDER:LGU}")
    private String SMS_PROVIDER;
    
    private final Gson gson = new Gson();
    
    private String FILEDB_PATH;
    
    private boolean useFileDB = false;
        
    private FileQueueHelper Q_HELPER;

    public MmsWorkerMgr(){}
    
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
    		logger.error("MMS 파일 DB 파일 생성 중 에러 발송:"+e);
    		useFileDB = false;
    	}
    	
        // 리퀘스트 요청 처리 Work 쓰레드 구동
        for(int i=0; i< mmsWorkerThreads.length; i++){
            mmsWorkerThreads[i] = new MmsWorkerThread("mmsWorkerThreads-"+i, this);
            mmsWorkerThreads[i].start();
        }
        logger.info("####################################");
        logger.info("# "+(useFileDB?fileDbName+" 파일큐 모드로":"")+" MMS 발송 Thread 구동!");
        logger.info("####################################");
    }

    public IMmsDao getProviderDao(String providerName){
    	IMmsDao dao = null;
    	if(StringUtils.isBlank(providerName))providerName = umsChannelProvierFactory.getSMS_PROVIDER();
    	switch(providerName.toUpperCase()) {
            case "IMO":
                dao = imoMmsDao;
                break;
    		case "KT":
    			dao = ktMmsDao;
    			break;
            case "LOTTE":
                dao = lotteMmsDao;
                break;
	    	default :
	    		dao =lguMmsDao;
	    		break;
    	}
        return dao;
    }

    protected synchronized void putWork(BaseProcessBean _work){
    	if(useFileDB) {
    		Q_HELPER.putData(_work);
    	}else {
    		mmsSendWorkQueue.offer(_work);    		
    	}
    	
        TpsManager.getInstance().addInputCnt(TpsManager.TPSSERVERKIND.SMS);
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
                        baseBean = gson.fromJson(qData.getData(), AmMmsProcessBean.class);
                        break;
                    case "IMO":
                        baseBean = gson.fromJson(qData.getData(), ImoMmsProcessBean.class);
                        break;
                    case "LOTTE":
                        baseBean = gson.fromJson(qData.getData(), LotteMmsProcessBean.class);
                        break;
                    case "KT":
                        baseBean = gson.fromJson(qData.getData(), KtMmsProcessBean.class);
                        break;
                    case "LGU":
                        baseBean = gson.fromJson(qData.getData(), LguMmsProcessBean.class);
                        break;
                    default:
                        logger.warn("MMS 파일큐에서 알수 없는 ProcessBean 탐색 됨");
                        break;
                }
    		}else {
    			try {
    				wait(60000);						
				} catch (InterruptedException e) {
                    logger.warn("MMS Work Interrupted call~!");
                }
    		}
        }else {
            while ((baseBean = mmsSendWorkQueue.poll())==null){
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
            if (mmsWorkerThreads != null) {
                for (int i = 0; i < mmsWorkerThreads.length; i++) {
                    if(mmsWorkerThreads[i]!=null){
                        mmsWorkerThreads[i].setRun(false);
                        mmsWorkerThreads[i].interrupt();
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
            logger.info("## Sms send processing... Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
        }
    }

    public SentInfoManager getSentInfoManager() {
        return sentInfoManager;
    }

    public void setSentInfoManager(SentInfoManager sentInfoManager) {
        this.sentInfoManager = sentInfoManager;
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

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public CancleManager getCancleManager() {
        return cancleManager;
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
                Q_HELPER.commit("MMS");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
