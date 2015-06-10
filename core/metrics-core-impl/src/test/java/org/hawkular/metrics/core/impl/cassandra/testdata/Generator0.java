package org.hawkular.metrics.core.impl.cassandra.testdata;

import static org.joda.time.DateTime.now;

import java.io.IOException;
import java.util.Random;

import org.hawkular.metrics.core.api.GaugeData;
import org.hawkular.metrics.schema.SchemaManager;
import org.joda.time.DateTime;

import com.codahale.metrics.Meter;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class Generator0 {
    private Meter meter;
    private  final Cluster cluster;
    private  final Session session;
    private  final Random ran = new Random();
    private  final long TIMESPAN = 1814400000L;
    private int num;

     {
        cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .build();

        final Session bootstrapSession = cluster.connect();
        SchemaManager report = new SchemaManager(bootstrapSession);
        try {
            report.createSchema("report");
        } catch (IOException e) {
            e.printStackTrace();
        }
        bootstrapSession.close();

        session = cluster.connect("report");

        session.execute("TRUNCATE data");
    }

    private  final PreparedStatement insertPS = session
            .prepare(
                    "INSERT INTO data (tenant_id, type, metric, interval, dpart, time, n_value) VALUES (?, ?, ?, ?, ?, ?, ?)");

    public Generator0(int num, Meter meter) {
        this.num = num;
        this.meter = meter;
    }

    public void insertData() {
        DateTime dataPoint;
        GaugeData data;
            dataPoint = now().minusWeeks(3 * num);
                for (int i = 0; i < 1000000; i++) {
                    data = new GaugeData(dataPoint.getMillis(),
                            ran.nextDouble());
                    session.executeAsync(new BoundStatement(insertPS).bind("tenant-1", 0, "metric-1", " ",
                            data.getTimestamp() / TIMESPAN, data.getTimeUUID(), data.getValue()));
                    meter.mark();
                    dataPoint = dataPoint.minusMillis(1);
                }
        }

    public  void close() {
        session.close();
        cluster.close();
    }

}
