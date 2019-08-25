package com.polarstation.diary10.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;

public class ImageHelper {
    public static Bitmap resize(Context context, Uri uri, int resize){
        Bitmap resizeBitmap=null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); // 1번

            int width = options.outWidth;
            int height = options.outHeight;
            int sampleSize = 1;

            Log.d("Bitmap Width", width+"");
            Log.d("Bitmap Height", height+"");

            while (true) { // 2번
                if (width / 2 < resize || height / 2 < resize)
                    break;
                width /= 2;
                height /= 2;
                sampleSize *= 2;
            }

            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); //3번

            Log.d("H:W", bitmap.getHeight()+":"+bitmap.getWidth());

            Matrix matrix = new Matrix();

            if(bitmap.getWidth() == 4032){
                matrix.postRotate(90);
                resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }else if(bitmap.getWidth() == 3264){
                matrix.postRotate(-90);
                resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
//            if(bitmap.getWidth() < bitmap.getHeight()) {
//                Log.d("ImageHelper", "portrait");
//                Matrix matrix = new Matrix();
//                matrix.postRotate(90);
//
//                resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            }else{
//                Log.d("ImageHelper", "landscape");
                resizeBitmap = bitmap;
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return resizeBitmap;
    }
//    출처: https://superwony.tistory.com/59 [개발자 키우기]
}