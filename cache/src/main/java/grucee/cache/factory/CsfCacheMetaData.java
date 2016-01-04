package grucee.cache.factory;
import java.util.ArrayList;
import java.util.List;

public class CsfCacheMetaData {
	
	//对应<caches>标签
	public static class Caches {
		private List<CacheGroup> cacheGroups = new ArrayList<CacheGroup>();
		private List<CacheListener> cacheListener = new ArrayList<CacheListener>();
		
		public List<CacheListener> getCacheListener() {
			return cacheListener;
		}

		public void addCacheListener(CacheListener cacheListener) {
			this.cacheListener.add(cacheListener);
		}

		public List<CacheGroup> getCacheGroups() {
			return cacheGroups;
		}

		public void addCacheGroup(CacheGroup cacheGroup) {
			this.cacheGroups.add(cacheGroup);
		}
	}
	
	//对应<cacheListener>标签
	public static class CacheListener {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
	//对应<cacheGroup>标签
	public static class CacheGroup {
		private String id;
		private List<Cache> caches = new ArrayList<Cache>();
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public List<Cache> getCaches() {
			return caches;
		}
		
		public void addCache(Cache cache) {
			this.caches.add(cache);
		}
	}
	
	//对应<cache>标签
	public static class Cache {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

}