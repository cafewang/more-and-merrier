package ind.wang;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.concurrent.*;

import static ind.wang.ThreadPoolInspector.RUNNING;

class ThreadPoolExecutorTest {
    private static final String ADD_WORKER_SIGNATURE = "addWorker";
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
        Assertions.assertEquals(3, InvocationRecorder.getInvocations(ADD_WORKER_SIGNATURE));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 1));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 2));
    }

    @Test
    void addWorkerReturnFalseInShutdownState() {
        InvocationRecorder.reset();
        Pair<ThreadPoolExecutor, CountDownLatch> pair = setUpWithEverRunningThread();
        ThreadPoolExecutor executorService = pair.getKey();
        executorService.shutdown();
        Assertions.assertEquals(ThreadPoolInspector.SHUTDOWN, ThreadPoolInspector.getState(executorService));
        ignoreException(() -> executorService.submit(() -> System.out.println("task rejected")));
        Assertions.assertEquals(3, InvocationRecorder.getInvocations(ADD_WORKER_SIGNATURE));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 1));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 2));
    }

    @Test
    void addWorkerReturnFalseInTerminatedState() {
        InvocationRecorder.reset();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        executorService.shutdown();
        Assertions.assertEquals(ThreadPoolInspector.TERMINATED, ThreadPoolInspector.getState(executorService));
        ignoreException(() -> executorService.submit(() -> System.out.println("task rejected")));
        Assertions.assertEquals(2, InvocationRecorder.getInvocations(ADD_WORKER_SIGNATURE));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 0));
        Assertions.assertEquals(false, InvocationRecorder.getReturnValue(ADD_WORKER_SIGNATURE, 1));
    }

    @Test
    void testGetWorkers() {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        HashSet workers = ThreadPoolInspector.getWorkers(executorService);
        Assertions.assertEquals(1, workers.size());
        Assertions.assertEquals(1, ThreadPoolInspector.getWorkerState(workers.stream().findAny().get()));
    }


    Pair<ThreadPoolExecutor, CountDownLatch> setUpWithEverRunningThread() {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CountDownLatch signal = new CountDownLatch(1);
        Runnable waitUntilSignal = sleepAfterInterrupted(signal, 100);
        executorService.submit(waitUntilSignal);
        return Pair.of(executorService, signal);
    }

    Runnable sleepAfterInterrupted(CountDownLatch signal, int sleepMs) {
        return () -> {
            try {
                signal.await();
            } catch (InterruptedException e) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
