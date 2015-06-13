package org.hawkular.metrics.core.impl.cassandra.testdata;

import com.codahale.metrics.Meter;

public class RunnableWriter implements Runnable {
    private Generator generator;

    public RunnableWriter(int num, Meter meter) {
        generator = new Generator(num, meter);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}