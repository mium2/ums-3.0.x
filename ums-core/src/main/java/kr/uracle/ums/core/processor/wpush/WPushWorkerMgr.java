package kr.uracle.ums.core.processor.wpush;

import kr.uracle.ums.codec.redis.utils.TransactionUtil;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.dao.push.PushDao;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.processor.CancleManager;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.processor.push.*;
import kr.uracle.ums.core.service.AllotterManager;
import kr.uracle.ums.core.service.UmsChannelProviderFactory;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.UmsSendMacroService;
import kr.uracle.ums.core.service.queue.FileQueueData;
import kr.uracle.ums.core.service.queue.FileQueueHelper;
import kr.uracle.ums.core.service.queue.FileQueueHelperManager;
import kr.uracle.ums.core.util.tcpchecker.TcpAliveConManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service @Scope("prototype")
@SuppressWarnings("unchecked")
public class WPushWorkerMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ConcurrentLinkedQueue<PushBasicProcessBean> wpushWorkQueue = new ConcurrentLinkedQueue();
    private final WPushWorkerThread[] wpushWorkers =  new WPushWorkerThread[5];
    private int processCnt = 0;
    private long startMilSec = 0;
    @Autowired(required = true)
    private UmsDao umsDao;
    @Autowired(required = true)
    private PushDao pushDao;
    @Autowired(required = true)
    private SentInfoManager sentInfoManager;
    @Autowired(required = true)
    private TcpAliveConManager tcpAliveConManager;
    @Autowired(required = true)
    private UmsSendMacroService umsSendMacroService;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;
    @Autowired(required = true)
    private AllotterManager allotterManager;

    @Autowired(required = true)
    private PushWorkerMgrPool pushWorkerMgrPool;

    @Autowired(required = true)
    private CancleManager cancleManager;

    @Autowired(required = true)
    @Qualifier("redisTemplate_vo")
    private RedisTemplate redisTemplate_vo;
    @Value("${PUSH.SENDTYPE:HTTP}")
    private String PUSHSENDTYPE;

    // 설정피로도 체크유무
    @Value("${UMS.USE_FATIGUE_YN:N}")
    private String USE_FATIGUE;

    @Value("${PUSH.UPMC.ERR.OTHERSEND:}")
    private String errOtherSend;
    
    private final Gson gson = new Gson();
    
    private String FILEDB_PATH;
    
    private boolean useFileDB = false;
        
    private FileQueueHelper Q_HELPER;

    public WPushWorkerMgr(){}
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
        for(int i=0; i< wpushWorkers.length; i++){
            wpushWorkers[i] = new WPushWorkerThread("wpushWorkers-" + i, this);
            wpushWorkers[i].start();
        }
        logger.info("####################################");
        logger.info("# "+(useFileDB?fileDbName+" 파일큐 모드로":"")+" 웹푸시 발송 Thread 구동!");
        logger.info("####################################");
    
    }

    protected synchronized void putWork(PushBasicProcessBean _work){
    	if(useFileDB) {
    		Q_HELPER.putData(_work);
    	}else {
    		wpushWorkQueue.offer(_work);    		
    	}

        TpsManager.getInstance().addInputCnt(TpsManager.TPSSERVERKIND.WPUSH);

        notifyAll();
    }

    public synchronized PushBasicProcessBean takeWork() throws InterruptedException {
        PushBasicProcessBean baseBean= null;
        if(useFileDB) {
    		if(Q_HELPER.getDataCount() >0) {        			
    			FileQueueData qData = Q_HELPER.getData();
    			switch(qData.getHint()) {
    				case "PushEachProcessBean":
    					baseBean = gson.fromJson(qData.getData(), PushEachProcessBean.class);
    					break;
    				case "PushFailProcessBean":
    					baseBean = gson.fromJson(qData.getData(), PushFailProcessBean.class);
    					break;
    				case "PushNotSendFailProcessBean":
    					baseBean = gson.fromJson(qData.getData(), PushNotSendFailProcessBean.class);
    					break;
    			}
    		}else {
    			try {
    				wait(60000);						
				} catch (InterruptedException e) {logger.warn("WPUSH Work Interrupted call~!");}
    		}
        
        }else {
            while ((baseBean = wpushWorkQueue.poll())==null){
                try {
                    wait(60000);
                    continue;
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
            if (wpushWorkers != null) {
                for (int i = 0; i < wpushWorkers.length; i++) {
                    if(wpushWorkers[i]!=null){
                        wpushWorkers[i].setRun(false);
                        wpushWorkers[i].interrupt();
                    }
                }
            }
            if(Q_HELPER != null)Q_HELPER.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public SentInfoManager getSentInfoManager() { return sentInfoManager; }

    public UmsDao getUmsDao() { return umsDao; }

    public PushDao getPushDao() { return pushDao; }

    public TcpAliveConManager getTcpAliveConManager() { return tcpAliveConManager; }

    public String getErrOtherSend() { return errOtherSend; }
    public void setErrOtherSend(String errOtherSend) { this.errOtherSend = errOtherSend; }

    public RedisTemplate getRedisTemplate_vo() { return redisTemplate_vo; }
    public void setRedisTemplate_vo(RedisTemplate redisTemplate_vo) { this.redisTemplate_vo = redisTemplate_vo; }

    public String getPUSHSENDTYPE() { return PUSHSENDTYPE; }
    public void setPUSHSENDTYPE(String PUSHSENDTYPE) { this.PUSHSENDTYPE = PUSHSENDTYPE; }

    public CancleManager getCancleManager() { return cancleManager; }
    
    public UmsChannelProviderFactory getUmsChannelProvierFactory() { return umsChannelProvierFactory; }
    
    public AllotterManager getAllotterManager() { return allotterManager; }

    public UmsSendMacroService getUmsSendMacroService() { return umsSendMacroService; }
    public UmsSendCommonService getUmsSendCommonService() { return umsSendCommonService; }

    public PushWorkerMgrPool getPushWorkerMgrPool() { return pushWorkerMgrPool; }

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
            logger.info("## WebPush result data processing... Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
        }
    }

    public synchronized void fileQueueCommit(){
        try {
            if(Q_HELPER!=null) {
                Q_HELPER.commit("WEB PUSH");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
