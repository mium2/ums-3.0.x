package kr.uracle.ums.core.util.amsoft;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.core.util.amsoft.result.ResultSqlMgr;
import kr.uracle.ums.core.util.amsoft.result.ResultUmsLogBean;
import kr.uracle.ums.tcppitcher.codec.messages.*;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;

public class AmSendMsgECache {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    private Gson gson = new Gson();

    private static AmSendMsgECache INSTANCE;

    public static AmSendMsgECache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AmSendMsgECache();
        }
        return INSTANCE;
    }

    public void init(String ehcachePath, boolean isClasspath){
        try {
            if(isClasspath){
                File ehcacheConfigFile = ResourceUtils.getFile(ehcachePath);
                ehcachePath = ehcacheConfigFile.getAbsolutePath();
            }
            manager = CacheManager.create(ehcachePath);
            getCache();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public CacheManager getCacheManager() {
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            manager = CacheManager.create(ehcacheConfigAbsPath);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return manager;
    }

    public CacheManager getCacheManager(String configFilePath) {
        try {
            manager = CacheManager.create(configFilePath);
        } catch ( CacheException e) {
            e.printStackTrace();
        }
        return manager;
    }

    public void evictExpiredElements() {
        cache.evictExpiredElements();
        return;
    }

    public Cache getCache() {
        cache = (Cache)manager.getCache("AM_SEND_CACHE");
        cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
            public Object clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void notifyElementExpired(Ehcache arg0, Element arg1) {
                logger.warn("ACK가 들어오지 않아 만료됨. ");
                Object obj = arg1.getObjectValue();
                if(obj!=null){
                    BaseBodyMessage baseBodyMessage = (BaseBodyMessage)obj;
                    logger.info("!!! 만로메세지 정보 : "+gson.toJson(baseBodyMessage));
                    // ack가 들어오지 않은 경우 . T_UMS_LOG에 인설트 하는 큐만들어 담아야 함.
                    ResultUmsLogBean resultUmsLogBean = new ResultUmsLogBean();
                    resultUmsLogBean.setTRANS_TYPE(baseBodyMessage.getTranType());
                    resultUmsLogBean.setSEND_TYPE(baseBodyMessage.getSendChannel());
                    resultUmsLogBean.setPROVIDER("AM");
                    resultUmsLogBean.setMOBILE_NUM(baseBodyMessage.getPhoneNum());
                    resultUmsLogBean.setSEND_TYPE_SEQCODE(baseBodyMessage.getMessageId());
                    resultUmsLogBean.setERRCODE(ErrorManager.ERR_500);
                    resultUmsLogBean.setRESULTMSG("ACK 들어오지 않음.");
                    ResultSqlMgr.getInstance().putWork(resultUmsLogBean);
                }
            }

            @Override
            public void notifyElementPut(Ehcache arg0, Element arg1)
                    throws CacheException {
            }

            @Override
            public void notifyElementRemoved(Ehcache arg0, Element arg1)
                    throws CacheException {
            }

            @Override
            public void notifyElementUpdated(Ehcache arg0, Element arg1)
                    throws CacheException {
            }

            @Override
            public void notifyRemoveAll(Ehcache arg0) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void notifyElementEvicted(Ehcache arg0, Element arg1) {
            }

        });
        return cache;
    }

    public void put(String msgID, BaseHeaderMessage baseHeaderMessage) throws CacheException {
        Element element = new Element(msgID, baseHeaderMessage);
        cache.put(element);
    }

    public Element getCacheElement(String name) throws CacheException {
        return cache.get(name);
    }

    public void remove(String name) throws CacheException {
        logger.debug("[AMSoft 발송메세지] Before 캐시 사이즈 : {}",cache.getSize()+"");
        cache.remove(name);
        logger.debug("[AMSoft 발송메세지] After 캐시 사이즈 : {}",cache.getSize()+"");
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }

    public void shutdown() {
        manager.shutdown();
    }
}
