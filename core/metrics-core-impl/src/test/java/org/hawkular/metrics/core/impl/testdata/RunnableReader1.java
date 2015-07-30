package org.hawkular.metrics.core.impl.testdata;

import java.util.Iterator;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

public class RunnableReader1 implements Runnable {
    private ResultSet result;

    public RunnableReader1(ResultSet result) {
        this.result = result;
    }

    @Override
    public void run() {
        Iterator<Row> it = result.iterator();
        while (it.hasNext()) {
            it.next();
        }
    }
}
