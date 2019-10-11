package com.polarstation.diary10.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ImageUtils {
    public static String fileToString(String imageFilePath){
        File imageFile = new File(imageFilePath);
//        Log.d("Image Path", imageFilePath);
        String encodedString = "";
        try {
            byte[] fileContent = FileUtils.readFileToByteArray(imageFile);
            encodedString = Base64.encodeToString(fileContent, Base64.DEFAULT);
//            Log.d("EncodedString", encodedString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedString;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            Log.e("GetRealPathFromUriError", "getRealPathFromURI Exception : " + e.toString());
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
