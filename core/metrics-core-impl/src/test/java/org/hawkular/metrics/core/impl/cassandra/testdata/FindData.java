package org.hawkular.metrics.core.impl.cassandra.testdata;

import static org.joda.time.DateTime.now;

import java.util.concurrent.TimeUnit;

import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.impl.cassandra.DataAccessImpl;
import org.joda.time.DateTime;

import org.apache.cassandra.service.StorageService;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class FindData {
    private static final Cluster cluster;
    private static final Session session;
    private static DataAccessImpl dataAccess;


    static {
        cluster = new Cluster.Builder()
        .addContactPoint("127.0.0.1")
        .build();
        session = cluster.connect("report");
        dataAccess = new DataAccessImpl(session);
    }
    
    public static void main(String[] args){       
        
        DateTime start = now().minusYears(1);
        dataAccess.findData("tenant-1", new MetricId("metric-1"), start.getMillis(),
                now().getMillis());
                       
        
        session.close();
        cluster.close();
        
    }


}
