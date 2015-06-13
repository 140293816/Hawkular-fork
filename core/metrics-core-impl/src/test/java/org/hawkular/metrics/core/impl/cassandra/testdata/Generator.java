package org.hawkular.metrics.core.impl.cassandra.testdata;

import static org.joda.time.DateTime.now;

import java.io.IOException;
import java.util.Random;

import org.hawkular.metrics.core.api.GaugeData;
import org.hawkular.metrics.schema.SchemaManager;
import org.joda.time.DateTime;

import com.codahale.metrics.Meter;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class Generator {
    private Meter meter;
    private static final Cluster cluster;
    private static final Session session;
    private static final Random ran = new Random();
    private static final int RANGE = 50000;
    private static final long TIMESPAN = 1814400000L;
    private int num;

    static {
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

    private static final PreparedStatement insertPS = session
            .prepare(
            "INSERT INTO data (tenant_id, type, metric, interval, dpart, time, n_value) VALUES (?, ?, ?, ?, ?, ?, ?)");

    public Generator(int num, Meter meter) {
        this.num = num;
        this.meter = meter;
    }

    public void insertData() {
        DateTime dataPoint;
        GaugeData data;
        for (int z = 0; z < num; z++) {
            dataPoint = now().minusWeeks(3 * z);
            for (int j = 0; j < 500; j++) {
                final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.UNLOGGED);
                for (int i = 0; i < RANGE; i++) {
                    data = new GaugeData(dataPoint.getMillis(),
                            ran.nextDouble());
                    batchStatement.add(new BoundStatement(insertPS).bind("tenant-1", 0, "metric-1", "",
                            data.getTimestamp() / TIMESPAN, data.getTimeUUID(), data.getValue()));
                    meter.mark();
                    dataPoint = dataPoint.minusMillis(1);
                }
                session.execute(batchStatement);
            }
        }
    }

    public static void close() {
        session.close();
        cluster.close();
    }

}
