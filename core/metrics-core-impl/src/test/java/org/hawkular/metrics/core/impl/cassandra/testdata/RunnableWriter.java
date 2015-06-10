package org.hawkular.metrics.core.impl.cassandra.testdata;

import com.codahale.metrics.Meter;

public class RunnableWriter implements Runnable {
    private Generator0 generator;

    public RunnableWriter(int num, Meter meter) {
        generator = new Generator0(num, meter);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}