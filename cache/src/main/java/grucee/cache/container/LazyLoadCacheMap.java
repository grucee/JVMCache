package grucee.cache.container;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import grucee.cache.factory.CsfCacheFactory;


/**
 * 应用场景：
 * 懒加载缓存，只有get没有set；当执行get操作，而在本地jvm并没有已经缓存的对象时，才去加载；并且保证只有一个线程会去加载。
 * 
 * 特点：
 * 1、在key上面加锁，因此一个key的获取不会阻塞另外一个key的获取。
 * 2、针对key为String，有特殊的优化，因为不适合在String类型的对象上面加锁。之所以不适合在String对象上面加锁，是因为值相同的两个String对象，
 * 根据他们构造方式的不同，可能指向不同的对象，因此造成非线程安全问题。 
 *	   一般情况下，只有编译期间可以确定下来的的字符串才能存放到缓冲区中。
 *   
 *   针对String的加锁：synchronized(key.intern()),这样保证是在常量池中的该string进行加锁。
 *   这样做的坏处是可能有大量的字符串被加入到 方法区(常量池），而方法区属于永久带，基本不进行垃圾回收。
 * 
 * @author sundd(24204)
 * @param <V>
 */
public class LazyLoadCacheMap<K,V> {
	private final static transient Log LOGGER = LogFactory.getLog(CsfCacheFactory.class);
	//cache
	private Map<K, V> innerMap = new ConcurrentHashMap<K, V>();
	private ConcurrentHashMap<K, Object> lockerMap = new ConcurrentHashMap<K, Object>();
	
	//cache loader
	private IValueLoader<K,V> loader = null;
	
	public LazyLoadCacheMap(IValueLoader<K,V> loader) {
		if (loader == null) {
			LOGGER.error("ValueLoader loader is null.");
			throw new RuntimeException("ValueLoader loader is null.");
		}
		this.loader = loader;
	}
	
	public V get(K key) throws Exception {
		V value = innerMap.get(key);
		
		if (value == null) {
			synchronized(getLocker(key)) {
				if ((value = innerMap.get(key)) == null) {
					//根据key去加载value
					value = this.loader.loadByKey(key);
					innerMap.put(key, value);
				}
			}
		}
		
		return value;
	}
	
	/**
	 * 被动的更改值
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		synchronized(getLocker(key)) {
			innerMap.put(key, value);
		}
	}
	
	public Object getLocker(K key) {
		Object locker = lockerMap.get(key);
		if (locker != null) {
			return locker;
		}
		
		//此处可能会有多个线程同时去放，但是只会有一个成功
		locker = lockerMap.putIfAbsent(key, new Object());
		if (locker == null) {
			return lockerMap.get(key);
		}
		return locker;
	}
	
	public static interface IValueLoader<K,V> {
		V loadByKey(K k) throws Exception;
	}
}
