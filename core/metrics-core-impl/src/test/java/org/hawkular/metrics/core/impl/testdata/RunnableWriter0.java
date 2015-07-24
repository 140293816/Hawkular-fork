package org.hawkular.metrics.core.impl.testdata;

import com.codahale.metrics.Meter;

public class RunnableWriter0 implements Runnable {
    private Generator0 generator;

    public RunnableWriter0(int num, int rowWidth, Meter meter) {
        generator = new Generator0(num, rowWidth, meter);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}