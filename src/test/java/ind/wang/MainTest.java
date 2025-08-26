package ind.wang;


import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;

import static ind.wang.ThreadPoolInspector.RUNNING;

class MainTest {
    void ignoreException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {}
    }

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
        Pair<ThreadPoolExecutor, CountDownLatch> pair = setUpWithEverRunningThread();
        ThreadPoolExecutor executorService = pair.getKey();
        executorService.shutdownNow();
        Assertions.assertEquals(ThreadPoolInspector.STOP, ThreadPoolInspector.getState(executorService));
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
        Pair<ThreadPoolExecutor, CountDownLatch> pair = setUpWithEverRunningThread();
        ThreadPoolExecutor executorService = pair.getKey();
        executorService.shutdown();
        Assertions.assertEquals(ThreadPoolInspector.SHUTDOWN, ThreadPoolInspector.getState(executorService));
        ignoreException(() -> executorService.submit(() -> System.out.println("task rejected")));
        Assertions.assertEquals(3, InvocationRecorder.getInvocations());
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(1));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(2));
    }

    Pair<ThreadPoolExecutor, CountDownLatch> setUpWithEverRunningThread() {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
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
        return Pair.of(executorService, signal);
    }
}
