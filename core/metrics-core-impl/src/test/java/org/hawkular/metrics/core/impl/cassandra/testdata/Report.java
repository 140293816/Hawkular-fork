package org.hawkular.metrics.core.impl.cassandra.testdata;

import java.util.concurrent.ExecutionException;

public class Report {
    public static void main(String[] args) throws InterruptedException, ExecutionException{
        int num = Integer.valueOf(args[0]);
        Consumer.GenerateData(num);   
        FindData t =new  FindData(num);
        FindData2 t2 =new FindData2(num);
        t.generateReport();
        t2.generateReport();
    }

}
