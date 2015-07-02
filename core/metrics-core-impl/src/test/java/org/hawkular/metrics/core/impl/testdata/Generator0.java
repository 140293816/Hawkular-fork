package org.hawkular.metrics.core.impl.testdata;

import static org.hawkular.metrics.core.impl.TimeUUIDUtils.getTimeUUID;
import static org.joda.time.DateTime.now;

import java.util.Random;

import org.hawkular.metrics.schema.SchemaManager;
import org.joda.time.DateTime;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class Generator0 {
    private static final Cluster cluster;
    private static final Session session;
    private static final Random ran = new Random();
    private int num;
    static {
        cluster = new Cluster.Builder()
        .addContactPoint("127.0.0.1")
        .build();
        final Session bootstrapSession = cluster.connect();
        SchemaManager report = new SchemaManager(bootstrapSession);
        report.createSchema("original");
        bootstrapSession.close();
        session = cluster.connect("original");
        session.execute("TRUNCATE data");
    }
    private static final PreparedStatement insertPS = session
            .prepare(
                    "INSERT INTO data (tenant_id, type, metric, interval, dpart, time, n_value) VALUES (?, ?, ?, ?, ?, ?, ?)");

    public Generator0(int num) {
        this.num = num;
    }

    public void insertData() {
        DateTime dataPoint;
        dataPoint = now().minusWeeks(3 * num);
        for (int i = 0; i < 1000000; i++) {
            session.executeAsync(new BoundStatement(insertPS).bind("tenant-1", 0, "metric-1", "",
                    0L, getTimeUUID(dataPoint.getMillis()), ran.nextDouble()));
            dataPoint = dataPoint.minusMillis(1);
        }
    }

    public static void close() {
        session.close();
        cluster.close();
    }
}