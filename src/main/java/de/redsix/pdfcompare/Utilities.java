package de.redsix.pdfcompare;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public static MemoryUsageSetting getMemorySettings(final int bytes) throws IOException {
        return MemoryUsageSetting.setupMixed(bytes).setTempDir(FileUtils.createTempDir("PdfBox").toFile());
    }

    static class NamedThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(final String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public static ExecutorService blockingExecutor(final String name, int coreThreads, int maxThreads, int queueCapacity) {
        return new ThreadPoolExecutor(coreThreads, maxThreads, 3, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(queueCapacity), new NamedThreadFactory(name), new BlockingHandler());
    }

    public static ExecutorService blockingExecutor(final String name, int threads, int queueCapacity) {
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(queueCapacity), new NamedThreadFactory(name), new BlockingHandler());
    }

    public static void shutdownAndAwaitTermination(final ExecutorService executor, final String executorName) {
        if (executor != null) {
            executor.shutdown();
            try {
                final int timeout = 15;
                final TimeUnit unit = TimeUnit.MINUTES;
                if (!executor.awaitTermination(timeout, unit)) {
                    LOG.error("Awaiting Shutdown of Executor '{}' timed out after {} {}", executorName, timeout, unit);
                };
            } catch (InterruptedException e) {
                LOG.warn("Awaiting Shutdown of Executor '{}' was interrupted", executorName);
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void await(final CountDownLatch latch, final String latchName) {
        try {
            final int timeout = 15;
            final TimeUnit unit = TimeUnit.MINUTES;
            if (!latch.await(timeout, unit)) {
                LOG.error("Awaiting Latch '{}' timed out after {} {}", latchName, timeout, unit);
            };
        } catch (InterruptedException e) {
            LOG.warn("Awaiting Latch '{}' was interrupted", latchName);
            Thread.currentThread().interrupt();
        }
    }
}
