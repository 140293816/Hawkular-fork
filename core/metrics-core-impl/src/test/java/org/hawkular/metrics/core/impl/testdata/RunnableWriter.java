package org.hawkular.metrics.core.impl.testdata;


public class RunnableWriter implements Runnable {
    private Generator generator;

    public RunnableWriter(int num) {
        generator = new Generator(num);
    }

    @Override
    public void run() {
        generator.insertData();
    }
}