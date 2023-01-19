package com.itaem.downloaddemo.download;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.itaem.downloaddemo.MainActivity;
import com.itaem.downloaddemo.R;

import java.io.File;

public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;
    private NotificationCompat.Builder builder;
    // 创建接口实例
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
       //     getNotificationManager().notify(1,getNotification("下载开始...",progress));
            getNotificationManager().notify(1,builder.setContentText(progress+"%")
                    // 设置此通知表示的进度（参数：通知的最大进度、当前进度、是否使用模糊进度条）
                    .setProgress(100,progress,false).build());
        }
        @Override
        public void onSuccess() {
            downloadTask = null;
            // 下载成功时将前台服务通知关闭，并创建一个下载成功通知
            stopForeground(true);
          //  getNotificationManager().notify(1,getNotification("下载成功",-1));
            getNotificationManager().notify(1,builder.setContentText("下载成功").build());
            Toast.makeText(DownloadService.this,"下载成功",Toast.LENGTH_LONG).show();
        }
        @Override
        public void onFailed() {
            downloadTask = null;
            // 下载失败时将前台服务通知关闭，并创建一个下载失败通知
            stopForeground(true);
     //       getNotificationManager().notify(1,getNotification("下载失败",-1));
            getNotificationManager().notify(1,builder.setContentText("下载失败").build());
            Toast.makeText(DownloadService.this,"下载失败",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this,"下载暂停",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            Toast.makeText(DownloadService.this,"下载取消",Toast.LENGTH_LONG).show();
        }
    };
    private DownloadBinder mBinder = new DownloadBinder();
    public DownloadService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public class DownloadBinder extends Binder{
        // 开始下载
        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                // 前台服务
              //  startForeground();
                startForeground(1,getNotification("下载中...",0));
                Toast.makeText(DownloadService.this,"下载中...",Toast.LENGTH_LONG).show();
            }
        }
        /**
         * 暂停下载
         */
        public void pauseDownload(){
            if (downloadTask != null){
                downloadTask.pauseDownload();
            }
        }
        /**
         * 取消下载
         */
        public void cancelDownload(){
            if (downloadTask != null){
                downloadTask.cancelDownload();
            }
            // 取消下载时需将文件删除，并将通知关闭
            if (downloadUrl != null){
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if (file.exists()){
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this,"下载取消",Toast.LENGTH_LONG).show();
            }
        }
    }



    /**
     * @return
     */
    private NotificationManager getNotificationManager(){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("AppTestNotificationId", "AppTestNotificationName", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);
        }
        // 检索来通知用户的后台事件
        return  manager;
    }

    /**
     * 构建用于显示下载进度的通知
     * @param title
     * @param progress
     * @return
     */
    private Notification getNotification(String title,int progress){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        String channelId = null;
        // 8.0 以上需要特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("kim.hsl", "ForegroundService");
        } else {
            channelId = "";
        }
        builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setContentTitle(title);
        if (progress>=0){
            // 当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress+"%")
                    // 设置此通知表示的进度（参数：通知的最大进度、当前进度、是否使用模糊进度条）
                    .setProgress(100,progress,false);
        }
        return builder.build();
    }
    /**
     * 启动前台服务
     */
    private void startForeground() {
        String channelId = null;
        // 8.0 以上需要特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("kim.hsl", "ForegroundService");
        } else {
            channelId = "";
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        Notification notification = builder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }

    /**
     * 创建通知通道
     * @param channelId
     * @param channelName
     * @return
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        // 参数：
        // 通道id。 每个包必须是唯一的
        // 通道的用户可见名称
        // 渠道的重要性
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }
}

