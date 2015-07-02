package org.hawkular.metrics.core.impl.testdata;


public class RunnableWriter0 implements Runnable {
    private Generator0 generator;

    public RunnableWriter0(int num) {
        generator = new Generator0(num);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}