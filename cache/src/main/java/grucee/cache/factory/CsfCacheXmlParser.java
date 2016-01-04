package grucee.cache.factory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.digester3.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import grucee.cache.factory.CsfCacheMetaData.Cache;
import grucee.cache.factory.CsfCacheMetaData.CacheGroup;
import grucee.cache.factory.CsfCacheMetaData.CacheListener;
import grucee.cache.factory.CsfCacheMetaData.Caches;
import grucee.common.ClassHelper;
import grucee.cache.common.Constants.CacheConstants;

/**
 * system/cache/csf.cache.xm
 * @author sundd
 */
public class CsfCacheXmlParser {
	private final static transient Log LOGGER = LogFactory.getLog(CsfCacheXmlParser.class);
	private static CsfCacheXmlParser INSTANCE = null;
	private static final Object LOCKER = new Object();
	
	private Caches caches = null;

	private CsfCacheXmlParser() {
	}
	
	public static CsfCacheXmlParser getInstance() {
		if (INSTANCE == null) {
			synchronized(LOCKER) {
				CsfCacheXmlParser t = new CsfCacheXmlParser();
				t.parse();
				INSTANCE = t;
			}
		}
		return INSTANCE;
	}
	
	public Caches getCaches() {
		return caches;
	}
	
	private void parse() {
	    InputStream input = ClassHelper.getClassLoader(CsfCacheXmlParser.class).getResourceAsStream(CacheConstants.CACHE_FILE_PATH);

	    if (input == null) {
	    	LOGGER.error("csf自定义缓存配置文件未找到，位置：" + CacheConstants.CACHE_FILE_PATH);
			throw new RuntimeException("csf自定义缓存配置文件未找到，位置：" + CacheConstants.CACHE_FILE_PATH);
	    }

	    Digester digester = new Digester();

	    digester.setValidating(false);
	    digester.addObjectCreate("caches", Caches.class.getName());
	    digester.addSetProperties("caches");

	    digester.addObjectCreate("caches/cacheGroup", CacheGroup.class.getName());
	    digester.addSetProperties("caches/cacheGroup");
	    
	    digester.addObjectCreate("caches/cacheListener", CacheListener.class.getName());
	    digester.addSetProperties("caches/cacheListener");
	    
	    digester.addObjectCreate("caches/cacheGroup/cache", Cache.class.getName());
	    digester.addSetProperties("caches/cacheGroup/cache");

	    digester.addSetNext("caches/cacheGroup", "addCacheGroup", CacheGroup.class.getName());
	    digester.addSetNext("caches/cacheGroup/cache", "addCache", Cache.class.getName());
	    digester.addSetNext("caches/cacheListener", "addCacheListener", CacheListener.class.getName());
	    
	    try {
	    	caches = (Caches) digester.parse(input);
		} catch (Exception e) {
			LOGGER.error("解析自定义缓存配置失败，文件位置：" + CacheConstants.CACHE_FILE_PATH, e);
			throw new RuntimeException(e);
		} finally {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("csf.cache.properties load success.");
			}
		}
	    
	    List<Cache> cacheList = new ArrayList<Cache>();
	    //校验id不能重复
	    List<CacheGroup> groups = caches.getCacheGroups();
	    Set<String> groupNames = new HashSet<String>();
	    for (CacheGroup group : groups) {
	    	groupNames.add(group.getId());
	    	
	    	
	    	cacheList.addAll(group.getCaches());
	    }
	    if (groups.size() != groupNames.size()) {
	    	throw new RuntimeException(CacheConstants.CACHE_FILE_PATH + "中存在重复的缓存组。");
	    }
	    
	    //校验组内的cache不能重复
    	Set<String> cacheNames = new HashSet<String>();
    	for (Cache cache : cacheList) {
    		cacheNames.add(cache.getId());
    	}
    	if (cacheList.size() != cacheNames.size()) {
	    	throw new RuntimeException(CacheConstants.CACHE_FILE_PATH + "中存在重复的缓存。");
	    }
	}
	
	public static void main(String[] args) {
		CsfCacheXmlParser.getInstance().getCaches();
	}

}
