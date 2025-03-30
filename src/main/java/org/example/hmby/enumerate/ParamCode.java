package org.example.hmby.enumerate;

public enum ParamCode {
    /**
     * qbittorrent下载地址，用于检查下载文件
     */
    QB_DOWNLOAD_PATH,

    /**
     * qbittorrent 清理回收站
     */
    QB_RECYCLE,

    /**
     * 处理媒体文件的临时目录
     */
    TMP_MEDIA,

    /**
     * 媒体文件处理后输出目录
     */
    OUTPUT_MEDIA,

    /**
     * qbittorrent 服务器地址 （需要设置免登白名单）
     */
    QB_SERVER,

    /**
     * emby服务器地址
     */
    EMBY_SERVER,

    /**
     * emby用户名 （管理员）
     */
    EMBY_USER,

    /**
     * emby密码
     */
    EMBY_PASSWORD,

    FFMPEG_PATH,

    FFPROBE_PATH,

    /**
     * EMBY管理页默认库ID
     */
    EMBY_DEFAULT_LIBRARY_ID,

    /**
     * Emby处理输出目录ID
     */
    EMBY_OUTPUT_ID,

    /**
     * ffmpeg水印样式
     */
    FFMPEG_WATERMARKS_STYLE
}
