package ind.wang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
