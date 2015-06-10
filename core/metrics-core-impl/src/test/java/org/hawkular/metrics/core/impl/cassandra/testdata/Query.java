package org.hawkular.metrics.core.impl.cassandra.testdata;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
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

    public static void main(String[] args) {
        ResultSet result = session.execute("SELECT COUNT(*) FROM data");
        System.out.println(result.one().getLong("count"));
        session.close();
        cluster.close();
    }

}
