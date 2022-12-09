package kr.uracle.ums.core.ehcache;

import kr.uracle.ums.core.vo.template.NaverTemplateVo;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NaverTemplateCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    @Autowired(required = true)
    private SqlSessionTemplate sqlSessionTemplate;

    private Map<String, NaverTemplateVo> testTemplMap = new HashMap<>();

    private NaverTemplateCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("NAVER_TEMPLATE_CACHE");

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
                    logger.info("!![NaverTemplateCacheMgr]:notifyElementExpired!!");
                }

                @Override
                public void notifyElementEvicted(Ehcache ehcache, Element element) {
                    logger.info("!![NaverTemplateCacheMgr]:NotifyElementEvicted!!");
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
    public void put(String templateCode, NaverTemplateVo naverTemplateVo) throws CacheException {
        Element templateElement = new Element(templateCode,naverTemplateVo);
        cache.put(templateElement);
    }

    public Element get(String templateCode) throws CacheException {
        return cache.get(templateCode);
    }

    public NaverTemplateVo getTemplate(String templateCode) throws CacheException {
        Element templateElement = cache.get(templateCode);
        if(templateElement==null){
            //캐시에 없을 경우 DB에서 가져옴.
            Map<String,String> dbParamMap = new HashMap<String,String>();
            dbParamMap.put("TMPL_CD",templateCode);
            NaverTemplateVo naverTemplateVo = sqlSessionTemplate.selectOne("mybatis.naver.mts.selTemplate",dbParamMap);
            // 다음을 위하여 캐시에 등록
            if(naverTemplateVo!=null) {
                put(templateCode, naverTemplateVo);
            }
            // 테스트 치환 네이버톡템플릿 리턴
            if(testTemplMap.containsKey(templateCode)){
                naverTemplateVo = testTemplMap.get(templateCode);
            }
            return naverTemplateVo;
        }else{
            NaverTemplateVo naverTemplateVo = (NaverTemplateVo)templateElement.getObjectValue();
            return naverTemplateVo;
        }
    }

    public List<NaverTemplateVo> getAll() {
        List<NaverTemplateVo> templateVos = new ArrayList<NaverTemplateVo>();
        List keys = cache.getKeys();
        for(Object key : keys){
            Element element = cache.get(key);
            if(element!=null) {
                templateVos.add((NaverTemplateVo)element.getObjectValue());
            }
        }
        return templateVos;
    }

    public void remove(String templateCode) throws CacheException {
        Element element = get(templateCode);
        if(element!=null) {
            cache.remove(templateCode);
        }else{
            logger.info("### NaverTemplateCacheMgr cache not exist templateCode");
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
