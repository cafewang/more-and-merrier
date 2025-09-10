package ind.wang;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class ThreadPoolInspector {
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    public static final int RUNNING    = -1 << COUNT_BITS;
    public static final int SHUTDOWN   =  0 << COUNT_BITS;
    public static final int STOP       =  1 << COUNT_BITS;
    public static final int TIDYING    =  2 << COUNT_BITS;
    public static final int TERMINATED =  3 << COUNT_BITS;
    private static final MethodHandles.Lookup LOOKUP;
    private static final VarHandle CTL_HANDLE;
    private static final VarHandle WORKERS_HANDLE;
    private static final VarHandle WORKER_THREAD_HANDLE;
    public static final Class<?> WORKER_CLASS = Arrays.stream(ThreadPoolExecutor.class.getDeclaredClasses())
            .filter(klass -> klass.getName().contains("Worker")).findFirst().get();

    private static final Field STATE_FIELD;

    static {
        try {
            STATE_FIELD = AbstractQueuedSynchronizer.class.getDeclaredField("state");
            STATE_FIELD.setAccessible(true); // 绕过访问检查
            LOOKUP = MethodHandles.privateLookupIn(ThreadPoolExecutor.class, MethodHandles.lookup());
            CTL_HANDLE = LOOKUP.findVarHandle(ThreadPoolExecutor.class, "ctl", AtomicInteger.class);
            WORKERS_HANDLE = LOOKUP.findVarHandle(ThreadPoolExecutor.class, "workers", HashSet.class);
            WORKER_THREAD_HANDLE = LOOKUP.findVarHandle(WORKER_CLASS, "thread", Thread.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static int getCtl(ThreadPoolExecutor executor) {
        try {
            return ((AtomicInteger)CTL_HANDLE.get(executor)).get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getWorkerCount(ThreadPoolExecutor executor) {
        int ctl = getCtl(executor);
        return ctl & COUNT_MASK;
    }

    public static int getQueueSize(ThreadPoolExecutor executor) {
        return executor.getQueue().size();
    }

    public static int getState(ThreadPoolExecutor executor) {
        int ctl = getCtl(executor);
        return ctl & (~COUNT_MASK);
    }

    public static HashSet getWorkers(ThreadPoolExecutor executor) {
        return (HashSet) WORKERS_HANDLE.get(executor);
    }

    public static int getWorkerState(Object worker) {
        if (!WORKER_CLASS.isInstance(worker)) {
            throw new IllegalArgumentException();
        }

        try {
            return (int) STATE_FIELD.get(worker);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Thread getWorkerThread(Object worker) {
        if (!WORKER_CLASS.isInstance(worker)) {
            throw new IllegalArgumentException();
        }

        return (Thread) WORKER_THREAD_HANDLE.get(worker);
    }

}
