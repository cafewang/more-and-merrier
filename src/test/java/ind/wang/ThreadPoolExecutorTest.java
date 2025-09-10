package ind.wang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static ind.wang.ThreadPoolInspector.RUNNING;

class ThreadPoolExecutorTest {
    private static final String ADD_WORKER_SIGNATURE = "addWorker";

    @Test
    void addWorkerWhenRunningIfLessThanCorePoolSize() {
        InvocationRecorder.reset();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        Assertions.assertEquals(RUNNING, ThreadPoolInspector.getState(executor));
        Assertions.assertEquals(0, ThreadPoolInspector.getWorkerCount(executor));
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(1, ThreadPoolInspector.getWorkerCount(executor));
        Assertions.assertEquals(1, InvocationRecorder.getInvocations(ADD_WORKER_SIGNATURE));
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(2, ThreadPoolInspector.getWorkerCount(executor));
        Assertions.assertEquals(2, InvocationRecorder.getInvocations(ADD_WORKER_SIGNATURE));
        latch.countDown();
    }

    @Test
    void enQueueWhenRunningIfMoreThanCorePoolSize() {
        InvocationRecorder.reset();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(1, ThreadPoolInspector.getWorkerCount(executor));
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(1, ThreadPoolInspector.getQueueSize(executor));
        latch.countDown();
    }

    @Test
    void addWorkerWhenRunningIfQueueFullAndMoreThanCorePoolSize() {
        InvocationRecorder.reset();
        int corePoolSize = 1;
        int maximumPoolSize = 2;
        int queueSize = 1;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(queueSize));
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(1, ThreadPoolInspector.getWorkerCount(executor));
        executor.submit(taskWaitingForSignal(latch));
        int taskInQueue = ThreadPoolInspector.getQueueSize(executor);
        Assertions.assertEquals(1, taskInQueue);
        executor.submit(taskWaitingForSignal(latch));
        Assertions.assertEquals(taskInQueue, ThreadPoolInspector.getQueueSize(executor));
        Assertions.assertEquals(2, ThreadPoolInspector.getWorkerCount(executor));
        latch.countDown();
    }

    private Runnable taskWaitingForSignal(CountDownLatch latch) {
        return () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }


}
