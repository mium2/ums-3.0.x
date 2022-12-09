package kr.uracle.ums.core.ehcache;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import kr.uracle.ums.core.vo.setting.ProviderVo;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

@Service
public class ChannelAllotRateCache {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	private CacheManager manager;
	private Cache cache;

	@Autowired(required = true)
	private SqlSessionTemplate sqlSessionTemplate;

	public ChannelAllotRateCache() {
		try {
			File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
			String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
			logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
			manager = CacheManager.create(ehcacheConfigAbsPath);
			getCache("CHANNEL_ALLOT_RATE");

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
					logger.info("!![ChannelAllotRateCache]:notifyElementExpired!!");
				}

				@Override
				public void notifyElementEvicted(Ehcache ehcache, Element element) {
					logger.info("!![ChannelAllotRateCache]:NotifyElementEvicted!!");
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
	
	public void put(String channel, List<ProviderVo> providerVoList) throws CacheException {
		Element element = new Element(channel, providerVoList);
		cache.put(element);
	}

	public Element get(String channel) throws CacheException {
		return cache.get(channel);
	}

	public boolean isExpiry(String channel) { 
		Element e = cache.get(channel);
		if(e == null) return true;
		return e.isExpired(); 	
	}
	
	//발송비율 쿼리 생성 - 테이블 설계 후 작업
	public List<ProviderVo> getProviders(String channel) throws CacheException {
		Element element = cache.get(channel);
		if (element == null) {
			// 캐시에 없을 경우 DB에서 가져옴.
			Map<String, String> dbParamMap = new HashMap<String, String>();
			dbParamMap.put("CHANNEL", channel);
			List<ProviderVo> providerVoList =  sqlSessionTemplate.selectList("mybatis.common.selectChannelSendRatio", dbParamMap);
			if(providerVoList != null) {
				for(ProviderVo p : providerVoList) {
					if(StringUtils.isAnyBlank(p.getCHANNEL(), p.getPROVIDER())) providerVoList.remove(p);
				}
				put(channel, providerVoList);
			}
			return providerVoList;
		} 

		return (List<ProviderVo>) element.getObjectValue();
	}
	
	public Map<String, List<ProviderVo>> refreshAllProvider() throws CacheException {
		// clean
		for (Object key : cache.getKeys()) {
			Element element = cache.get(key);
			if (element != null) cache.remove(key);
		}
		
		// setting
		Map<String, List<ProviderVo>> providerMap = new HashMap<String, List<ProviderVo>>();
		List<ProviderVo> providerVoList =  sqlSessionTemplate.selectList("mybatis.common.selectAllSendRatio");
		for(ProviderVo p: providerVoList) {
			List<ProviderVo> list = providerMap.get(p.getCHANNEL());
			if(list != null) {
				list.add(p);
				continue;
			}
			providerMap.put(p.getCHANNEL(), Arrays.asList(p));
		}
		
		for(Entry<String, List<ProviderVo>>  e : providerMap.entrySet()) {
			put(e.getKey(), e.getValue());
		}

		return providerMap;
	}
	

	public Map<String, List<ProviderVo>> getAll() {
		Map<String, List<ProviderVo>> providerMap = new HashMap<String, List<ProviderVo>>();
		List<Object> keys = cache.getKeys();
		for (Object key : keys) {
			Element element = cache.get(key);
			if (element != null) providerMap.put(key.toString(), (List<ProviderVo>) element.getObjectValue());
		}
		return providerMap;
	}
	
	public List<ProviderVo> refreshProvider(String channel) throws CacheException {
		// clean
		Element element = cache.get(channel);
		if (element != null) cache.remove(channel);

		// setting
		Map<String, String> dbParamMap = new HashMap<String, String>();
		dbParamMap.put("CHANNEL", channel);
		List<ProviderVo> providerVoList =  sqlSessionTemplate.selectList("mybatis.common.selectChannelSendRatio", dbParamMap);
		for(ProviderVo p: providerVoList) {
			if(StringUtils.isAnyBlank(p.getCHANNEL(), p.getPROVIDER())) {
				providerVoList.remove(p);
			}else {
				logger.info("{}채널 프로바이더 초기화, {} 공급사, {} 분배비율 ", channel, p.getPROVIDER(), p.getRATIO());
			}
		}

		put(channel, providerVoList);
		
		return providerVoList;
	}

	public void remove(String channel) throws CacheException {
		Element element = get(channel);
		if (element != null) {
			cache.remove(channel);
		} else {
			logger.info("### ChannelAllotRateCache cache not exist {}channel", channel);
		}
	}

	public int getSize() throws CacheException { return cache.getSize(); }

	public void flush() { cache.flush(); }
}
