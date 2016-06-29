package com.djf.androidutils.cache;

import java.util.Collection;


/**
 * ����Ľӿ�
 * @author jiefeiduan
 * @date 2016-6-21
 * @description 
 * @param <K>
 * @param <V>
 */
public interface CacheAware<K,V> {
	/**
	 * �洢����
	 * @Title:put
	 * @param key
	 * @param value
	 * @return
	 * boolean
	 */
	V put(K key,V value);
	/**
	 * ��ȡ����
	 * @Title:get
	 * @param key
	 * @return
	 * V
	 */
	V get(K key);
	/**
	 * ɾ������
	 * @Title:remove
	 * @param key
	 * void
	 */
	void remove(K key);
	/**
	 * ��ջ���
	 * @Title:clear
	 * void
	 */
	void clear();
	/**
	 * �������ļ�ֵ
	 * @Title:keys
	 * @return
	 * Collection<K>
	 */
	Collection<K> keys();

}
