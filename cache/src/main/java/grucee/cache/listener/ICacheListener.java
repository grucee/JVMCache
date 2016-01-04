package grucee.cache.listener;


/**
 * 数据变更监听以及通知
 * (1)针对每个缓存分组，应该配置一个监听,就是说要实现缓存变动的感知
 * (2)感知后主动调用CsfCacheFactory.getInstance().refresh
 * @author sundd
 */
public interface ICacheListener {
	
	void addCacheListenerAndRefresh();
	
}