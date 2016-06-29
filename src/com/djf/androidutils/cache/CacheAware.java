package com.djf.androidutils.cache;

import java.util.Collection;


/**
 * 缓存的接口
 * @author jiefeiduan
 * @date 2016-6-21
 * @description 
 * @param <K>
 * @param <V>
 */
public interface CacheAware<K,V> {
	/**
	 * 存储缓存
	 * @Title:put
	 * @param key
	 * @param value
	 * @return
	 * boolean
	 */
	V put(K key,V value);
	/**
	 * 获取缓存
	 * @Title:get
	 * @param key
	 * @return
	 * V
	 */
	V get(K key);
	/**
	 * 删除缓存
	 * @Title:remove
	 * @param key
	 * void
	 */
	void remove(K key);
	/**
	 * 清空缓存
	 * @Title:clear
	 * void
	 */
	void clear();
	/**
	 * 返回所的键值
	 * @Title:keys
	 * @return
	 * Collection<K>
	 */
	Collection<K> keys();

}
