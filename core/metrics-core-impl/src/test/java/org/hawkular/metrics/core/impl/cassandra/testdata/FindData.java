package org.hawkular.metrics.core.impl.cassandra.testdata;

import static org.joda.time.DateTime.now;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.hawkular.metrics.core.api.Gauge;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.cassandra.DataAccessImpl;
import org.hawkular.metrics.core.impl.cassandra.Order;
import org.joda.time.DateTime;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class FindData {
    private static final Cluster cluster;
    private static DataAccessImpl dataAccess;
    private static final Session session;

    static {
        cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .build();
        session = cluster.connect("report");
        dataAccess = new DataAccessImpl(session);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int num = Integer.valueOf(args[0]);
        final MetricRegistry metricRegistry = new MetricRegistry();

        final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(new File("/home/zzhong/workspace/test_results/1"));
        reporter.start(1, TimeUnit.MINUTES);

        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        DateTime date = now().minusDays(1);

        for (int i = 0; i < num; i++) {
            test(dataAccess.findData("tenant-1", new MetricId("metric-1"), date.getMillis(),
                    now().getMillis()), metricRegistry.timer("find_data_without_writetime_" + i));

            test(dataAccess.findData("tenant-1", new MetricId("metric-1"), date.getMillis(),
                    now().getMillis(), true), metricRegistry.timer("find_data_with_writetime_" + i));

            test(dataAccess.findData(metric, date.getMillis(), now().getMillis(), Order.DESC),
                    metricRegistry.timer("find_data_DESC_" + i));

            test(dataAccess.findData(metric, date.getMillis(), now().getMillis(), Order.ASC),
                    metricRegistry.timer("find_data_ASC_" + i));

            date = date.minusWeeks(3);
        }

        session.close();
        cluster.close();

        reporter.report();
        reporter.stop();
    }

    private static void test(ResultSetFuture result, Timer timer) throws InterruptedException, ExecutionException {
        Context context = timer.time();
        for (Iterator<Row> i = result.get().iterator(); i.hasNext(); i.next()) {
            context.stop();
        }
    }

}
