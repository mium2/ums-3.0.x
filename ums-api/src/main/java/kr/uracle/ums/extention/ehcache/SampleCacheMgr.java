package kr.uracle.ums.extention.ehcache;

import com.google.gson.Gson;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.service.bean.PreventUserBean;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.List;
import java.util.Set;
/*
*
* resource ehcache.xml에 캐시 설정 추가 필요
* */
@Service
public class SampleCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    @Autowired(required = true)
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private Gson gson;

    @Autowired(required = true)
    PreventMobileCacheMgr preventMobileCacheMgr;

    public SampleCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
//            manager = CacheManager.create(ehcacheConfigAbsPath);
//            getCache("SAMPLE_CACHE");
//
//            cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
//                @Override
//                public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {
//
//                }
//
//                @Override
//                public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {
//
//                }
//
//                @Override
//                public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {
//
//                }
//
//                @Override
//                public void notifyElementExpired(Ehcache ehcache, Element element) {
//                }
//
//                @Override
//                public void notifyElementEvicted(Ehcache ehcache, Element element) {
//                }
//
//                @Override
//                public void notifyRemoveAll(Ehcache ehcache) {
//
//                }
//
//                @Override
//                public void dispose() {
//
//                }
//
//                @Override
//                public Object clone() throws CloneNotSupportedException {
//                    return null;
//                }
//            });
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

    //이곳을 호출하면 핸드폰번호 캐시도 같이 처리 하도록 되어 있음.
    public void initializeDbSync(){
        try {
            List<PreventUserBean> dbList = sqlSessionTemplate.selectList("mybatis.common.selectAllPreventUsers");
            if (dbList != null) {
                logger.info("수신거부 동기화 사이즈 : {}", dbList.size());
                for (PreventUserBean preventUserBean : dbList) {
                    try {
                        Set rejectChannelSet = gson.fromJson(preventUserBean.getREJECTCHANNEL(), Set.class);
                        putCache(preventUserBean.getAPPID()+preventUserBean.getUSERID(), rejectChannelSet);
                        //핸드폰번호 기준 제한 채널 정보캐쉬입력
                        preventMobileCacheMgr.putCache(preventUserBean.getMOBILE(), rejectChannelSet);
                    } catch (Exception e) {
                        logger.error("!!! ERROR : " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception ex){
            logger.error("수신거부자 정보로드시 에러 : "+ex.getMessage());
        }

    }

    public synchronized boolean isPreventUserFromID(String APPID, String USERID, String SENDCHANNEL) throws Exception {
        Element rejectUserIdElement = cache.get(APPID+USERID);
        if(rejectUserIdElement==null){
            return false; //발송가능한 유저
        }else{
            Set<String> rejectChannelSet = (Set<String>)rejectUserIdElement.getObjectValue();
            if(rejectChannelSet.contains(SENDCHANNEL)){
                return true; //해당 채널을 발송제한으로 등록한 유저
            }else{
                return false; //발송 가능한 유저
            }
        }
    }

    public synchronized void putCache(String USERID, Set REJECTCHANNELS) throws CacheException {
        Element element = new Element(USERID,REJECTCHANNELS);
        cache.put(element);
    }

    public void update(String USERID, Set REJECTCHANNELS) throws CacheException {
        Element element = cache.get(USERID);;
        if(element!=null) {
            Element addElement = new Element(USERID,REJECTCHANNELS);
            cache.put(addElement);
        }
    }

    public void remove(String USERID) throws CacheException {
        Element element = cache.get(USERID);
        if(element!=null) {
            cache.remove(USERID);
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
