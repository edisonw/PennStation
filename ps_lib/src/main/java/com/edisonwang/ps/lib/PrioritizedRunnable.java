package com.edisonwang.ps.lib;

import java.util.Comparator;

/**
 * @author edi
 */
public class PrioritizedRunnable implements Runnable {

    public final int priority;
    private final Runnable runnable;

    public PrioritizedRunnable(int priority, Runnable runnable) {
        this.priority = priority;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }

    public static class PrioritizedRunnableComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            if ((lhs instanceof PrioritizedRunnable) && !(rhs instanceof PrioritizedRunnable)) {
                return 1;
            }
            if (!(lhs instanceof PrioritizedRunnable) && (rhs instanceof PrioritizedRunnable)) {
                return -1;
            }
            if (!(lhs instanceof PrioritizedRunnable)) {
                return 0;
            }
            int diff = ((PrioritizedRunnable) rhs).priority - ((PrioritizedRunnable) lhs).priority;
            if (diff > 0) {
                return 1;
            } else if (diff < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}