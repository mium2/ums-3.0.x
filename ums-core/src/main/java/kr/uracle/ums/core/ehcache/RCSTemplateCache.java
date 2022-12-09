package kr.uracle.ums.core.ehcache;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.uracle.ums.core.vo.template.RCSTemplateVo;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.springframework.util.ResourceUtils;

@Service
public class RCSTemplateCache {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	private CacheManager manager;
	private Cache cache;

	@Autowired(required = true)
	private SqlSessionTemplate sqlSessionTemplate;

	public RCSTemplateCache() {
		try {
			File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
			String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
			logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
			manager = CacheManager.create(ehcacheConfigAbsPath);
			getCache("RCS_TEMPLATE_CACHE");

			cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
				@Override
				public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {}
				@Override
				public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {}
				@Override
				public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {}
				@Override
				public void notifyRemoveAll(Ehcache ehcache) {}				
				@Override
				public void dispose() {}
				@Override
				public Object clone() throws CloneNotSupportedException {return null;}
				
				@Override
				public void notifyElementExpired(Ehcache ehcache, Element element) {
					logger.info("!![RCSTemplateCache]:notifyElementExpired!!");
				}

				@Override
				public void notifyElementEvicted(Ehcache ehcache, Element element) {
					logger.info("!![RCSTemplateCache]:NotifyElementEvicted!!");
				}
			});
		} catch (Exception e) {
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
		cache = (Cache) manager.getCache(cacheName);
		return cache;
	}

	public void put(String templateCode, RCSTemplateVo rcsTemplateVo) throws CacheException {
		Element templateElement = new Element(templateCode, rcsTemplateVo);
		cache.put(templateElement);
	}

	public Element get(String templateCode) throws CacheException {
		return cache.get(templateCode);
	}

	public RCSTemplateVo getTemplate(String templateId) throws CacheException {
		Element templateElement = cache.get(templateId);
		if (templateElement == null) {
			// 캐시에 없을 경우 DB에서 가져옴.
			Map<String, String> dbParamMap = new HashMap<String, String>();
			dbParamMap.put("TEMPLT_ID", templateId);
			RCSTemplateVo rcsTemplateVo = sqlSessionTemplate.selectOne("mybatis.template.rcs.selectTemplate", dbParamMap);
			
			if (rcsTemplateVo != null) {
				put(templateId, rcsTemplateVo);
			}

			return rcsTemplateVo;
		} 

		return (RCSTemplateVo) templateElement.getObjectValue();
	}

	public List<RCSTemplateVo> getAll() {
		List<RCSTemplateVo> templateVos = new ArrayList<RCSTemplateVo>();
		List keys = cache.getKeys();
		for (Object key : keys) {
			Element element = cache.get(key);
			if (element != null) {
				templateVos.add((RCSTemplateVo) element.getObjectValue());
			}
		}
		return templateVos;
	}

	public void remove(String templateCode) throws CacheException {
		Element element = get(templateCode);
		if (element != null) {
			cache.remove(templateCode);
		} else {
			logger.info("### RCSTemplateCache cache not exist templateCode");
		}
	}

	public int getSize() throws CacheException {
		return cache.getSize();
	}

	public void flush() {
		cache.flush();
	}
}
