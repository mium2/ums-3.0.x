package kr.uracle.ums.core.ehcache;

import kr.uracle.ums.core.vo.template.CommonTemplateVo;
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
public class CommonTemplateCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    @Autowired(required = true)
    private SqlSessionTemplate sqlSessionTemplate;

    private CommonTemplateCacheMgr(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("COMMON_TEMPLATE_CACHE");


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
                    logger.info("!![CommonTemplateCacheMgr]:notifyElementExpired!!");
                }

                @Override
                public void notifyElementEvicted(Ehcache ehcache, Element element) {
                    logger.info("!![CommonTemplateCacheMgr]:NotifyElementEvicted!!");
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
    public void put(String templateId, CommonTemplateVo commonTemplateVo) throws CacheException {
        Element templateElement = new Element(templateId,commonTemplateVo);
        cache.put(templateElement);
    }

    public Element get(String templateCode) throws CacheException {
        return cache.get(templateCode);
    }

    public CommonTemplateVo getTemplate(String templateId) throws CacheException {
        Element templateElement = cache.get(templateId);
        if(templateElement==null){
            //캐시에 없을 경우 DB에서 가져옴.
            Map<String,String> dbParamMap = new HashMap<String,String>();
            dbParamMap.put("TEMPL_ID",templateId);
            CommonTemplateVo commonTemplateVo = sqlSessionTemplate.selectOne("mybatis.templete.selTemplateForCache",dbParamMap);
            // 다음을 위하여 캐시에 등록
            if(commonTemplateVo!=null) {
                put(templateId, commonTemplateVo);
            }

            return commonTemplateVo;
        }else{
            CommonTemplateVo commonTemplateVo = (CommonTemplateVo)templateElement.getObjectValue();
            return commonTemplateVo;
        }
    }

    public List<CommonTemplateVo> getAll() {
        List<CommonTemplateVo> commonTemplateVos = new ArrayList<CommonTemplateVo>();
        List keys = cache.getKeys();
        for(Object key : keys){
            Element element = cache.get(key);
            if(element!=null) {
                commonTemplateVos.add((CommonTemplateVo)element.getObjectValue());
            }
        }
        return commonTemplateVos;
    }

    public void remove(String templateId) throws CacheException {
        Element element = get(templateId);
        if(element!=null) {
            cache.remove(templateId);
        }else{
            logger.info("### CommonTemplateCacheMgr cache not exist templateCode");
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
