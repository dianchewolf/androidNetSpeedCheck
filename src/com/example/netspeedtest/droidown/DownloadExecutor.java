package com.example.netspeedtest.droidown;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件下载器
 */
public final class DownloadExecutor {
    private static final String TAG = "DownloadExecutor";   // 设置LogCat日志标签
    private static final String SUFFIX = ".droidown.adl";   // 下载未完成前为文件添加的后缀名
    private DownloadLogger logger;  // 下载进度信息记录器
    private DownloadListener listener;  // 下载进度监听器
    private DownloadThread[] threads;   // 根据线程数设置下载线程池
    private Map<Integer, Long> threadData;  // 缓存各线程下载的长度
    private URL downloadUrl;    // 下载路径
    private File saveDir;   // 下载保存到的文件夹
    private File saveFile;  // 数据保存到的本地文件
    private File logFile;   // 与下载文件对应的配置文件
    private boolean initialized;    // 初始化下载标志
    private boolean downloading;    // 下载进行中标志
    private boolean paused; // 停止下载标志
    private boolean finished;   // 完成下载标志
    private boolean failed; // 下载失败标志
    private long remoteLastModified;    // 远程文件最后一次被修改的时间，断点续传时有用
    private long createDateTime;    // 下载初始化完成的时间
    private long spentTime; // 下载过程使用了的时间
	private long nowSpentTime;
    private long fileSize;  // 原始文件长度
    private long downloadedSize;    // 已下载文件长度
    private long block; // 每条线程下载的长度
    private long delay = 1000 * 5;  // 下载不正常时重新连接的等待时间
    private int retryLimit = 35;    // 下载不正常时重新连接的最大次数
    private int cacheSize = 1024 * 5;   // 下载缓冲区大小

    /**
     * 获取文件的下载路径
     *
     * @return 下载路径
     */
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * 获取文件保存到本地的目录
     *
     * @return 文件保存的目录
     */
    public File getSaveDir() {
        return saveDir;
    }

    /**
     * 获取下载线程数量
     *
     * @return 线程数
     */
    public int getThreadSize() {
        return threads.length; // 根据数组长度返回线程数
    }

    /**
     * 获取下载线程出错时重新连接的等待时间
     *
     * @return 毫秒
     */
    public long getDelay() {
        return delay;
    }

    /**
     * 设置下载线程出错时重新连接的等待时间
     *
     * @param millis 毫秒
     */
    public void setDelay(long millis) {
        this.delay = millis;
    }

    /**
     * 获取下载线程缓冲区大小
     *
     * @return 缓冲区大小
     */
    public int getCacheSize() {
        return cacheSize;
    }

    /**
     * 设置下载线程缓冲区大小。如果当前正在下载中，会在下载暂停后生效
     *
     * @param cacheSize 缓冲区大小
     */
    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * 获得允许下载线程的最大重新连接次数
     *
     * @return 最大重新连接次数
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * 设置允许下载线程的最大重新连接次数
     *
     * @param times 要设定的值
     */
    public void setRetryLimit(int times) {
        this.retryLimit = times;
    }

    /**
     * 获取下载到目前为止的耗时
     *
     * @return 毫秒数
     */
    public long getSpentTime() {
        return spentTime;
    }
	/**
	 * 获取下载到目前为止的耗时 用于计算速度
	 *
	 * @return 毫秒数
	 */
	public long getNowSpentTime() {
		return nowSpentTime;
	}
    /**
     * 获取下载的创建日期时间，本方法应该在下载初始化后才调用
     *
     * @return 毫秒数的型式
     */
    public long getCreateDateTime() {
        return createDateTime;
    }

    /**
     * 获取下载到本地的文件名称，本方法应该在下载初始化后才调用
     *
     * @return 文件名称
     */
    public String getFileName() {
        return saveFile.getName().replace(DownloadExecutor.SUFFIX, "");
    }

    /**
     * 获取下载文件大小，本方法应该在下载初始化后才调用
     *
     * @return 文件的字节数
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * 获取当前已经下载到本地的文件大小，本方法应该在下载初始化后才调用
     *
     * @return 已经下载的文件大小
     */
    public long getDownloadedSize() {
        return downloadedSize;
    }

