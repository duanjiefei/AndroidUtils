package com.djf.androidutils.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.djf.androidutils.R;
import com.djf.androidutils.cache.LruDiskCache.Editor;
import com.djf.androidutils.cache.LruDiskCache.Snapshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader {
	
	private static String TAG = "ImageLoader";
	private Context mContext;
	private LruMemoryCache memoryCache;
	private static final long DiskCacheSize = 1024 * 1024 * 50;
	private LruDiskCache diskCache;
	private boolean isDiskCached;
	private int BUFF_SIZE = 8 * 1024;
	private final int ImageLoader_TAG = R.id.imageloader_uri;
	private final int LoadBitmapResult = 1;
//	private static final int cpuSize = Runtime.getRuntime().availableProcessors();
//	private static final int corePoolSize = cpuSize +1;
//	private static final int maximumPoolSize = 2 * cpuSize +1;
//	private static final long keepAliveTime = 10L;
	
	
    private static final int CPU_COUNT = Runtime.getRuntime()
            .availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;
	/**
	 * ����ImageLoader��ʵ��
	 * @Title:getInstance
	 * @param context
	 * @return
	 * ImageLoader
	 */
	public static ImageLoader getInstance(Context context){
		Log.i(TAG, "public static ImageLoader getInstance");
		return new ImageLoader(context);
	}
	
	/**
	 * ��ʼ��ImageLoader
	 * @param context
	 */
	private ImageLoader(Context context){
		Log.i(TAG, "private ImageLoader(Context context)");
		mContext = context.getApplicationContext();
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		
		int cacheMemorySize = maxMemory/8;
		
		memoryCache = new LruMemoryCache(cacheMemorySize);//��ʼ���ڴ滺��Ĵ�С
		
		File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
		
		if(!diskCacheDir.exists()){
			diskCacheDir.mkdirs();
		}
		
		if(getUsableSpace(diskCacheDir) > DiskCacheSize){
			
			try {
				diskCache = LruDiskCache.open(diskCacheDir, 1, 1, DiskCacheSize);//��ʼ��Ӳ�̻����·������С
				isDiskCached = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ͨ��handler��������ؽ��
	 */
	private Handler handler = new Handler(Looper.getMainLooper()){
		public void handleMessage(android.os.Message msg) {
			Log.i(TAG, "public void handleMessage(android.os.Message msg)");
			switch (msg.what) {
			case LoadBitmapResult:
				Log.i(TAG, "public void handleMessage(android.os.Message msg)   LoadBitmapResult");
				LoadResult loadResult = (LoadResult) msg.obj;
				ImageView imageView = loadResult.imageView;
				String url = (String) imageView.getTag(ImageLoader_TAG);
				if(url.equals(loadResult.url)){
					imageView.setImageBitmap(loadResult.bitmap);
				}
				break;

			default:
				break;
			}
		};
	};
	
	
	 private static final ThreadFactory sThreadFactory = new ThreadFactory() {
	        private final AtomicInteger mCount = new AtomicInteger(1);

	        public Thread newThread(Runnable r) {
	            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
	        }
	    };
	/**
	 * �����̹߳���
	 */
//	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
//		private final AtomicInteger mCount = new AtomicInteger(1);
//		@Override
//		public Thread newThread(Runnable r) {
//			return new Thread("creat thread in threadFactory ,the number is "+mCount.getAndIncrement());
//		}
//	};
	
	public static final Executor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), sThreadFactory);
//	

//
//	    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
//	            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
//	            KEEP_ALIVE, TimeUnit.SECONDS,
//	            new LinkedBlockingQueue<Runnable>(), sThreadFactory);
	
	/**
	 * ����������ڴ�
	 * @Title:addBitmapToMemoryCache
	 * void
	 */
	private void addBitmapToMemoryCache(String key,Bitmap bitmap){
		
		if(memoryCache.get(key)==null){
			memoryCache.put(key, bitmap);
		}
	}
	/**
	 * �ӻ����ж�ȡͼƬ
	 * @Title:getBitmapFromMemoryCache
	 * @param url
	 * @return
	 * Bitmap
	 */
	private Bitmap getBitmapFromMemoryCache(String key){
		return memoryCache.get(key);
	}
	
	public void bindBitmap(final ImageView imageview,final String url , final int reqWidth,final int reqHeight){
		Log.i(TAG, "public void bindBitmap(final ImageView imageview,final String url , final int reqWidth,final int reqHeight)");
		imageview.setTag(ImageLoader_TAG, url);
		 Bitmap bitmap = loadBitmapFromMemory(url);
		if(bitmap != null){
			imageview.setImageBitmap(bitmap);
			return;
		}
		Log.i(TAG, "loadBitmapTask start");
		Runnable loadBitmapTask  = new Runnable() {
			
			@Override
			public void run() {
			   Log.i(TAG, "loadBitmapTask");
			   Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);
			   if(bitmap != null ){
				   LoadResult loadResult = new LoadResult(imageview, bitmap, url);
				   handler.obtainMessage(LoadBitmapResult, loadResult).sendToTarget();
			   }
			}
		};
		Log.i(TAG, "loadBitmapTask end");
		threadPoolExecutor.execute(loadBitmapTask);
		
	}
	
	/**
	 * �������� ���δ��ڴ� sd�� �����ϻ�ȡͼƬ
	 * @Title:loadBitmap
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * Bitmap
	 */
	public Bitmap loadBitmap(String url, int reqWidth, int reqHeight){
		Log.i(TAG, "public Bitmap loadBitmap(String url, int reqWidth, int reqHeight)");
		Bitmap bitmap = loadBitmapFromMemory(url);
		if(bitmap != null){
			Log.i(TAG, "loadBitmapFromMemory");
			return bitmap;
		}
		try {
			bitmap = loadBitmapFromDisk(url, reqWidth, reqHeight);
			if(bitmap != null){
				Log.i(TAG, "loadBitmapFromDisk");
				return bitmap;
			}
			bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
			if(bitmap != null){
				Log.i(TAG, "loadBitmapFromHttp");
				return bitmap;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//����ڴ��sd���ж�û�л��棬���´������ϻ�ȡ
		if(bitmap == null && !isDiskCached){
			bitmap = downLoadBitmapFromUrl(url);
		}
		return bitmap;
		
	}
	/**
	 * �ӻ����м���ͼƬ
	 * @Title:loadBitmapFromMemory
	 * @param url
	 * @return
	 * Bitmap
	 */
	private Bitmap loadBitmapFromMemory(String url){
		Log.i(TAG, "private Bitmap loadBitmapFromMemory(String url)");
		String key = hashKeyFormUrl(url);
		Bitmap bitmap = getBitmapFromMemoryCache(key);
		return bitmap;
	}
	/**
	 * ��SD���л�ȡbitmap
	 * @Title:loadBitmapFromDisk
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * @throws IOException
	 * Bitmap
	 */
	private Bitmap loadBitmapFromDisk(String url, int reqWidth, int reqHeight) throws IOException{
		Log.i(TAG, "private Bitmap loadBitmapFromDisk(String url, int reqWidth, int reqHeight) throws IOException");
		if(Looper.myLooper() == Looper.getMainLooper()){
			Log.w(TAG, "load bitmap from UI thread , it is not recommended");
		}
		
		if(diskCache == null ){
			return null;
		}
		Bitmap bitmap = null;
		String key  = hashKeyFormUrl(url);
		Snapshot snapshot =  diskCache.get(key);
		if(snapshot!=null){
			FileInputStream is = (FileInputStream) snapshot.getInputStream(0);//�����ڵ�ֻ��Ӧһ�����ݣ��˴�indexΪ0��
			FileDescriptor fd  = is.getFD();
			bitmap = ImageResizer.decodeSampledBitmapFromFileDescriptor(fd, reqWidth, reqHeight);
			if(bitmap != null){
				addBitmapToMemoryCache(key, bitmap);//�����sd�����õ��ˣ��ٽ��õ���ͼƬ�����ڴ���һ��
			}
		}
		
		return bitmap;
		
	}
	/**
	 * �������ϼ���ͼƬbitmap������Ϣ
	 * @Title:loadBitmapFromHttp
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * Bitmap
	 * @throws IOException 
	 */
	private Bitmap loadBitmapFromHttp(String url, int reqWidth,int reqHeight) throws IOException{
		Log.i(TAG, "private Bitmap loadBitmapFromHttp(String url, int reqWidth,int reqHeight) throws IOException");
           if(Looper.myLooper() == Looper.getMainLooper()){
        	   throw new RuntimeException("can not visit network from UI thread");
           }
           if(diskCache == null){
        	   return null;
           }
           
         
           String key  = hashKeyFormUrl(url);
           Editor editor = diskCache.edit(key);
           if(editor != null){
        	  OutputStream outputStream =  editor.newOutputStream(0);
        	  if(downLoadUrlToStream(url, outputStream)){
        		  editor.commit();//���������õ�key��Ӧ��������Ѿ���д��sd����
        	  }else{
        		  editor.abort();
        	  }
        	  diskCache.flush();
           }
           return loadBitmapFromDisk(url, reqWidth, reqHeight);//��sd�����õ���Ӧ��bitmap
		
	}
	
	/**
	 * ����õ�ͼƬ������  д�뵽  DISKCACHE ��Ҫд���outputStream
	 * @Title:downLoadUrlToStream
	 * @param urlString
	 * @param outputStream
	 * @return
	 * boolean
	 */
	private boolean downLoadUrlToStream(String urlString , OutputStream outputStream){
		HttpURLConnection httpURLConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream input = null;
		
		try {
			URL url = new URL(urlString);
			httpURLConnection = (HttpURLConnection) url.openConnection();
			input = new BufferedInputStream(httpURLConnection.getInputStream(), BUFF_SIZE);
			out = new BufferedOutputStream(outputStream, BUFF_SIZE);
			
			int length;
			while((length = input.read()) != -1){
				out.write(length);
			}
			
			return true;
		}  catch (Exception e) {
           Log.e("TAG >> downLoadUrlToStream", "load error"+e);//����ʧ��
		}finally{
			if(httpURLConnection!=null){
				httpURLConnection.disconnect();
			}
//			try {
//				out.close();
//				input.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		return false;
	}
	/***
	 * ֱ�Ӵ������ϻ�ȡͼƬ
	 * @Title:downLoadBitmapFromUrl
	 * @param url
	 * @return
	 * Bitmap
	 */
	private Bitmap downLoadBitmapFromUrl(String urlString){
		HttpURLConnection httpURLConnection = null;
		Bitmap bitmap = null ;
		BufferedInputStream is = null;
		try {
			URL url = new URL(urlString);
			httpURLConnection = (HttpURLConnection) url.openConnection();
			is = new BufferedInputStream(httpURLConnection.getInputStream(),BUFF_SIZE);
			bitmap = BitmapFactory.decodeStream(is);
			
		} catch (IOException e) {
			
             Log.e("TAG ", " downLoadBitmapFromUrl >>>>> load bitmap error !"+e);
		}finally{
			if(httpURLConnection != null){
				httpURLConnection.disconnect();
			}
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}
	
	/**
	 * ��ȡ
	 * @Title:getDiskCacheDir
	 * @param context
	 * @param uniqueName
	 * @return
	 * File
	 */
	private File getDiskCacheDir(Context context,String uniqueName){
		boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		String filePath;
		if(externalStorageAvailable){
			filePath = context.getExternalCacheDir().getPath();
		}else{
			filePath = context.getCacheDir().getPath();
		}
		 Log.i("TAG >> getDiskCacheDir", "cachePath == "+filePath);
		return new File(filePath+File.separator+uniqueName);
	}
//	/**
//	 * ��ȡĳ���ļ��Ŀ��ÿռ�
//	 * @Title:getAvailableSpace
//	 * @param file
//	 * @return
//	 * long
//	 */
//	private long getAvailableSpace(File file){
//		
//		StatFs statFs =  new StatFs(file.getPath());
//		       
//		long blocks = statFs.getAvailableBlocksLong();//��ȡ�����ĸ���
//		long blockSize = statFs.getBlockSizeLong();//��ȡ�����Ĵ�С
//		    
//		return blocks * blockSize;
//	}
    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }
	
	private String hashKeyFormUrl(String url) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    
    private static class LoadResult{
    	private ImageView imageView;
    	private String url;
    	private Bitmap bitmap;
    	public LoadResult(ImageView imageView, Bitmap bitmap, String url ){
    		this.imageView = imageView;
    		this.bitmap = bitmap;
    		this.url = url;
    	}
    }

}
