package kr.uracle.ums.core.ehcache;

import kr.uracle.ums.core.vo.template.AltTemplateBaseVo;
import kr.uracle.ums.core.vo.template.AltTemplateLgcnsVo;
import kr.uracle.ums.core.vo.template.AltTemplateLotteVo;
import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
@Service
public class AltTemplateCacheMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private CacheManager manager;
    private Cache cache;
    @Autowired(required = true)
    private SqlSessionTemplate sqlSessionTemplate;

    private String ALT_PROVIDER;
    private List<String> USE_ALT_PROVIDERS = new ArrayList<>();
    private Map<String,AltTemplateBaseVo> testTemplMap = new HashMap<>();

    private AltTemplateCacheMgr(@Value("${KKO.PROVIDER:LGCNS}") String _ALT_PROVIDER){
        try {
            this.ALT_PROVIDER = _ALT_PROVIDER.trim();
            if(this.ALT_PROVIDER.indexOf(",")>0){
                String[] ALT_PROVIDER_ARR = this.ALT_PROVIDER.split(",");
                for(String PROVIDER : ALT_PROVIDER_ARR){
                    this.USE_ALT_PROVIDERS.add(PROVIDER.trim());
                }
            }else{
                this.USE_ALT_PROVIDERS.add(ALT_PROVIDER);
            }
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
            String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
            logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
            manager = CacheManager.create(ehcacheConfigAbsPath);
            getCache("ALT_TEMPLATE_CACHE");

            // 테스트 할 LGCNS 알림톡 템플릿 만들기
            AltTemplateLgcnsVo testTemplateVo = new AltTemplateLgcnsVo();
            testTemplateVo.setAPPROVAL("A");
            testTemplateVo.setKKOBIZCODE("10659");
            testTemplateVo.setTEMPLATECONTENTS("알림톡 테스트 발송입니다.");
//            testTemplateVo.setTEMPLATECONTENTS("#{이름}, #{아이디} ,#{금액},#{날자} CSV치환발송 알림톡 테스트 발송입니다.");
            testTemplateVo.setSENDERKEYTYPE("S");
            testTemplMap.put(testTemplateVo.getKKOBIZCODE(),testTemplateVo);

            cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
                @Override
                public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {}

                @Override
                public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {}

                @Override
                public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {}

                @Override
                public void notifyElementExpired(Ehcache ehcache, Element element) {
                    logger.info("!![AltTemplateCacheMgr]:notifyElementExpired!!");
                }

                @Override
                public void notifyElementEvicted(Ehcache ehcache, Element element) {
                    logger.info("!![AltTemplateCacheMgr]:NotifyElementEvicted!!");
                }

                @Override
                public void notifyRemoveAll(Ehcache ehcache) {}

                @Override
                public void dispose() {}

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
    public void put(String templateCode, AltTemplateBaseVo altTemplateBaseVo) throws CacheException {
        Element templateElement = new Element(templateCode,altTemplateBaseVo);
        cache.put(templateElement);
    }

    public Element get(String templateCode) throws CacheException {
        return cache.get(templateCode);
    }

    public synchronized AltTemplateBaseVo getTemplate(String templateCode) throws CacheException {
        Element templateElement = cache.get(templateCode);
        if(templateElement==null){
            //캐시에 없을 경우 DB에서 가져옴.
            Map<String,String> dbParamMap = new HashMap<String,String>();
            dbParamMap.put("KKOBIZCODE",templateCode);

            // 테스트 치환 알림톡템플릿 리턴
            if(testTemplMap.containsKey(templateCode)){
                return (AltTemplateLgcnsVo)testTemplMap.get(templateCode);
            }

            for(String PROVIDER : USE_ALT_PROVIDERS){
                if(PROVIDER.equals("LGCNS")){
                    AltTemplateLgcnsVo altTemplateLgcnsVo = sqlSessionTemplate.selectOne("mybatis.allimtolk.lgcns.selTemplate",dbParamMap);
                    // 다음을 위하여 캐시에 등록
                    if(altTemplateLgcnsVo!=null) {
                        put(templateCode, altTemplateLgcnsVo);
                        return altTemplateLgcnsVo;
                    }
                }else if(PROVIDER.equals("LOTTE")){
                    AltTemplateLotteVo altTemplateLotteVo = sqlSessionTemplate.selectOne("mybatis.allimtolk.lotte.selTemplate",dbParamMap);
                    // 다음을 위하여 캐시에 등록
                    if(altTemplateLotteVo!=null) {
                        put(templateCode, altTemplateLotteVo);
                        return altTemplateLotteVo;
                    }
                }
            }
            return null;
        }else{
            if(templateElement.getObjectValue() instanceof AltTemplateLotteVo) {
                AltTemplateLotteVo altTemplateLotteVo = (AltTemplateLotteVo) templateElement.getObjectValue();
                return altTemplateLotteVo;
            }else{
                AltTemplateLgcnsVo altTemplateLgcnsVo = (AltTemplateLgcnsVo) templateElement.getObjectValue();
                return altTemplateLgcnsVo;
            }
        }
    }


    public void remove(String templateCode) throws CacheException {
        Element element = get(templateCode);
        if(element!=null) {
            cache.remove(templateCode);
        }else{
            logger.info("### AltTemplateCacheMgr cache not exist templateCode");
        }
    }

    public int getSize() throws CacheException {
        return cache.getSize();
    }

    public void flush() {
        cache.flush();
    }
}
