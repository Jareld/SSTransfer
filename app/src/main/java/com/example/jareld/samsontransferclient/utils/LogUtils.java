package com.example.jareld.samsontransferclient.utils;

import android.util.Log;

/*
 *  @项目名：  VrexPlayer 
 *  @包名：    com.example.lyc.vrexplayer.Utils
 *  @文件名:   LogUtils
 *  @创建者:   LYC2
 *  @创建时间:  2017/2/13 16:05
 *  @描述：    TODO
 */
public class LogUtils {
    private static final String TAG = "Jareld_for_test";

    public static void prinfLog(String str) {

        Log.d(TAG, "Jareld:" + str);
    }

    public  static  void logInfo(String activity,String method,String info){
        if(true){
            Log.d(TAG+activity, "Method:"+method+"Info:"+info);
        }

    }
    public  static void logException(String activity,String ex){
        if(true){
            Log.d(TAG+activity, "logException: "+ex);


        }
    }
}
