package org.hawkular.metrics.core.impl.testdata;

import java.util.Iterator;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;

public class RunnableReader0 implements Runnable{
    private ResultSetFuture result;
    private Timer timer;

    public RunnableReader0(ResultSetFuture result, Timer timer) {
        this.result = result;
        this.timer = timer;
    }

    @Override
    public void run() {
        Context context = timer.time();
        Iterator<Row> it = result.getUninterruptibly().iterator();
        while(it.hasNext()){
            it.next();
        }
        context.stop();
    }
}