    /**
     * 退出下载
     */
    public void pause() {
        this.paused = true; // 设置退出标志为true
    }

    /**
     * 判断下载是否被用户暂停的
     *
     * @return 是为true，否则为false
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 设置文件下载状态监听器
     *
     * @param listener 监听器
     */
    public void setDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 初始化一个下载器，默认的下载线程数量为1条，缓存空间为1024 * 5字节，下载遇到问题时会重新连接35次，每次等待时间1000 * 5毫秒
     *
     * @param downloadUrl 下载路径
     * @param saveDir     文件保存目录
     * @param threadSize  下载线程的数量
     * @throws java.net.MalformedURLException
     */
    public DownloadExecutor(String downloadUrl, File saveDir, Integer threadSize) throws MalformedURLException {
        this(new URL(downloadUrl), saveDir, threadSize);
    }

    /**
     * 初始化一个下载器，默认的下载线程数量为1条，缓存空间为1024 * 5字节，下载遇到问题时会重新连接35次，每次等待时间1000 * 5毫秒
     *
     * @param downloadUrl 下载路径
     * @param saveDir     文件保存目录
     * @param threadSize  下载线程的数量
     */
    public DownloadExecutor(URL downloadUrl, File saveDir, Integer threadSize) {
        this.downloadUrl = downloadUrl; // 对下载的路径赋值
        if (saveDir == null) {
            throw new IllegalArgumentException("the directory to save the file, which can't be null");
        }
        this.saveDir = saveDir;
        this.threads = new DownloadThread[(threadSize != null && threadSize > 0 ? threadSize : 1)]; // 根据下载的线程数创建下载线程池
        this.threadData = new ConcurrentHashMap<Integer, Long>();
        for (int i = 0; i < this.threads.length; i++) { // 遍历线程池
            this.threadData.put(i + 1, 0L);    // 初始化每条线程已经下载的数据长度为0
        }
        this.remoteLastModified = 520 * 1314;   // 避免赋值-1、0、1这类即可

        this.logger = new DownloadLogger();
        this.logger.setThreadData(this.threadData);
    }

    /**
     * 根据已经存在的下载记录文件初始化一个下载器，用于继续完成之前的下载过程。请确保在同目录下存在与记录文件对应的文件，否则会把整个文件从头开始下载
     *
     * @param downloadLog 后缀名为".droidown.cfg"的下载记录文件的所在路径
     * @throws java.io.IOException
     */
    public DownloadExecutor(String downloadLog) throws IOException {
        this(new File(downloadLog));
    }

    /**
     * 根据已经存在的下载记录文件初始化一个下载器，用于继续完成之前的下载过程。请确保在同目录下存在与记录文件对应的文件，否则会把整个文件从头开始下载
     *
     * @param downloadLog 后缀名为".droidown.cfg"的下载记录文件
     * @throws java.io.IOException
     */
    public DownloadExecutor(File downloadLog) throws IOException {
        this.logger = DownloadLogger.read(downloadLog); // 读取磁盘上的下载记录
        this.logFile = downloadLog;
        this.saveDir = downloadLog.getAbsoluteFile().getParentFile();
        this.saveFile = new File(saveDir, logFile.getName().replace(DownloadLogger.SUFFIX, DownloadExecutor.SUFFIX));    // 下载文件应该和记录文件在同一目录
        if (saveFile.exists()) {
            this.createDateTime = logger.getCreateDateTime();
            this.spentTime = logger.getSpentTime();
            this.downloadedSize = logger.getDownloadedSize();
            this.threadData = logger.getThreadData();
        } else {    // 如果同目录下只有下载记录而没有下载文件，只好重新下载
            this.createDateTime = System.currentTimeMillis();
            this.spentTime = 0;
            this.downloadedSize = 0;
            this.threadData = new ConcurrentHashMap<Integer, Long>();
            for (int i = 0; i < logger.getThreadData().size(); i++) { // 遍历线程池
                this.threadData.put(i + 1, 0L);    // 初始化每条线程已经下载的数据长度为0
            }
            this.logger.setThreadData(this.threadData);
            this.logger.setDownloadedSize(this.downloadedSize);
            this.logger.setSpentTime(this.spentTime);
            this.logger.setCreateDateTime(this.createDateTime);

            this.logger.write(this.logFile);    // 更新下载记录
        }
        this.threads = new DownloadThread[this.threadData.size()];
        this.downloadUrl = logger.getDownloadUrl();
        this.fileSize = logger.getFileSize();
        this.block = logger.getBlock();

        this.remoteLastModified = logger.getRemoteLastModified();
        print("已经下载的长度" + this.downloadedSize + "个字节"); // 打印出已经下载的数据总和
    }

