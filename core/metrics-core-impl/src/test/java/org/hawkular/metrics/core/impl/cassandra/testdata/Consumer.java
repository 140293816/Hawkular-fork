package org.hawkular.metrics.core.impl.cassandra.testdata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class Consumer {
    public static void GenerateData(int num) throws InterruptedException  {

        final MetricRegistry metricRegistry = new MetricRegistry();
        final Meter meter = metricRegistry.meter("throughput");
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);

        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        for(int i=0; i< num; i++){
            executorService.submit(new RunnableWriter(i, meter));
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        Generator.close();

        reporter.report();
        reporter.stop();
    }

}
