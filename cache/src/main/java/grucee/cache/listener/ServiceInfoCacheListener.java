package grucee.cache.listener;

/**
 * 示例
 * @author sundd
 */
public class ServiceInfoCacheListener implements ICacheListener{
	
	@Override
	public void addCacheListenerAndRefresh() {
		/*final RouterClient client = ServiceRouter.getInstance().getClient();
		if (client == null) {//一般情况下不应该
			LOGGER.error("注册服务信息缓存变动监听失败，缓存将无法更新.");
			return;
		}
		
		//变动路径配置
		final String path = CSFNodeConfigInfo.getZkRootPath() + CSFNodeConfigInfo.getZkServiceChangePath();
		client.addRouteListener(path, new RouteListener(){

			@Override
			public void notify(RouteEvent evt) throws Exception {
				//变动的serviceCode列表
				List<String> changes = evt.getChildren();
				if (changes == null || changes.isEmpty()) {
					return;
				}
				
				//目前服务信息组id协定好
				refresh(client, path, changes);
			}
			
		});
		//注册之后立即获取一次获取(全量加载和注册监听之间变动的数据）
		List<String> changes = client.getRoutes(path);
		refresh(client, path, changes);*/
	}
	
	/*private void refresh(RouterClient client, String dirPath, List<String> changes) {
		if (changes == null || changes.isEmpty()) {
			return;
		}
		
		List<String> success = CsfCacheFactory.getInstance().refresh(CsfCache.SERVICE_CACHE_CHANGE_GROUP, changes);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("待刷新的缓存列表：" + changes + ",刷新成功的缓存列表:" + success);
		}
		if (success == null || success.isEmpty()) {
			return;
		}
		
		//成功的从zk中删除
		for (String s : success) {
			client.deleteRoute(dirPath + s);
		}
	}*/
}
