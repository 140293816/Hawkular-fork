package org.hawkular.metrics.core.impl.testdata;

import static org.hawkular.metrics.core.api.MetricType.GAUGE;
import static org.joda.time.DateTime.now;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.DataAccessImpl2;
import org.hawkular.metrics.core.impl.DataAccessImpl3;
import org.hawkular.metrics.core.impl.Functions;
import org.hawkular.metrics.core.impl.Order;
import org.joda.time.DateTime;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import rx.Observable;
import rx.Observer;

public class Solution2 {
    private static final Cluster cluster;
    private static final Session session;
    private static final DataAccessImpl3 dataAccess;
    static {
        cluster = new Cluster.Builder()
        .addContactPoint("127.0.0.1")
        .build();
        session = cluster.connect("hawkularfork");
        dataAccess = new DataAccessImpl3(session);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        //                int num = Integer.valueOf(args[0]);
        int num = 9;
        final MetricRegistry metricRegistry = new MetricRegistry();
        final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(new File("/home/zzhong/workspace/test_results/2"));
        //                        .build(new File("/home/ubuntu/test_results/2"));
        reporter.start(1, TimeUnit.MINUTES);
        DateTime dataPoint = now();

        Metric<Double> metric = new Metric<>("tenant-1", GAUGE, new MetricId("metric-1"));

        test(metric, dataPoint, Order.DESC, metricRegistry.timer("warmup"), num);

        for (int i = 0; i < num; i++) {
            test(metric, dataPoint, Order.DESC,
                    metricRegistry.timer("find_data_DESC_" + i), i);
            test(metric, dataPoint, Order.ASC,
                    metricRegistry.timer("find_data_ASC_" + i), i);

        }
        session.close();
        cluster.close();
        reporter.report();
        reporter.stop();
    }

    private static void test(Metric<Double> metric, DateTime dataPoint, Order order, Timer timer, int bucket)
            throws InterruptedException, ExecutionException
    {

        DateTime start = dataPoint;

        for (int i = 0; i < 100; i++)
        {
            Context context = timer.time();
            ListenableFuture<List<ResultSet>> future = dataAccess
                    .findData(metric, start.minusWeeks(bucket * 3).minusDays(1).getMillis(), start.getMillis(), order);
            for(ResultSet j: future.get()){
                Iterator<Row> it = j.iterator();
                while(it.hasNext()){
                    it.next();
                }
            }
            context.stop();
        }

    }
}
