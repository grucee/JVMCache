package grucee.cache.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import grucee.cache.ICsfCache;
import grucee.cache.common.Constants.CacheConstants;
import grucee.cache.factory.CsfCacheMetaData.Cache;
import grucee.cache.factory.CsfCacheMetaData.CacheGroup;
import grucee.cache.factory.CsfCacheMetaData.Caches;
import grucee.common.ClassHelper;

/**
 * 最初的目的服务信息缓存管理，现在已经做成通用
 *  (1)目前服务信息包括四张表：csf_srv_service_info,csf_srv_service_param,
 * csf_srv_service_extend_info,csf_srv_service_auth
 * (2)加载规则：根据csf.properties中的csf.integrated.centers参数决定加载哪些中心的服务信息作为缓存
 * (3)更新规则：服务治理界面对某个服务执行变更后,将变更的serviceCode写入zk,zk在通知刷新了本缓存后，需要将该serviceCode删除
 * (4)更新锁粒度：首先是serviceCode之间不互相影响；另外就是读取同一个serviceCode的时候不应该有锁；
 * 只有在同时读和写同一个serviceCode时候才需要加锁
 * 
 * @author sundd
 */
public class CsfCacheFactory {
	private final static transient Log LOGGER = LogFactory.getLog(CsfCacheFactory.class);
	/**
	 * 实际数据缓存
	 */
	@SuppressWarnings("rawtypes")
	private final ConcurrentHashMap<Class, Map> CACHES = new ConcurrentHashMap<Class, Map>();
	/**
	 * group->实例列表
	 */
	private final ConcurrentHashMap<String, List<ICsfCache>> INSTANCES = new ConcurrentHashMap<String, List<ICsfCache>>();

	/**
	 * 根据clazz反查归属的groupId，便于快速查询
	 */
	@SuppressWarnings("rawtypes")
	private final ConcurrentHashMap<Class, String> GROUP_QUICK_QUERY = new ConcurrentHashMap<Class, String>();
	/**
	 * groupid@@key的锁
	 */
	private ConcurrentHashMap<String, ReadWriteLock> LOCKERS = new ConcurrentHashMap<String, ReadWriteLock>();
	// 组装锁的key值
	private String SEPERATOR = "@@";

	private static CsfCacheFactory INSTANCE = null;
	private static final Object LOCKER = new Object();
	//控制监听器注册
	private static AtomicBoolean LISTENER_INITED = new AtomicBoolean(false);
	
	private CsfCacheFactory(){}
	
	/*static {
		//注册缓存变动监听不能放在此处，因为缓存变动监听器里面可能会直接调用CsfcacheFactory.refresh，而此时CsfCacheFactory类还没有初始化完成
		//registerCacheChangeListener();
		
	}*/

	public static CsfCacheFactory getInstance() {
		if (INSTANCE == null) {
			synchronized(LOCKER) {
				CsfCacheFactory t = new CsfCacheFactory();
				//从csf.cache.xml初始化
				t.init();
				INSTANCE = t;
			}
		}
		//初始化完成，INSTANCE不为空，此时缓存已经可以接受刷新refresh了
		
		//注册监听器之所以不能放在init中，是因为注册监听器中可能会直接调用CsfCacheFactory.getInstance().refresh()，
		//而此时CsfCacheFactory还处在init中，表示还没有初始化完成，是不能调用refresh的
		//true:表示第一次初始化，false表示已经在初始化或者初始化已经完成了
		if (LISTENER_INITED.compareAndSet(false, true)) {
			registerCacheChangeListener();
		}
		
		return INSTANCE;
	}
	
