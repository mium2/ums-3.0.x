package kr.uracle.ums.core.ehcache;

import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2020. 1. 15..
 * 핸드폰 번호를 이용하여 발송제한을 걸때 사용
 */
@Service
@SuppressWarnings("unchecked")
public class PreventMobileCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;

    @Value("${UMS.INFOMSG_PREVENT_NO_CHECK:N}")
    private String PREVENT_NO_CHECK;

    public PreventMobileCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("PREVENT_ID_CACHE");

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



    public synchronized boolean isPreventUserFromMobile(String MOBILE, String SENDCHANNEL, String MSG_TYPE){
        try {
            // 설정의 의해 정보성메세지는  수신거부 체크하지 않도록 기능구현
            if("I".equals(MSG_TYPE) && "Y".equals(PREVENT_NO_CHECK)){
                return false; //발송 가능한 유저
            }
            Element rejectUserIdElement = cache.get(MOBILE);
            if (rejectUserIdElement == null) {
                return false; //발송가능한 유저
            } else {
                Set<String> rejectChannelSet = (Set<String>) rejectUserIdElement.getObjectValue();
                if(SENDCHANNEL.startsWith("RCS")){
                    SENDCHANNEL = "RCS";
                }else if(SENDCHANNEL.endsWith("MS")){
                    SENDCHANNEL = "SMS";
                }
                if (rejectChannelSet.contains(SENDCHANNEL)) {
                    return true; //해당 채널을 발송제한으로 등록한 유저
                } else {
                    return false; //발송 가능한 유저
                }
            }
        }catch (Exception e){
            logger.error(e.toString());
            return false;
        }
    }

    public synchronized void putCache(String MOBILE, Set REJECTCHANNELS) throws CacheException {
        Element element = new Element(MOBILE,REJECTCHANNELS);
        cache.put(element);
    }

    public void update(String MOBILE, Set REJECTCHANNELS) throws CacheException {
        Element element = cache.get(MOBILE);;
        if(element!=null) {
            Element addElement = new Element(MOBILE,REJECTCHANNELS);
            cache.put(addElement);
        }
    }

    public void remove(String MOBILE) throws CacheException {
        Element element = cache.get(MOBILE);;
        if(element!=null) {
            cache.remove(MOBILE);
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
