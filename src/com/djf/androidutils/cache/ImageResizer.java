package com.djf.androidutils.cache;

import java.io.FileDescriptor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageResizer {
	
	
	/**
	 * 从资源Resources文件中 构建所需bitmap
	 * @Title:decodeSampledBitmapFromResource
	 * @param res
	 * @param id
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * Bitmap
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res,int id,int reqWidth, int reqHeight){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, id, options);
		
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, id, options);
		
	}

	
	/**
	 * 从fd获得所需的bitmap
	 * @Title:decodeSampledBitmapFromFileDescriptor
	 * @param fd
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * Bitmap
	 */
	public static Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd ,int reqWidth,int reqHeight){
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true ;//只计算bitmap的宽高
		BitmapFactory.decodeFileDescriptor(fd, null, options);
		
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight); //图片需要缩小的倍数
		
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fd, null, options);
		
	}
	
	
	
	/**
	 * 图片加载时计算   图片需要缩小的倍数
	 * @Title:calculateInSampleSize
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 * int
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options ,int reqWidth,int reqHeight){
		if(reqWidth == 0 || reqHeight == 0){
			return 1;
		}
		
		 final int width = options.outWidth;
		 final int height = options.outHeight;
		
		int SampleSize = 1;
		if(width > reqWidth || height > reqHeight ){
			final int halfWidth  = width / 2;
			final int halfHeight = height / 2;
			
			while((halfWidth/SampleSize)>width && (halfHeight / SampleSize)>height){
                        SampleSize *= 2;
			}
			
            long totalPixels = width * height / SampleSize;

          
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
            	SampleSize *= 2;
                totalPixels /= 2;
            }
		}
		return SampleSize;
	}
}
