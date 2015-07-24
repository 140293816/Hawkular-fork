package org.hawkular.metrics.core.impl.testdata;

import static org.hawkular.metrics.core.api.MetricType.GAUGE;
import static org.joda.time.DateTime.now;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.DataAccessImpl2;
import org.hawkular.metrics.core.impl.Order;
import org.joda.time.DateTime;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import rx.Observable;

public class Query {
    private static final Cluster cluster;
    private static final Session session;
    static {
        cluster = new Cluster.Builder()
        .addContactPoint("127.0.0.1")
        .build();
        session = cluster.connect("hawkularfork");
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
//        for(int i=0; i<100;i++){
        ResultSet r = session.execute("SELECT * FROM data");
        Timer timer = metricRegistry.timer("test");
        Iterator<Row> it = r.iterator();
        Context context = timer.time();
        while(it.hasNext()){
            context.stop();
            it.next();
        }
//        }
       
//        for(int i=0; i<100; i++){
//             test(timer);
//        }
        session.close();
        cluster.close();
        reporter.report();
        reporter.stop();
    }
    
    private static void test(Timer timer) throws InterruptedException, ExecutionException{
        
        final ExecutorService executorService = Executors.newWorkStealingPool();
        for(int i= 784; i<792;i++){
        executorService.submit(new RunnableReader0(session.executeAsync(
                "SELECT *  FROM data WHERE tenant_id = 'tenant-1' AND type = 0 AND interval = '' AND dpart = "+i+" AND metric = 'metric-1'"),
                timer));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
    }


}