    /**
     * 判断下载有没有被初始化
     *
     * @return 如果初始化了就返回true，否则为false
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 初始化下载信息
     */
    public void initialize() {
        if (isInitialized()) return;    // 防止二次初始化，提高效率
        try {
            HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();  // 建立一个远程连接句柄，此时尚未真正连接
            conn.setConnectTimeout(5 * 1000);   // 设置连接超时时间为5秒
            conn.setRequestMethod("GET");   // 设置请求方式为GET
            conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");    //设置客户端可以接受的媒体类型
            conn.setRequestProperty("Accept-Language", "zh-CN");    // 设置客户端语言
            conn.setRequestProperty("Referer", downloadUrl.toString());    // 设置请求的来源页面，便于服务端进行来源统计
            conn.setRequestProperty("Charset", "UTF-8");    // 设置客户端编码
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");    //设置用户代理
            conn.setRequestProperty("Connection", "Keep-Alive");    // 设置Connection的方式
            conn.connect(); // 和远程资源建立真正的连接，但尚无返回的数据流
            printResponseHeader(conn);  // 服务器响应返回的HTTP头字段集合
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {  // 此处的请求会打开返回流并获取返回的状态码，用于检查是否请求成功，当返回码为200时执行下面的代码
                // 根据URL指向服务器上同名资源与本地已下载文件对比判断是否适用断点续传
                long lastModified = conn.getLastModified();
                if (lastModified == this.remoteLastModified) {
                    this.initialized = true;
                    if (this.listener != null) {
                        this.listener.onInitialization(this, null);    // 通知下载初始化完成
                    }
                    return;
                }

                this.remoteLastModified = lastModified;
                this.logger.setRemoteLastModified(this.remoteLastModified);

                String contentLength = conn.getHeaderField("Content-Length");
                if (contentLength != null) {
                    this.fileSize = Long.parseLong(contentLength);  // 根据响应获取文件大小
                }
                if (this.fileSize <= 0) {
                    throw new RuntimeException("Unknown file size ");    // 当文件大小为小于等于零时抛出运行时异常
                }
                this.logger.setFileSize(this.fileSize);

                this.downloadedSize = 0;    // 设置已经下载的长度为0
                this.logger.setDownloadedSize(this.downloadedSize);

                String filename = getFileName(conn);    // 获取文件名称
                this.saveFile = new File(saveDir, filename + DownloadExecutor.SUFFIX);    // 根据文件保存目录和文件名构建保存文件
                this.logFile = new File(saveDir, filename + DownloadLogger.SUFFIX);

                // 对downloadUrl的重新赋值需要在getFileName(conn)之后
                this.downloadUrl = conn.getURL();   // 获取最终的URL以保证将要运行的DownloadThread目标一致
                this.logger.setDownloadUrl(this.downloadUrl);

                this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize / this.threads.length : this.fileSize / this.threads.length + 1;    // 计算每条线程下载的数据长度
                this.logger.setBlock(this.block);

                this.spentTime = 0;
                this.logger.setSpentTime(this.spentTime);
                this.createDateTime = System.currentTimeMillis();
                this.logger.setCreateDateTime(this.createDateTime);

                if (!saveDir.exists() && saveDir.mkdirs()) {    // 如果指定的文件不存在，则创建目录，此处可以创建多层目录
                    print("'" + saveDir + "' has been created");
                }
                this.logger.write(this.logFile);

                this.initialized = true;
                if (this.listener != null) {
                    this.listener.onInitialization(this, null);    // 通知下载初始化完成
                }
            } else {
                Log.w(TAG, "服务器响应错误:" + conn.getResponseCode() + conn.getResponseMessage()); // 打印错误
                throw new RuntimeException("Server response error ");   // 抛出运行时服务器返回异常
            }
        } catch (Exception e) {
            if (this.listener != null) {
                this.listener.onInitialization(this, e);    // 通知下载初始化失败
            }
            throw new RuntimeException("Initialization error", e); // 抛出运行时无法连接的异常
        }
    }

