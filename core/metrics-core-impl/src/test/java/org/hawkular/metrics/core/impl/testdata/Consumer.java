package org.hawkular.metrics.core.impl.testdata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Consumer {
    public static void main(String[] args) throws InterruptedException {
        int num = Integer.valueOf(args[0]);
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < num; i++) {
            executorService.submit(new RunnableWriter(i));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        Generator.close();
    }
}