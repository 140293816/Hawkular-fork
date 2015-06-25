package org.hawkular.metrics.core.impl.cassandra.testdata;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class Query {
    private static final Cluster cluster;
    private static final Session session;
    static {
        cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .build();
        session = cluster.connect("report");
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final Meter meter = metricRegistry.meter("throughput");
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
        ResultSetFuture result = session
                .executeAsync("SELECT * FROM data;");
        for (Iterator<Row> i = result.get().iterator(); i.hasNext(); i.next()) {
            meter.mark();
        }
        session.close();
        cluster.close();
        reporter.report();
        reporter.stop();
    }
}
