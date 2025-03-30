package org.example.hmby.enumerate;

/**
 * description:  <br>
 * date: 2022/12/8 10:13 <br>
 */
public enum MediaStatus {
    
    /**
     * 不处理
     */
    NONE,

    /**
     * 未处理
     */
    PENDING,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 已完成
     */
    SUCCESS,

    /**
     * 失败
     */
    FAIL,

    /**
     * 已删除
     */
    DELETED,

    /**
     * 等待中
     */
    WAITING,

    /**
     * 删除以及删除EMBY资源
     */
    DELETE_EMBY;

}
