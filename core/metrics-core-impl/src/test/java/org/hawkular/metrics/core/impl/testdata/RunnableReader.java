package org.hawkular.metrics.core.impl.testdata;

import java.util.Iterator;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import rx.Observable;

public class RunnableReader implements Runnable{
    private Observable<ResultSet> result;
    private Timer timer;

    public RunnableReader(Observable<ResultSet> result, Timer timer) {
        this.result = result;
        this.timer = timer;
    }

    @Override
    public void run() {
        Context context = timer.time();
        Iterator<Row> it = result.flatMap(Observable::from).toBlocking().toIterable().iterator();
        while(it.hasNext()){
            it.next();
        }
        context.stop();
    }
}
