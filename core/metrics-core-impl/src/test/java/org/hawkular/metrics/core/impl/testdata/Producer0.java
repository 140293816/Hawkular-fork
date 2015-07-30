package org.hawkular.metrics.core.impl.testdata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class Producer0 {
    public static void main(String[] args) throws InterruptedException {
      int num = Integer.valueOf(args[0]);
      int rowWidth = Integer.valueOf(args[1]);
        final MetricRegistry metricRegistry = new MetricRegistry();
        final Meter meter = metricRegistry.meter("throughput");
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
        final ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int i = 0; i < num; i++) {
            executorService.submit(new RunnableWriter0(i, rowWidth, meter));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        Generator0.close();
        reporter.report();
        reporter.stop();
    }
}