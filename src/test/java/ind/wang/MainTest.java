package ind.wang;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;

import static ind.wang.ThreadPoolInspector.RUNNING;

class MainTest {

    @Test
    void initState() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            int state = ThreadPoolInspector.getState((ThreadPoolExecutor) executorService);
            Assertions.assertEquals(RUNNING, state);
            int workerCount = ThreadPoolInspector.getWorkerCount((ThreadPoolExecutor) executorService);
            Assertions.assertEquals(0, workerCount);
        }
    }

    @Test
    void addWorkerReturnFalseInStopState() {
        InvocationRecorder.reset();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch signal = new CountDownLatch(1);
        Runnable waitUntilSignal = () -> {
            try {
                signal.await();
            } catch (InterruptedException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        executorService.submit(waitUntilSignal);
        executorService.shutdownNow();
        Assertions.assertEquals(ThreadPoolInspector.STOP, ThreadPoolInspector.getState((ThreadPoolExecutor) executorService));
        try {
            executorService.submit(() -> System.out.println("task rejected"));
        } catch (Exception ignored) {
        }
        Assertions.assertEquals(3, InvocationRecorder.getInvocations());
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(1));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(2));
    }

    @Test
    void addWorkerReturnFalseInShutdownState() {
        InvocationRecorder.reset();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch signal = new CountDownLatch(1);
        Runnable waitUntilSignal = () -> {
            try {
                signal.await();
            } catch (InterruptedException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        executorService.submit(waitUntilSignal);
        executorService.shutdown();
        Assertions.assertEquals(ThreadPoolInspector.SHUTDOWN, ThreadPoolInspector.getState((ThreadPoolExecutor) executorService));
        try {
            executorService.submit(() -> System.out.println("task rejected"));
        } catch (Exception ignored) {
        }
        Assertions.assertEquals(3, InvocationRecorder.getInvocations());
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(1));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(2));
    }
}
