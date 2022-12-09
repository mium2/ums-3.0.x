package kr.uracle.ums.core.ehcache;

import kr.uracle.ums.core.vo.member.CacheMemberVo;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
@Service
public class MemberCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;

    private MemberCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("MEMBER_CACHE");

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
                    logger.info("!![MemberCacheMgr]푸시가 제거되기 전 호출될것으로 보임:notifyElementExpired!!");
                }

                @Override
                public void notifyElementEvicted(Ehcache ehcache, Element element) {
                    logger.info("!![MemberCacheMgr]푸시가 제거된 후 호출될 것으로 보임:NotifyElementEvicted!!");
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
    public void put(String memberID, CacheMemberVo cacheMemberVo) throws CacheException {
        Element memberElement = new Element(memberID,cacheMemberVo);
        cache.put(memberElement);
    }

    public Element get(String templateCode) throws CacheException {
        return cache.get(templateCode);
    }

    public List<CacheMemberVo> getAll() {
        List<CacheMemberVo> cacheMemberVos = new ArrayList<CacheMemberVo>();
        List keys = cache.getKeys();
        for(Object key : keys){
            Element element = cache.get(key);
            if(element!=null) {
                cacheMemberVos.add((CacheMemberVo)element.getObjectValue());
            }
        }
        return cacheMemberVos;
    }

    public void remove(String memberID) throws CacheException {
        Element element = get(memberID);
        if(element!=null) {
            cache.remove(memberID);
        }else{
            logger.info("### Member cache not exist templateCode");
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
