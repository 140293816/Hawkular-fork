package org.hawkular.metrics.core.impl.testdata;

import com.codahale.metrics.Meter;

public class RunnableWriter implements Runnable {
    private Generator generator;

    public RunnableWriter(int num, int rowWidth, Meter meter) {
        generator = new Generator(num, rowWidth, meter);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}