package org.hawkular.metrics.core.impl.testdata;

import java.util.Iterator;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import rx.Observable;

public class RunnableReader1 implements Runnable{
    private Observable<ResultSet> result;

    public RunnableReader1(Observable<ResultSet> result) {
        this.result = result;
    }

    @Override
    public void run() {
        Iterator<Row> it = result.flatMap(Observable::from).toBlocking().toIterable().iterator();
        while(it.hasNext()){
            it.next();
        }
    }
}
