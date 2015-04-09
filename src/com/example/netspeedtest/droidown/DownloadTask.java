package com.example.netspeedtest.droidown;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 下载任务，针对Android环境对DownloadExecutor进行的封装，可避免下载的时候主线程阻塞
 */
public final class DownloadTask implements Runnable {
    private static final String TAG = "DownloadTask";   // 设置LogCat日志标签
    private boolean justGetReady;   // 标识即将执行的操作（准备或开始下载）
    private DownloadExecutor loader;  // 文件下载器(下载线程的容器)

    /**
     * 获取文件的下载地址
     *
     * @return 下载地址
     */
    public URL getDownloadUrl() {
        return loader.getDownloadUrl();
    }

    /**
     * 获取文件保存到本地的目录
     *
     * @return 文件保存的目录
     */
    public File getSaveDirectory() {
        return loader.getSaveDir();
    }

    /**
     * 获取用于下载文件的线程数量
     *
     * @return 线程数量
     */
    public int getThreadSize() {
        return loader.getThreadSize();
    }

    /**
     * 获取下载任务出错时重新连接的等待时间
     *
     * @return 毫秒
     */
    public long getDelay() {
        return loader.getDelay();
    }

    /**
     * 设置下载任务出错时重新连接的等待时间
     *
     * @param millis 毫秒
     */
    public void setDelay(long millis) {
        loader.setDelay(millis);
    }

    /**
     * 获取下载线程可使用的缓冲区大小
     *
     * @return 缓冲区大小
     */
    public int getCacheSize() {
        return loader.getCacheSize();
    }

    /**
     * 设置下载线程可使用的缓冲区大小。如果下载任务当前正在下载中，会在任务暂停后才生效
     *
     * @param cacheSize 缓冲区大小
     */
    public void setCacheSize(int cacheSize) {
        loader.setCacheSize(cacheSize);
    }

    /**
     * 获得当下载连接出现故障时允许的最大重新尝试恢复次数
     *
     * @return 最大重新连接次数
     */
    public int getRetryLimit() {
        return loader.getRetryLimit();
    }

    /**
     * 设置当下载连接出现故障时允许的最大重新尝试恢复次数
     *
     * @param times 要设定的值
     */
    public void setRetryLimit(int times) {
        loader.setRetryLimit(times);
    }

    /**
     * 获取下载任务到目前为止已经花费的时间
     *
     * @return 毫秒数
     */
    public long getSpentTime() {
        return loader.getSpentTime();
    }

    /**
     * 获取创建下载任务的日期时间，本方法应该在下载任务准备就绪后才调用
     *
     * @return 毫秒数的型式
     */
    public long getCreateDateTime() {
        return loader.getCreateDateTime();
    }

    /**
     * 获取下载到本地的文件名称，本方法应该在下载任务准备就绪后才调用
     *
     * @return 文件名称
     */
    public String getFileName() {
        return loader.getFileName();
    }

    /**
     * 获取下载文件的大小，本方法应该在下载任务准备就绪后才调用
     *
     * @return 文件的字节数
     */
    public long getFileSize() {
        return loader.getFileSize();
    }

    /**
     * 获取当前已经下载到本地的文件大小，本方法应该在下载任务准备就绪后才调用
     *
     * @return 已经下载的文件大小
     */
    public long getDownloadedSize() {
        return loader.getDownloadedSize();
    }

    /**
     * 获得执行当前下载任务的DownloadExecutor实例
     *
     * @return DownloadExecutor实例
     */
    public DownloadExecutor getDownloadExecutor() {
        return loader;
    }

    /**
     * 设置下载状态监听器
     *
     * @param listener 监听器
     */
    public void setTaskStatusListener(DownloadListener listener) {
        loader.setDownloadListener(listener);
    }

    /**
     * 初始化一个下载任务，默认的下载线程数量为1条，缓存空间为1024 * 5字节，下载遇到问题时会重新连接35次，每次等待时间1000 * 5毫秒
     *
     * @param downloadUrl   下载路径
     * @param saveDirectory 下载要保存到的目录
     * @param threadSize    下载线程的数量
     * @throws java.net.MalformedURLException
     */
    public DownloadTask(String downloadUrl, File saveDirectory, Integer threadSize) throws MalformedURLException {
        this.loader = new DownloadExecutor(downloadUrl, saveDirectory, threadSize); // 初始化下载器
    }

    /**
     * 初始化一个下载任务，默认的下载线程数量为1条，缓存空间为1024 * 5字节，下载遇到问题时会重新连接35次，每次等待时间1000 * 5毫秒
     *
     * @param downloadUrl   下载路径
     * @param saveDirectory 下载要保存到的目录
     * @param threadSize    下载线程的数量
     */
    public DownloadTask(URL downloadUrl, File saveDirectory, Integer threadSize) {
        this.loader = new DownloadExecutor(downloadUrl, saveDirectory, threadSize);
    }

    /**
     * 根据已经存在的下载记录文件初始化一个下载任务，用于继续完成之前的下载过程。请确保在同目录下存在与记录文件对应的文件，否则会把整个文件从头开始下载
     *
     * @param downloadLog 后缀名为".droidown.dls"的下载记录文件的所在路径
     * @throws java.io.IOException
     */
    public DownloadTask(String downloadLog) throws IOException {
        this.loader = new DownloadExecutor(downloadLog);
    }

    /**
     * 根据已经存在的下载记录文件初始化一个下载任务，用于继续完成之前的下载过程。请确保在同目录下存在与记录文件对应的文件，否则会把整个文件从头开始下载
     *
     * @param downloadLog 后缀名为".droidown.dls"的下载记录文件
     * @throws java.io.IOException
     */
    public DownloadTask(File downloadLog) throws IOException {
        this.loader = new DownloadExecutor(downloadLog);
    }

    /**
     * 判断下载任务是否失败了
     *
     * @return 失败了为true，否则为false
     */
    public boolean isFailed() {
        return loader.isFailed();
    }

    /**
     * 判断下载任务是否已经成功完成
     *
     * @return 完成了为true，否则为false
     */
    public boolean isDone() {
        return loader.isFinished();
    }

    /**
     * 退出下载
     */
    public void stop() {
        loader.pause();
    }

    /**
     * 判断下载暂停是否人为的
     *
     * @return 人为暂停为true，否则为false
     */
    public boolean isStopped() {
        return loader.isPaused();
    }

    /**
     * 开始下载
     */
    public void start() {
        if (!loader.isDownloading()) {
            justGetReady = false;
            new Thread(this).start();   // 开始下载
        }
    }

    /**
     * 判断下载任务是否已经开始了
     *
     * @return 开始了为true，否则为false
     */
    public boolean isStarted() {
        return loader.isDownloading();
    }

    /**
     * 初始化为正式下载做准备
     */
    public void prepare() {
        if (!loader.isInitialized()) {
            justGetReady = true;
            new Thread(this).start();
        }
    }

    /**
     * 判断下载任务是否已经准备就绪
     *
     * @return 准备好了为true，否则为false
     */
    public boolean isReady() {
        return loader.isInitialized();
    }

    /**
     * 下载任务的核心执行方法 内部调用函数
     */
    @Override
    public void run() {
        try {
            if (!loader.isInitialized()) {
                loader.initialize();
            }
            if (!justGetReady) {
                loader.download();
            }

        } catch (Exception e) {
            Log.w(TAG, Log.getStackTraceString(e));
        }
    }
}
