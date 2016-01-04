package grucee.cache;

import java.util.Map;

/**
 * 由于采用zookeeper通知动态刷新，所以key值限定为String
 * @author sundd
 */
public interface ICsfCache {
	/**
	 * 缓存初始化时，全量加载,例如加载全表数据
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Map load() throws Exception;
	
	/**
	 * 按照key值更新，常见更新单条表记录
	 * 如果没有找到值，需要返回null
	 * @param key
	 */
	Object refresh(String key) throws Exception;
}

