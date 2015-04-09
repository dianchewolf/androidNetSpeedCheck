package com.example.netspeedtest.droidown;

import java.io.*;
import java.net.URL;
import java.util.Map;

/**
 * 下载信息记录器
 */
public final class DownloadLogger implements Serializable {
    /**
     * 下载记录文件的后缀
     */
    public static final String SUFFIX = ".droidown.cfg";

    private URL downloadUrl;    // 下载路径
    private Map<Integer, Long> threadData;  // 缓存各线程下载的长度
    private long downloadedSize;    // 已下载文件长度
    private long fileSize;  // 原始文件长度
    private long block; // 每条线程下载的长度
    private long createDateTime;    // 创建时间
    private long spentTime; // 下载耗时
    private long remoteLastModified;    // 远程文件最后一次被修改的时间，断点续传时有用

    /**
     * 获取目标文件的下载路径
     *
     * @return 下载路径
     */
    protected URL getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * 设置目标文件的下载路径
     *
     * @param downloadUrl 下载路径
     */
    protected void setDownloadUrl(URL downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * 获取各下载线程的下载进度
     *
     * @return 各下载线程的下载进度
     */
    protected Map<Integer, Long> getThreadData() {
        return threadData;
    }

    /**
     * 设置各下载线程的下载进度
     *
     * @param threadData 各下载线程的下载进度
     */
    protected void setThreadData(Map<Integer, Long> threadData) {
        this.threadData = threadData;
    }

    /**
     * 获取已经下载到本地的文件大小
     *
     * @return 已经下载的文件大小
     */
    protected long getDownloadedSize() {
        return downloadedSize;
    }

    /**
     * 设置已经下载到本地的文件大小
     *
     * @param downloadedSize 已经下载的文件大小
     */
    protected void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    /**
     * 获取要下载的文件的原始大小
     *
     * @return 文件原始大小
     */
    protected long getFileSize() {
        return fileSize;
    }

    /**
     * 设置要下载的文件的原始大小
     *
     * @param fileSize 文件原始大小
     */
    protected void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 获取下载分段大小
     *
     * @return 分段大小
     */
    protected long getBlock() {
        return block;
    }

    /**
     * 设置下载分段大小
     *
     * @param block 分段大小
     */
    protected void setBlock(long block) {
        this.block = block;
    }

    /**
     * 获取下载创建时间
     *
     * @return 创建时间
     */
    protected long getCreateDateTime() {
        return createDateTime;
    }

    /**
     * 设置下载创建时间
     *
     * @param createDateTime 创建时间
     */
    protected void setCreateDateTime(long createDateTime) {
        this.createDateTime = createDateTime;
    }

    /**
     * 获取下载耗时
     *
     * @return 耗时
     */
    protected long getSpentTime() {
        return spentTime;
    }

    /**
     * 设置下载耗时
     *
     * @param spentTime 耗时
     */
    protected void setSpentTime(long spentTime) {
        this.spentTime = spentTime;
    }

    protected long getRemoteLastModified() {
        return remoteLastModified;
    }

    protected void setRemoteLastModified(long remoteLastModified) {
        this.remoteLastModified = remoteLastModified;
    }

    /**
     * 受保护的构造器
     */
    protected DownloadLogger() {
    }

    /**
     * 把下载记录序列化到磁盘上
     *
     * @param downloadLog 结果文件
     * @throws java.io.IOException
     */
    protected void write(File downloadLog) throws IOException {
        FileOutputStream fos = new FileOutputStream(downloadLog);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.close();
        fos.close();
    }

    /**
     * 把磁盘上的下载记录反序列化读取
     *
     * @param downloadLog 记录文件
     * @return DownloadLogger实例
     * @throws java.io.IOException
     */
    protected static DownloadLogger read(File downloadLog) throws IOException {
        FileInputStream fis = new FileInputStream(downloadLog);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
            return (DownloadLogger) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } finally {
            ois.close();
            fis.close();
        }
    }
}