    /**
     * 获取文件名
     *
     * @param conn HttpURLConnection对象
     * @return 文件名
     */
    private String getFileName(HttpURLConnection conn) {
        for (int i = 0; ; i++) {    // 无限循环遍历
            String mine = conn.getHeaderField(i);   // 从返回的流中获取特定索引的头字段值
            if (mine == null) break;    // 如果遍历到了返回头末尾这退出循环
            if ("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())) {    // 获取content-disposition返回头字段，里面可能会包含文件名
                Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase()); // 使用正则表达式查询文件名
                if (m.find()) return m.group(1);    // 如果有符合正则表达规则的字符串
            }
        }
        String filename = this.downloadUrl.toString().substring(this.downloadUrl.toString().lastIndexOf('/') + 1);    // 从下载路径的字符串中获取文件名称
        if ("".equals(filename.trim())) {   // 如果获取不到文件名称
            filename = UUID.randomUUID() + ".suffix";  // 由网卡上的标识数字(每个网卡都有唯一的标识号)以及 CPU 时钟的唯一数字生成的的一个 16 字节的二进制作为文件名
        }
        return filename;
    }

    /**
     * 判断下载是不是进行中
     *
     * @return 如果是就返回true，否则为false
     */
    public boolean isDownloading() {
        return downloading;
    }

    /**
     * 调用此方法后下载正式开始
     */
    public void download() throws IOException {
        if (isDownloading() || isFinished()) return;    // 如果下载已经开始或已经完成，就不要再执行下载
        this.downloading = true;
        this.paused = false; // 设置退出标志为false
        this.failed = false;
        long startTime = System.currentTimeMillis();
	    nowSpentTime =0;
        try {
            if (!isInitialized()) { // 如果下载没有初始化，先初始化
                initialize();
            }
            if (this.listener != null) {
                this.listener.onStart(this);  // 通知下载开始
            }
            RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rwd");  // The file is opened for reading and writing. Every change of the file's content must be written synchronously to the target device.
            if (this.fileSize > 0) {
                randOut.setLength(this.fileSize);    // 设置文件的大小
            }
            randOut.close();    //关闭该文件，使设置生效
            for (int i = 0; i < this.threads.length; i++) { // 开启线程进行下载
                int threadId = i + 1;
                long downloadedLength = this.threadData.get(threadId);    // 通过特定的线程ID获取该线程已经下载的数据长度
                if (downloadedLength < this.block) { // 判断线程是否已经完成下载,否则继续下载
                    this.threads[i] = new DownloadThread(this, threadId, downloadedLength, false);    // 初始化特定id的线程
                    this.threads[i].setPriority(7); // 设置线程的优先级，Thread.NORM_PRIORITY = 5 Thread.MIN_PRIORITY = 1 Thread.MAX_PRIORITY = 10
                    this.threads[i].start();    // 启动线程
                } else {
                    this.threads[i] = null; // 表明在线程已经完成下载任务
                }
            }
            int threadFailCount = 0;
            while (!paused && this.downloadedSize < this.fileSize) {   // 循环判断所有线程是否完成下载
                Thread.sleep(900);
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinished() && !this.threads[i].isWorking()) { // 如果发现线程未完成下载且失败了,重新在已经下载的数据长度的基础上下载
                        for (int j = 0; j < this.threads.length; j++) {
                            if (this.threads[j] != null && this.threads[j].isWorking()) {
                                break;  // 只要有1条下载线程能正常工作，就继续尝试恢复没能正常工作的下载线程
                            }
                            if (j + 1 == this.threads.length && ++threadFailCount > this.retryLimit) {  // 进入这里说明已经没有下载线程能正常工作且超过了重新连接的最大限制数
                                throw new RuntimeException("No download thread functional ");
                            }
                        }
                        this.threads[i] = new DownloadThread(this, i + 1, this.threadData.get(i + 1), true);    //重新开辟下载线程
                        this.threads[i].setPriority(7); // 设置下载的优先级
                        this.threads[i].start();    // 开始下载线程
                    }
                }
	            nowSpentTime = System.currentTimeMillis() - startTime;
                if (this.listener != null) {

                    this.listener.onProgressing(this, this.downloadedSize);  // 通知目前已经下载完成的数据长度
                }
            }
            if (this.downloadedSize == this.fileSize) {
                this.finished = true;
                boolean isLogFileDeleted = this.logFile.delete();
                File newName = new File(saveDir, saveFile.getName().replace(DownloadExecutor.SUFFIX, ""));
                boolean isSaveFileRenamed = this.saveFile.renameTo(newName);
                if (isLogFileDeleted && isSaveFileRenamed) {
                    print("location of the downloaded file: " + newName);
                }
                if (this.listener != null) {
                    this.listener.onFinish(this);  // 通知下载完成
                }
            } else {
                if (this.listener != null) {
                    this.listener.onPause(this, this.downloadedSize);  // 通知下载被暂停了
                }
            }

        } catch (Exception e) {
            this.failed = true;
            if (this.listener != null) {
                this.listener.onFailure(this, e);  // 通知下载失败
            }
            throw new RuntimeException("Download error ", e);    //抛出文件下载异常

        } finally {
            if (!finished) {
                this.downloading = false;
                this.spentTime += System.currentTimeMillis() - startTime;
                this.logger.setSpentTime(this.spentTime);

                this.logger.write(this.logFile);
            }
        }
    }

    /**
     * 判断下载是否已经完成了
     *
     * @return 如果完成了就返回true，否则为false
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * 判断下载是否失败了
     *
     * @return 如果是就返回true，否则为false
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * 获取Http响应头字段
     *
     * @param http HttpURLConnection对象
     * @return 返回头字段的LinkedHashMap
     */
    private static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();   // 使用LinkedHashMap保证写入和遍历的时候的顺序相同，而且允许空值存在
        for (int i = 0; ; i++) {    // 此处为无限循环，因为不知道头字段的数量
            String fieldValue = http.getHeaderField(i); // getHeaderField(int n)用于返回 第n个头字段的值。

            if (fieldValue == null) break;  // 如果第i个字段没有值了，则表明头字段部分已经循环完毕，此处使用Break退出循环
            header.put(http.getHeaderFieldKey(i), fieldValue);  // getHeaderFieldKey(int n)用于返回 第n个头字段的键。
        }
        return header;
    }

    /**
     * 打印Http头字段
     *
     * @param http HttpURLConnection对象
     */
    private static void printResponseHeader(HttpURLConnection http) {
        Map<String, String> header = getHttpResponseHeader(http);   // 获取Http响应头字段
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            print(key + entry.getValue());
        }
    }

    /**
     * 更新指定线程在某时间片段里下载的文件大小和最后下载的位置
     *
     * @param threadId 线程id
     * @param size     时间片段里下载的文件大小
     * @param pos      最后下载的位置
     */
    private synchronized void update(int threadId, int size, long pos) throws IOException { // 使用同步关键字解决并发访问问题
        this.threadData.put(threadId, pos);   // 把制定线程ID的线程赋予最新的下载长度，以前的值会被覆盖掉
        this.downloadedSize += size;    // 把实时下载的长度加入到总下载长度中
        this.logger.setDownloadedSize(this.downloadedSize);
    }

    /**
     * 在LogCat中打印信息
     *
     * @param msg 信息字符串
     */
    private static void print(String msg) {
        Log.i(TAG, msg);    // 使用LogCat的Information方式打印信息
    }

    /**
     * 下载线程
     */
    private class DownloadThread extends Thread {
        private static final String TAG = "DownloadThread"; // 设置LogCat日志标签
        private int threadId = -1;  // 初始化线程id设置
        private long threadDownloadedSize;   // 该线程已经下载的数据长度
        private boolean retry;  // 该线程是否属于再次启动的
        private boolean working;    // 该线程有否正常工作的标志
        private boolean finished;   // 该线程是否结束的标志
        private DownloadExecutor downloader;  // 文件下载器

        /**
         * 初始化DownloadThread对象
         *
         * @param downloader           FileDownloader对象
         * @param threadDownloadedSize 整个文件目前已经下载的大小
         * @param threadId             线程的ID
         */
        public DownloadThread(DownloadExecutor downloader, int threadId, long threadDownloadedSize, boolean retry) {
            this.downloader = downloader;
            this.threadId = threadId;
            this.threadDownloadedSize = threadDownloadedSize;
            this.retry = retry;
        }

        /**
         * 线程的执行体
         */
        @Override
        public void run() {
            if (this.threadDownloadedSize < block) { // 未下载完成
                this.working = true;
                try {
                    if (this.retry) {
                        Thread.sleep(delay);
                    }
                    HttpURLConnection http = (HttpURLConnection) downloadUrl.openConnection();  // 开启HttpURLConnection连接
                    http.setConnectTimeout(5 * 1000);   // 设置连接超时时间为5秒钟
                    http.setRequestMethod("GET");   // 设置请求的方法为GET
                    http.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");   // 设置客户端可以接受的返回数据类型
                    http.setRequestProperty("Accept-Language", "zh-CN");    // 设置客户端使用的语言问中文
                    http.setRequestProperty("Referer", downloadUrl.toString()); // 设置请求的来源，便于对访问来源进行统计
                    http.setRequestProperty("Charset", "UTF-8");    // 设置通信编码为UTF-8
                    long startPos = block * (threadId - 1) + threadDownloadedSize;   // 开始位置
                    long endPos = block * threadId - 1;  // 结束位置
                    http.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);   // 设置获取实体数据的范围,如果超过了实体数据的大小会自动返回实际的数据大小
                    http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)"); // 客户端用户代理
                    http.setRequestProperty("Connection", "Keep-Alive");    // 使用长连接
//                    printResponseHeader(http);

                    if (remoteLastModified != http.getLastModified()) {
                        throw new RuntimeException("been referred to a different version of the file downloading");
                    }

                    InputStream inStream = http.getInputStream();   // 获取远程连接的输入流
                    byte[] buffer = new byte[cacheSize]; // 设置本地数据缓存的大小
                    int offset; // 设置每次读取的数据量
                    Log.i(TAG, "Thread " + this.threadId + " starts to download from position " + startPos);    // 打印该线程开始下载的位置
                    RandomAccessFile threadFile = new RandomAccessFile(saveFile, "rwd");   // If the file does not already exist then an attempt will be made to create it and it require that every update to the file's content be written synchronously to the underlying storage device.
                    threadFile.seek(startPos);  // 文件指针指向开始下载的位置
                    while (!downloader.isPaused() && (offset = inStream.read(buffer)) != -1) {    // 但用户没有要求停止下载，同时没有到达请求数据的末尾时候会一直循环读取数据
                        threadFile.write(buffer, 0, offset);    // 直接把数据写到文件中
                        this.threadDownloadedSize += offset; // 把新下载的已经写到文件中的数据加入到下载长度中
                        downloader.update(this.threadId, offset, this.threadDownloadedSize);    // 把该线程已经下载的数据长度更新到数据库和内存哈希表中
                    }   // 该线程下载数据完毕或者下载被用户停止
                    threadFile.close(); // Closes this random access file stream and releases any system resources associated with the stream.
                    inStream.close();   // Concrete implementations of this class should free any resources during close
                    if (downloader.isPaused()) {
                        Log.i(TAG, "Thread " + this.threadId + " has been paused");
                    } else {
                        Log.i(TAG, "Thread " + this.threadId + " download finish");
                    }

                    this.finished = true;   // 设置完成标志为true，无论是下载完成还是用户主动中断下载
                    this.working = false;   // 线程已经不需要工作了

                } catch (Exception e) {
                    this.working = false;   // 设置该线程已经没有正常工作
                    Log.w(TAG, "Thread " + this.threadId + ":" + e);    // 打印出异常信息
                }
            }
        }

        /**
         * 下载线程是否正常工作中
         *
         * @return true为线程工作正常，否则为false
         */
        public boolean isWorking() {
            return working;
        }

        /**
         * 下载线程是否已经结束
         *
         * @return true为线程已结束，否则为false
         */
        public boolean isFinished() {
            return this.finished;
        }
    }
}
