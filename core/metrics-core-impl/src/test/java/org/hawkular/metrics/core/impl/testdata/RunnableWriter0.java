package org.hawkular.metrics.core.impl.testdata;

import com.codahale.metrics.Meter;

public class RunnableWriter0 implements Runnable {
    private Generator0 generator;

    public RunnableWriter0(int num, Meter meter) {
        generator = new Generator0(num, meter);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}