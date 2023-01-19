package com.itaem.downloadlibrary;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.itaem.downloadlibrary.download.DownloadService;

public class DownloadLibrary {
    private static Context appContext;
    private static DownloadService.DownloadBinder downloadBinder;
    private static ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    public static void init(Context context) {
        if (null != context) {
            appContext = context;
        }
        // 开启服务
        Intent intent = new Intent(appContext,DownloadService.class);
        appContext.startService(intent);
        // 绑定服务
        appContext.bindService(intent,connection,BIND_AUTO_CREATE);

    }

    /**
     * 传入下载url地址
     * @param url
     */
    public static void setUrl(String url){
        downloadBinder.startDownload(url);
    }

    /**
     * 取消绑定
     */
    public static void unBindService(){
        appContext.unbindService(connection);
    }

}
