package com.djf.androidutils.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.graphics.Bitmap;

public class LruMemoryCache  implements CacheAware<String , Bitmap>{
	
	private final LinkedHashMap<String, Bitmap> memoryCache;
	
	private int maxSize;
	
	private int curSize;
	
	public LruMemoryCache(int maxSize) {
		if (maxSize < 0) {
			throw new IllegalArgumentException("maxSize < 0");
		}
		
		
		this.maxSize = maxSize;
		/**
		 * 初始化LinkedHashMap
         * 第一个参数：initialCapacity，初始大小
         * 第二个参数：loadFactor，负载因子=0.75f,与map的扩容有关
         * 第三个参数：accessOrder=true，基于访问顺序；accessOrder=false，基于插入顺序
		 */
		this.memoryCache = new LinkedHashMap<String, Bitmap>(0,0.75f, true);

	}

	@Override
	public Bitmap put(String key, Bitmap value) {
		
		if(key == null || value == null ){
			throw new NullPointerException(" key == null || value == null");
		}
		
		Bitmap previous; 
		synchronized (this) {
			
			curSize += sizeOf(key, value);//重新计算当前缓存容量的大小
			
			previous = memoryCache.put(key, value);//将图片放入缓存
			
			//如果存在冲突
			if(previous != null){
				curSize -= sizeOf(key, previous);//重新调整当前缓存的容量
			}
		}
		
		//如果存在冲突
		if(previous != null){
            /*
             * previous值被剔除了，此次添加的 value 已经作为key的 新值
             * 告诉 自定义 的 entryRemoved 方法
             */
			entryRemoved(false, key, previous, value);
		}
		
		trimToSize(maxSize);//计算缓存
		
		return previous;
	}

	@Override
	public Bitmap get(String key) {
		
		if(key == null){
			throw new NullPointerException("key == null");
		}
		
		synchronized (this) {
			return memoryCache.get(key);
		}
		
	}

	/**
	 * 移除对应的缓存
	 */
	public void remove(String key) {
		if(key == null ){
			throw new NullPointerException("key == null");
		}
		
		synchronized (this) {
			Bitmap previous = memoryCache.remove(key);
			if(previous != null){
				curSize -= sizeOf(key, previous);
			}
		}
		
	}

	/**
	 * 清除缓存
	 */
	public void clear() {
		trimToSize(-1);
	}
/**
 * 返回所有的 key    url
 */
	@Override
	public Collection<String> keys() {
		return new HashSet<>(memoryCache.keySet());
	}

	
	
	
	
	/**
	 * 判断当前缓存是否超过缓存的最大容量，如果超过了最大缓存，会根据LRU算法删除缓存
	 * @Title:trimToSize
	 * @param maxSize
	 * void
	 */
	private void trimToSize(int maxSize){
		while(true){
			
			String key;
			Bitmap value;
			
			synchronized (this) {
				
				if(curSize < 0 || curSize != 0 && memoryCache.isEmpty()){
					throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
				}
				
				if(curSize <= maxSize || memoryCache.isEmpty()){
					break;
				}
				
				Entry<String, Bitmap> toEvict  =  memoryCache.entrySet().iterator().next();
				
				key = toEvict.getKey();
				value = toEvict.getValue();
				
				memoryCache.remove(key);
				
				curSize -= sizeOf(key, value);
				
			}
			
		}
	}
	/**
	 * 返回指定图片的大小
	 * @Title:sizeOf
	 * @param key
	 * @param value
	 * @return
	 * int
	 */
	private int sizeOf(String key, Bitmap value){
		return value.getRowBytes() * value.getHeight();
	}
	
	
    /**
     * 1.当被回收或者删掉时调用。该方法当value被回收释放存储空间时被remove调用
     * 或者替换条目值时put调用，默认实现什么都没做。
     * 2.该方法没用同步调用，如果其他线程访问缓存时，该方法也会执行。
     * 3.evicted=true：如果该条目被删除空间 （表示 进行了trimToSize or remove）  evicted=false：put冲突后 或 get里成功create后
     * 导致
     * 4.newValue!=null，那么则被put()或get()调用。
     */
    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
    }
}
