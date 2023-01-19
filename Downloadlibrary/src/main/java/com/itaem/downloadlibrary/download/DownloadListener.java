package com.itaem.downloadlibrary.download;

public interface DownloadListener {
    // 通知当前下载进度
    void onProgress(int progress);
    // 通知下载成功
    void onSuccess();
    // 通知下载失败
    void onFailed();
    // 通知下载暂停事件
    void onPaused();
    // 通知下载取消事件
    void onCanceled();
}
