package com.edisonwang.ps.lib;

/**
 *
 * @author edi
 */
public class LimitedQueueInfo {

    /**
     * How big can this queue get?
     */
    public final int limit;

    /**
     * What's the priority for this request?
     */
    public final int priority;

    /**
     * If there are multiple queues with the same limit, we need a tag to identify it.
     */
    public final String tag;

    public LimitedQueueInfo(int queueLimit, int queuePriority, String queueTag) {
        this.limit = queueLimit;
        this.priority = queuePriority;
        this.tag = queueTag;
    }
}
