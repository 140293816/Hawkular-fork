package org.hawkular.metrics.core.impl.testdata;

import static org.hawkular.metrics.core.api.MetricType.GAUGE;
import static org.joda.time.DateTime.now;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.DataAccessImpl;
import org.hawkular.metrics.core.impl.Order;
import org.joda.time.DateTime;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import rx.Observable;

public class Original {
    private static final Cluster cluster;
    private static final Session session;
    private static final DataAccessImpl dataAccess;
    static {
        cluster = new Cluster.Builder()
        .addContactPoint("127.0.0.1")
        .build();
        session = cluster.connect("original");
        dataAccess = new DataAccessImpl(session);
    }

    public static void main(String[] args) {
        int num = Integer.valueOf(args[0]);
        final MetricRegistry metricRegistry = new MetricRegistry();
        final CsvReporter reporter = CsvReporter.forRegistry(metricRegistry)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                .build(new File("/home/zzhong/workspace/test_results/0"));
//                .build(new File("/home/ubuntu/test_results/0"));
        reporter.start(1, TimeUnit.MINUTES);
        DateTime dataPoint = now();
        Metric<Double> metric = new Metric<>("tenant-1", GAUGE, new MetricId("metric-1"));

        test(metric, dataPoint, Order.DESC,
                metricRegistry.timer("warmup"), num);
        
        for (int i = 0; i <num; i++) {
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
    {

        DateTime start = dataPoint;
        for (int i = 0; i < 100; i++)
        {
            Context context = timer.time();
            Iterator<Row> it = dataAccess
                    .findData(metric, start.minusWeeks(bucket * 3).minusDays(1).getMillis(), start.getMillis(), order)
                    .flatMap(Observable::from).toBlocking().toIterable().iterator();
            while (it.hasNext()) {
                it.next();
            }
            context.stop();
        }

    }

}
