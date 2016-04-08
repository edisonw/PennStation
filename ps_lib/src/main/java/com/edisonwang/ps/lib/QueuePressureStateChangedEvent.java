package com.edisonwang.ps.lib;

/**
 * @author edi
 */
public class QueuePressureStateChangedEvent {

    public static final int STATE_GOOD = 1;
    public static final int STATE_ABOVE_THRESHOLD = 0;

    public final int state;
    public final int size;

    public QueuePressureStateChangedEvent(int state, int size) {
        this.state = state;
        this.size = size;
    }
}
