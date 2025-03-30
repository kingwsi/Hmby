package org.example.hmby.enumerate;

public enum CacheKey {
    EMBY_TOKEN,
    EMBY_KEY,
    EMBY_USER_ID,
    /**
     * qb下载黑名单
     */
    CACHE_PROCESSED_CHECK_FILTER,

    /**
     * 编码进度key
     */
    CACHE_ENCODING_PROGRESS
}
