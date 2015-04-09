package com.example.netspeedtest.droidown;

/**
 * 下载状态监听器
 */
public interface DownloadListener {

    /**
     * 下载初始化时触发，可通过判断e的值确定初始化是否成功
     *
     * @param downloader DownloadExecutor对象
     * @param e          导致初始化失败的异常，初始化成功时其值为null
     */
    void onInitialization(DownloadExecutor downloader, Exception e);

    /**
     * 下载开始时触发
     *
     * @param downloader DownloadExecutor对象
     */
    void onStart(DownloadExecutor downloader);

    /**
     * 在下载过程中触发，监听当前下载的最新进度
     *
     * @param downloader     DownloadExecutor对象
     * @param downloadedSize 当前已下载的数据大小
     */
    void onProgressing(DownloadExecutor downloader, long downloadedSize);

    /**
     * 下载被用户停止时触发
     *
     * @param downloader     DownloadExecutor对象
     * @param downloadedSize 当前已下载的数据大小
     */
    void onPause(DownloadExecutor downloader, long downloadedSize);

    /**
     * 下载失败时触发
     *
     * @param downloader DownloadExecutor对象
     * @param e          导致下载失败的异常
     */
    void onFailure(DownloadExecutor downloader, Exception e);

    /**
     * 下载完成时触发
     */
    void onFinish(DownloadExecutor downloader);
}
