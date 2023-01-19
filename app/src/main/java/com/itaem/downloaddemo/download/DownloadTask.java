package com.itaem.downloaddemo.download;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// 泛型参数：
// 表示在执行AsyncTask的时候需要传入一个字符串参数给后台任务；
// 表示使用整形数据来作为进度显示单位
// 表示使用整形数据来反馈执行结果
public class DownloadTask extends AsyncTask<String,Integer,Integer> {
    // 四个常量——显示下载状态
    private static final int TYPE_SUCCESS = 0;// 成功
    private static final int TYPE_FAILED = 1;// 失败
    private static final int TYPE_PAUSED = 2;// 暂停
    private static final int TYPE_CANCELED = 3;// 取消
    private DownloadListener listener;// 接口对象参数，将下载的状态通过这个参数进行回调
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 用于在后台执行具体的下载逻辑
     * @param params 下载url地址
     * @return
     */
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile saveFile = null;
        File file = null;
        try{
            long downloadedLength = 0;// 记录已下载的文件长度
            String downloadUrl = params[0]; // 获取文件url
            // 文件名字xxx.mp3
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(); //指定下载地址。sd的Download目录
            file = new File(directory+fileName);
            // 判断文件是否存在，为后面启用断点续传铺垫
            if (file.exists()){
                // 获取下载文件总字节
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);// 获取待下载文件的总长度
            Log.d("文件长度", "doInBackground: "+contentLength);
            if (contentLength == 0){
                return TYPE_FAILED;
            }else if (contentLength == downloadedLength){
                // 已经下载字节和文件总字节相等，说明已经下载完成
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    // 断点下载，指定从哪个字节开始下载（用于告诉服务器我们想从哪个字节开始下载）
                    .addHeader("RANGE","bytes = "+downloadedLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            // 文件流下载数据
            if (response!=null){
                is = response.body().byteStream();
                // 获取已下载文件
                saveFile = new RandomAccessFile(file,"rw");
                saveFile.seek(downloadedLength);// 跳过已下载字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b))!= -1){
                    // 判断用户有无出发暂停或者取消的操作
                    if (isCanceled){
                        return TYPE_CANCELED;
                    }else if (isPaused){
                        return TYPE_PAUSED;
                    }else {
                        // 实时计算当前的下载进度
                        total += len;
                        saveFile.write(b,0,len);
                        // 计算已下载的百分比
                        long progress = ((total + downloadedLength) * 100) / contentLength;
                        Log.d("TAG", "下载进度: "+progress);

                        // 他的方法可以从doInBackground调用，在后台计算仍在运行时发布UI线程上的更新。（调用此方法通知）
                        publishProgress((int)progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (is!=null){
                    is.close();
                }
                if (saveFile != null){
                    saveFile.close();
                }
                if (isCanceled && file!=null){
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    /**
     * 用于界面上更新当前的下载进度
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        // 获取当前下载进度
        int progress = values[0];
        Log.d("进度刷新", "onProgressUpdate: "+progress);
        // 与上一次下载进度比较
        if (progress > lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    /**
     * 用于通知最终的下载结果
     */
    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;

        }
    }
    public void pauseDownload(){
        isPaused = true;
    }
    public void cancelDownload(){
        isCanceled = true;
    }
    // 返回下载进度
    private long getContentLength(String downloadUrl)  {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response!=null){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }

/*        if (response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }*/
        return 0;
    }
}
