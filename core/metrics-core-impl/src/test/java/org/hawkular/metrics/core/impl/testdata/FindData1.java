package org.hawkular.metrics.core.impl.testdata;

import static org.hawkular.metrics.core.api.MetricType.GAUGE;
import static org.joda.time.DateTime.now;

import java.util.Iterator;

import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.DataAccessImpl1;
import org.hawkular.metrics.core.impl.Order;
import org.joda.time.DateTime;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import rx.Observable;

public class FindData1 {
    private static final Cluster cluster;
    private static final Session session;
    private static final DataAccessImpl1 dataAccess;
    static {
        cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .build();
        session = cluster.connect("hawkularfork");
        dataAccess = new DataAccessImpl1(session);
    }

    public static void main(String[] args) {
        int num = Integer.valueOf(args[0]);
        DateTime date = now().minusWeeks(1);
        Metric<Double> metric = new Metric<>("tenant-1", GAUGE, new MetricId("metric-1"));
        for (int i = 0; i < num; i++) {
            test(dataAccess.findData("tenant-1", new MetricId("metric-1"), GAUGE, date.getMillis(),
                    now().getMillis()));
            test(dataAccess.findData("tenant-1", new MetricId("metric-1"), GAUGE, date.getMillis(),
                    now().getMillis(), true));
            test(dataAccess.findData(metric, date.getMillis(), now().getMillis(), Order.DESC));
            //            test(dataAccess.findData(metric, date.getMillis(), now().getMillis(), Order.ASC),
            //                    metricRegistry.timer("find_data_ASC_" + i));
            date = date.minusWeeks(3);
        }
        session.close();
        cluster.close();
    }

    private static void test(Observable<ResultSet> result) {
        Iterator<Row> it = result.flatMap(Observable::from).toBlocking().toIterable().iterator();
        while (it.hasNext()) {
            it.next();
        }

    }

}