	@SuppressWarnings("rawtypes")
	public void init() {
		Caches caches = CsfCacheXmlParser.getInstance().getCaches();
		if (caches == null) {
			throw new RuntimeException(CacheConstants.CACHE_FILE_PATH + "配置存在问题");
		}

		List<CacheGroup> groups = caches.getCacheGroups();
		for (CacheGroup group : groups) {
			String groupId = group.getId();

			List<Cache> cacheList = group.getCaches();

			List<ICsfCache> cacheInstanceList = new ArrayList<ICsfCache>();
			for (Cache cache : cacheList) {
				String cacheId = StringUtils.trim(cache.getId());
				Class cacheClazz = ClassHelper.getClass(cacheId, CsfCacheFactory.class);
				if (cacheClazz == null) {
					// 提早报警比较好
					throw new RuntimeException("未找到配置的缓存实现类:" + cacheId);
				}

				if (!ICsfCache.class.isAssignableFrom(cacheClazz)) {
					throw new RuntimeException("配置的缓存实现类:" + cacheId + "未实现ICsfCache接口。");
				}

				// 类到归属组的缓存
				GROUP_QUICK_QUERY.put(cacheClazz, groupId);

				// 实例化
				ICsfCache instance = null;
				try {
					instance = (ICsfCache) cacheClazz.newInstance();
				} catch (Exception e) {
					throw new RuntimeException("实例化配置的缓存实现类:" + cacheId + "失败。");
				}

				cacheInstanceList.add(instance);

				// 调用缓存实现类，加载数据
				try {
					CACHES.put(cacheClazz, instance.load());
				} catch (Exception e) {
					//全量加载通常在启动时， 失败抛出异常
					throw new RuntimeException("缓存:" + cacheId + "加载失败。");
				}
			}

			// 缓存实例
			INSTANCES.put(groupId, cacheInstanceList);
		}
	}

	/**
	 * 注册自定义的缓存更新监听器
	 */
	private static void registerCacheChangeListener() {
		Caches caches = CsfCacheXmlParser.getInstance().getCaches();
		if (caches == null) {
			throw new RuntimeException(CacheConstants.CACHE_FILE_PATH + "配置存在问题");
		}
		
	}
	
	/**
	 * 需要更新的组以及改组中的索引
	 * 这方法的返回值表示已经正确刷新的索引，需要从zk中删除
	 * @param serviceCodeList
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> refresh(String groupId, List<String> keys) {
		//刷新成功的列表
		List<String> successList = new ArrayList<String>();
		
		if (keys == null || keys.isEmpty()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("没有要刷新的列表，不应该调用此接口。");
			}
			return successList;
		}

		// 先获取该组有哪些缓存
		List<ICsfCache> instanceList = INSTANCES.get(groupId);
		if (instanceList == null || instanceList.isEmpty()) {
			return successList;
		}

		// 依次调用刷新单条记录的接口
		for (ICsfCache instance : instanceList) {
			// 要更新的缓存获取出来
			Map cache = CACHES.get(instance.getClass());
			for (String key : keys) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("刷新缓存,group:" + groupId + ",key:" + key);
				}
				
				Object o = null;
				try {
					o = instance.refresh(key);
				} catch (Exception e) {
					//动态刷新一般意味着程序已经正常运行，不应该抛出异常
					LOGGER.error("刷新缓存,group:" + groupId + ",key:" + key + "失败", e);
					continue;
				}

				// 获取写锁,精确到缓存中的key
				ReadWriteLock lock = getLocker(groupId + SEPERATOR + key);
				Lock writeLock = lock.writeLock();
				
				writeLock.lock();
				try {
					//该条记录要删除
					if (o == null) {
						cache.remove(key);
					} else {
						cache.put(key, o);
					}
				} finally {
					writeLock.unlock();
				}
				
				//放入成功刷新的列表
				successList.add(key);
			}
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("成功刷新的缓存,group:" + groupId + ",key列表:" + keys);
		}
		return successList;
	}

	@SuppressWarnings("rawtypes")
	public Object get(Class clazz, String key) throws Exception {
		Map cache = CACHES.get(clazz);
		if (cache == null) {
			throw new Exception(clazz.getName() + " is not configed.");
		}

		String groupId = GROUP_QUICK_QUERY.get(clazz);
		ReadWriteLock lock = getLocker(groupId + SEPERATOR + key);
		Lock readLock = lock.readLock();

		// 针对groupId@@key的一个读锁
		readLock.lock();
		try {
			return cache.get(key);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 根据String获取读写锁
	 * 
	 * @param key
	 * @return
	 */
	private ReadWriteLock getLocker(String key) {
		ReadWriteLock locker = LOCKERS.get(key);
		if (locker != null) {
			return locker;
		}

		// 此处可能会有多个线程同时去放，但是只会有一个成功
		locker = LOCKERS.putIfAbsent(key, new ReentrantReadWriteLock());
		if (locker == null) {
			return LOCKERS.get(key);
		}
		return locker;
	}

}
