package kr.uracle.ums.core.ehcache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

@Service
public class ValidityCacheMgr {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	public enum ValidityType{
		BANNED, REQUIRED, SENDNUM
	}
	
	private final Gson gson = new Gson();
	
	private CacheManager manager;
	private Cache cache;

	@Autowired(required = true)
	private SqlSessionTemplate sqlSessionTemplate;

	public ValidityCacheMgr() {
		try {
			File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/ehcache.xml");
			String ehcacheConfigAbsPath = ehcacheConfigFile.getAbsolutePath();
			logger.trace("## EHCACHE CONFIG ABSPATH : {}", ehcacheConfigAbsPath);
			manager = CacheManager.create(ehcacheConfigAbsPath);
			cache = (Cache) manager.getCache("VALIDITY_CACHE");

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
					logger.info("!![ValidityCacheMgr]:notifyElementExpired!!");
				}

				@Override
				public void notifyElementEvicted(Ehcache ehcache, Element element) {
					logger.info("!![ValidityCacheMgr]:NotifyElementEvicted!!");
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void put(ValidityType type, Trie trie) throws CacheException {
		Element templateElement = new Element(type, trie);
		cache.put(templateElement);
	}
	
	public void put(String key, List<String> list) throws CacheException {
		Element templateElement = new Element(key, list);
		cache.put(templateElement);
	}

	public Element get(String type) throws CacheException {
		return cache.get(type);
	}
	
    public synchronized Trie getTrie(ValidityType type){
    	Trie trie = null;
    	List<String> checkList = null;
        try {
            Element checkListElement = cache.get(type);
            if (checkListElement == null) {
                switch(type) {
                	case BANNED:
                		checkList = sqlSessionTemplate.selectList("mybatis.common.selKeyWords", "F");
                	break;
                	case REQUIRED:
                		checkList = sqlSessionTemplate.selectList("mybatis.common.selKeyWords", "R");
                    	break;
                }
                // 다음을 위하여 캐시에 등록
                if (checkList != null && checkList.size() > 0) {
                	trie = Trie.builder().ignoreCase().addKeywords(checkList).build();
                    put(type, trie);
                }
            } else {
            	trie = (Trie) checkListElement.getObjectValue();
            }
        }catch (Exception e){
            logger.error(e.toString());
            return null;
        }
        
        return trie;
    }
    
    public synchronized List<String> getCheckList(ValidityType type){
    	List<String> checkList = null;
    	String key = type.toString()+"_LIST";
        try {
        	
            Element checkListElement = cache.get(key);
            if (checkListElement == null) {
                switch(type) {
                	case BANNED:
                		checkList = sqlSessionTemplate.selectList("mybatis.common.selKeyWords", "F");
                	break;
                	case REQUIRED:
                		checkList = sqlSessionTemplate.selectList("mybatis.common.selKeyWords", "R");
                    	break;
                }
                // 다음을 위하여 캐시에 등록
                if (checkList != null && checkList.size() > 0) {
                    put(key, checkList);
                }
            } else {
            	checkList = (List<String>) checkListElement.getObjectValue();
            }
        }catch (Exception e){
            logger.error(e.toString());
            return null;
        }
        
        return checkList;
    }
    
    public synchronized List<String> getChannelType(){
    	List<String> channelList = null;
        try {
        	String key = "CHANNEL_TYPE_LIST";
            Element checkListElement = cache.get(key);
            if (checkListElement == null) {
            	channelList = sqlSessionTemplate.selectList("mybatis.common.selChannelList");
                // 다음을 위하여 캐시에 등록
                if (ObjectUtils.isNotEmpty(channelList)) {
                    put(key, channelList);
                }
            } else {
            	channelList = (List<String>) checkListElement.getObjectValue();
            }
        }catch (Exception e){
            logger.error(e.toString());
            return null;
        }
    	return channelList;
    }
    
    public synchronized List<String> getSendNum(String channel){
    	List<String> sendNumList = new ArrayList<String>();
    	String key = channel+"_SENDNUM";
        try {
        	
            Element sendNumListElement = cache.get(key);
            if (sendNumListElement == null) {
            	List<String> configList = sqlSessionTemplate.selectList("mybatis.common.selSendNums", channel);
                // 다음을 위하여 캐시에 등록
                if (configList != null && configList.size() > 0) {
                	
                	for(String jStrConfig : configList) {
                		try {
                			Map<String, Object> config = gson.fromJson(jStrConfig, new TypeToken<Map<String, Object>>(){}.getType());
                			if(config.get("senderNumber") == null) continue;
                			Object sObject = config.get("senderNumber");
                			if(sObject instanceof String) {
                				String number = sObject.toString();
                				if(StringUtils.isBlank(number))continue;
                				
                				sendNumList.add(number.replaceAll("\\D", ""));
                			}else if(sObject instanceof List<?>) {
                				for(Object num: (List<?>)sObject) {
                					String number = num.toString();
                    				if(StringUtils.isBlank(number))continue;
                					sendNumList.add(number.replaceAll("\\D", ""));
                				}
                			}
                		}catch(Exception e) {
                			continue;
                		}
                	}
                	
                	if(sendNumList.size()>0)put(key, sendNumList);
                }
            } else {
            	sendNumList = (List<String>) sendNumListElement.getObjectValue();
            }
        }catch (Exception e){
            logger.error(e.toString());
            return sendNumList;
        }
        
        return sendNumList;
    }
		
}
