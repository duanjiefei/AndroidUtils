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
		 * ��ʼ��LinkedHashMap
         * ��һ��������initialCapacity����ʼ��С
         * �ڶ���������loadFactor����������=0.75f,��map�������й�
         * ������������accessOrder=true�����ڷ���˳��accessOrder=false�����ڲ���˳��
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
			
			curSize += sizeOf(key, value);//���¼��㵱ǰ���������Ĵ�С
			
			previous = memoryCache.put(key, value);//��ͼƬ���뻺��
			
			//������ڳ�ͻ
			if(previous != null){
				curSize -= sizeOf(key, previous);//���µ�����ǰ���������
			}
		}
		
		//������ڳ�ͻ
		if(previous != null){
            /*
             * previousֵ���޳��ˣ��˴���ӵ� value �Ѿ���Ϊkey�� ��ֵ
             * ���� �Զ��� �� entryRemoved ����
             */
			entryRemoved(false, key, previous, value);
		}
		
		trimToSize(maxSize);//���㻺��
		
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
	 * �Ƴ���Ӧ�Ļ���
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
	 * �������
	 */
	public void clear() {
		trimToSize(-1);
	}
/**
 * �������е� key    url
 */
	@Override
	public Collection<String> keys() {
		return new HashSet<>(memoryCache.keySet());
	}

	
	
	
	
	/**
	 * �жϵ�ǰ�����Ƿ񳬹��������������������������󻺴棬�����LRU�㷨ɾ������
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
	 * ����ָ��ͼƬ�Ĵ�С
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
     * 1.�������ջ���ɾ��ʱ���á��÷�����value�������ͷŴ洢�ռ�ʱ��remove����
     * �����滻��Ŀֵʱput���ã�Ĭ��ʵ��ʲô��û����
     * 2.�÷���û��ͬ�����ã���������̷߳��ʻ���ʱ���÷���Ҳ��ִ�С�
     * 3.evicted=true���������Ŀ��ɾ���ռ� ����ʾ ������trimToSize or remove��  evicted=false��put��ͻ�� �� get��ɹ�create��
     * ����
     * 4.newValue!=null����ô��put()��get()���á�
     */
    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
    }
}
