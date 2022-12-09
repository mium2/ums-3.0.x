package kr.uracle.ums.core.ehcache;

import com.google.gson.Gson;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.*;

@Service
public class FatigueCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    @Autowired(required = true)
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private Gson gson;


    private final String EHCACHE_KEY = "FATIGUE";

    public FatigueCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("FATIGUE_CACHE");

            cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
                @Override
                public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {

                }

                @Override
                public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {

                }

                @Override
                public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {

                }

                @Override
                public void notifyElementExpired(Ehcache ehcache, Element element) {
                }

                @Override
                public void notifyElementEvicted(Ehcache ehcache, Element element) {
                }

                @Override
                public void notifyRemoveAll(Ehcache ehcache) {

                }

                @Override
                public void dispose() {

                }

                @Override
                public Object clone() throws CloneNotSupportedException {
                    return null;
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public CacheManager getCacheManager() {
        return manager;
    }

    public void evictExpiredElements() {
        cache.evictExpiredElements();
        return;
    }

    public Cache getCache(String cacheName) {
        cache = (Cache)manager.getCache(cacheName);
        return cache;
    }
    public void put(String ehcacheKey, String ehcacheVal) throws CacheException {
        Element element = new Element(ehcacheKey,ehcacheVal);
        cache.put(element);
    }

    public Map<String,Double> geFatigue() throws CacheException {
        Map<String,Double> returnMap = new HashMap<>();
        try {
            Element templateElement = cache.get(EHCACHE_KEY);
            String fatigueInfoJosn = null;
            if (templateElement == null) {
                //캐시에 없을 경우 DB에서 가져옴.
                Map<String, String> dbParamMap = new HashMap<String, String>();
                Map<String, Object> fatigueInfoMap = sqlSessionTemplate.selectOne("mybatis.common.selectFatigue", dbParamMap);
                // 다음을 위하여 캐시에 등록
                if (fatigueInfoMap != null && fatigueInfoMap.size() > 0) {
                    fatigueInfoJosn =fatigueInfoMap.get("CONFIG").toString();
                    put(EHCACHE_KEY, fatigueInfoJosn);
                }
            } else {
                fatigueInfoJosn = (String) templateElement.getObjectValue();
            }
            if(fatigueInfoJosn!=null) {
                returnMap = gson.fromJson(fatigueInfoJosn, Map.class);
            }


        }catch (Exception e){
            logger.error("!!! 피로도 정보 캐쉬에서 가져오는 중 에러 발생 :"+e.toString());
        }
        return returnMap;
    }


    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}