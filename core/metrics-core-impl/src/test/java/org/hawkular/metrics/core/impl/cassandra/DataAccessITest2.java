/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.metrics.core.impl.cassandra;

import static java.util.Arrays.asList;

import static org.joda.time.DateTime.now;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.metrics.core.api.AggregationTemplate;
import org.hawkular.metrics.core.api.Availability;
import org.hawkular.metrics.core.api.AvailabilityData;
import org.hawkular.metrics.core.api.Counter;
import org.hawkular.metrics.core.api.Gauge;
import org.hawkular.metrics.core.api.GaugeData;
import org.hawkular.metrics.core.api.Interval;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.api.MetricType;
import org.hawkular.metrics.core.api.Tenant;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author John Sanda
 */
public class DataAccessITest2 extends MetricsITest {

    private DataAccessImpl dataAccess;
    
    private DataAccessImpl2 dataAccess2;
  
    private PreparedStatement truncateTenants;

    private PreparedStatement truncateGaugeData;

    private PreparedStatement truncateCounters;
    
    private static final long timeSpan = 1814400000L;

    @BeforeClass
    public void initClass() {
        initSession();
        dataAccess = new DataAccessImpl(session);
        dataAccess2 = new DataAccessImpl2(session);
        truncateTenants = session.prepare("TRUNCATE tenants");
        truncateGaugeData = session.prepare("TRUNCATE data");
        truncateCounters = session.prepare("TRUNCATE counters");
    }

    @BeforeMethod
    public void initMethod() {
        session.execute(truncateTenants.bind());
        session.execute(truncateGaugeData.bind());
        session.execute(truncateCounters.bind());
    }

    @Test
    public void insertAndFindTenant() throws Exception {
        Tenant tenant1 = new Tenant().setId("tenant-1")
            .addAggregationTemplate(new AggregationTemplate()
                .setType(MetricType.GAUGE)
                .setInterval(new Interval(5, Interval.Units.MINUTES))
                .setFunctions(ImmutableSet.of("max", "min", "avg")))
            .setRetention(MetricType.GAUGE, Days.days(31).toStandardHours().getHours())
            .setRetention(MetricType.GAUGE, new Interval(5, Interval.Units.MINUTES),
                Days.days(100).toStandardHours().getHours());

        Tenant tenant2 = new Tenant().setId("tenant-2")
            .setRetention(MetricType.GAUGE, Days.days(14).toStandardHours().getHours())
            .addAggregationTemplate(new AggregationTemplate()
                .setType(MetricType.GAUGE)
                .setInterval(new Interval(5, Interval.Units.HOURS))
                .setFunctions(ImmutableSet.of("sum", "count")));


        ResultSetFuture insertFuture = dataAccess.insertTenant(tenant1);
        getUninterruptibly(insertFuture);

        insertFuture = dataAccess.insertTenant(tenant2);
        getUninterruptibly(insertFuture);

        ResultSetFuture queryFuture = dataAccess.findTenant(tenant1.getId());
        ListenableFuture<Tenant> tenantFuture = Functions.getTenant(queryFuture);
        Tenant actual = getUninterruptibly(tenantFuture);
        Tenant expected = tenant1;

        assertEquals(actual, expected, "The tenants do not match");
    }

    @Test
    public void doNotAllowDuplicateTenants() throws Exception {
        getUninterruptibly(dataAccess.insertTenant(new Tenant().setId("tenant-1")));
        ResultSet resultSet = getUninterruptibly(dataAccess.insertTenant(new Tenant().setId("tenant-1")));
        assertFalse(resultSet.wasApplied(), "Tenants should not be overwritten");
    }  
  
    @Test
    public void insertAndFindGaugeRawDataWithOneBucket() throws Exception {
        DateTime start = now().minusMinutes(10);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData("tenant-1", new MetricId("metric-1"), start.getMillis(),
                now().getMillis());
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    @Test
    public void insertAndFindGaugeRawDataWithTwoBuckets() throws Exception {
        DateTime start = now().minusMonths(1);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(2,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData("tenant-1", new MetricId("metric-1"), start.getMillis(),
                now().getMillis());
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    @Test
    public void insertAndFindGaugeRawDataWithThreeBuckets() throws Exception {
        DateTime start = now().minusMonths(2);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(3,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess2.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        List<ResultSetFuture> queryFuture = dataAccess2.findData("tenant-1", new MetricId("metric-1"), start.getMillis(),
                now().getMillis());
        List<GaugeData> actual = new ArrayList<GaugeData>();
        for(ResultSetFuture i: queryFuture){
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(i, Functions.MAP_GAUGE_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }
        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWithOneBucket() throws Exception {
        DateTime start = now().minusMinutes(10);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData(metric, start.getMillis(), now().getMillis(), Order.DESC);
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWithTwoBuckets() throws Exception {
        DateTime start = now().minusMonths(1);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(2,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData(metric, start.getMillis(), now().getMillis(), Order.DESC);
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWithThreeBuckets() throws Exception {
        DateTime start = now().minusMonths(2);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGuageDESC(3,start);
        for(GaugeData i : list){
            metric.addData(i);
        }
       

        getUninterruptibly(dataAccess2.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        List<ResultSetFuture> queryFuture = dataAccess2.findData(metric, start.getMillis(), now().getMillis(), Order.DESC);
        List<GaugeData> actual = new ArrayList<GaugeData>();
        for(ResultSetFuture i: queryFuture){
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(i, Functions.MAP_GAUGE_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }        
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWitOneBucketOrderByASC() throws Exception {
        DateTime start = now().minusMinutes(10);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGaugeASC(1,start);
        for(GaugeData i : list){
            metric.addData(i);
        }

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData(metric, start.getMillis(), now().getMillis(), Order.ASC);
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWithTwoBucketsOrderByASC() throws Exception {
        DateTime start = now().minusMonths(1);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGaugeASC(2,start);
        for(GaugeData i : list){
            metric.addData(i);
        }

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData(metric, start.getMillis(), now().getMillis(), Order.ASC);
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    
    @Test
    public void insertAndFindGaugeRawDataByGaugeWithThreeBucketsOrderByASC() throws Exception {
        DateTime start = now().minusMonths(2);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        
        List<GaugeData> list = generateTestGaugeASC(3,start);
        for(GaugeData i : list){
            metric.addData(i);
        }

        getUninterruptibly(dataAccess2.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        List<ResultSetFuture> queryFuture = dataAccess2.findData(metric, start.getMillis(), now().getMillis(), Order.ASC);
        List<GaugeData> actual = new ArrayList<GaugeData>();
        for(ResultSetFuture i: queryFuture){
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(i, Functions.MAP_GAUGE_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }
        assertEquals(actual, list, "The data does not match the expected values");
    }
    
    
    
    @Test
    public void insertAndFindSingleGaugeData() throws Exception{
        DateTime start = now().minusMinutes(10);
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list){
            metric.addData(i);
        }       

        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));
        
        ResultSetFuture queryFuture = dataAccess.findData(metric, start.plusMinutes(1).getMillis(),false);
        
       List<Row> rows = queryFuture.get().all();
       assertEquals(rows.size(),1,"find more than one record with this timestamp");
       assertEquals(rows.get(0).getString("tenant_id"),"tenant-1","wrong tenant id");
       assertEquals(rows.get(0).getString("metric"),"metric-1","wrong metric id");
       assertEquals(rows.get(0).getDouble("n_value"),3.0,"wrong value");
       assertEquals(UUIDs.unixTimestamp(rows.get(0).getUUID("time")),start.plusMinutes(1).getMillis(),"wrong timestamp");
    }

    @Test
    public void addMetadataToGaugeRawData() throws Exception {
        DateTime start = now().minusMinutes(10);
        DateTime end = start.plusMinutes(6);

        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"),
            ImmutableMap.of("units", "KB", "env", "test"));

        ResultSetFuture insertFuture = dataAccess.addTagsAndDataRetention(metric);
        getUninterruptibly(insertFuture);

        metric.addData(new GaugeData(start.getMillis(), 1.23));
        metric.addData(new GaugeData(start.plusMinutes(2).getMillis(), 1.234));
        metric.addData(new GaugeData(start.plusMinutes(4).getMillis(), 1.234));
        metric.addData(new GaugeData(end.getMillis(), 1.234));
        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));

        ResultSetFuture queryFuture = dataAccess.findData("tenant-1", new MetricId("metric-1"), start.getMillis(),
                end.getMillis());
        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, Functions.MAP_GAUGE_DATA);
        List<GaugeData> actual = getUninterruptibly(dataFuture);
        List<GaugeData> expected = asList(
            new GaugeData(start.plusMinutes(4).getMillis(), 1.234),
            new GaugeData(start.plusMinutes(2).getMillis(), 1.234),
            new GaugeData(start.getMillis(), 1.23)
        );

        assertEquals(actual, expected, "The data does not match the expected values");
    }

//    @Test
//    public void insertAndFindAggregatedGaugeData() throws Exception {
//        DateTime start = now().minusMinutes(10);
//        DateTime end = start.plusMinutes(6);
//
//        Metric metric = new Metric()
//            .setTenantId("tenant-1")
//            .setId(new MetricId("m1", Interval.parse("5min")));
//        List<GaugeData> data = asList(
//
//        );
//
//        GaugeData d1 = new GaugeData()
//            .setTenantId("tenant-1")
//            .setId(new MetricId("m1", Interval.parse("5min")))
//            .setTimestamp(start.getMillis())
//            .addAggregatedValue(new AggregatedValue("sum", 100.1))
//            .addAggregatedValue(new AggregatedValue("max", 51.5, null, null, getTimeUUID(now().minusMinutes(3))));
//
//        GaugeData d2 = new GaugeData()
//            .setTenantId("tenant-1")
//            .setId(new MetricId("m1", Interval.parse("5min")))
//            .setTimestamp(start.plusMinutes(2).getMillis())
//            .addAggregatedValue(new AggregatedValue("sum", 110.1))
//            .addAggregatedValue(new AggregatedValue("max", 54.7, null, null, getTimeUUID(now().minusMinutes(3))));
//
//        GaugeData d3 = new GaugeData()
//            .setTenantId("tenant-1")
//            .setId(new MetricId("m1", Interval.parse("5min")))
//            .setTimestamp(start.plusMinutes(4).getMillis())
//            .setValue(22.2);
//
//        GaugeData d4 = new GaugeData()
//            .setTenantId("tenant-1")
//            .setId(new MetricId("m1", Interval.parse("5min")))
//            .setTimestamp(end.getMillis())
//            .setValue(22.2);
//
//        getUninterruptibly(dataAccess.insertGaugeData(d1));
//        getUninterruptibly(dataAccess.insertGaugeData(d2));
//        getUninterruptibly(dataAccess.insertGaugeData(d3));
//        getUninterruptibly(dataAccess.insertGaugeData(d4));
//
//        ResultSetFuture queryFuture = dataAccess.findGaugeData(d1.getTenantId(), d1.getId(), 0L, start.getMillis(),
//            end.getMillis());
//        ListenableFuture<List<GaugeData>> dataFuture = Futures.transform(queryFuture, new GaugeDataMapper());
//        List<GaugeData> actual = getUninterruptibly(dataFuture);
//        List<GaugeData> expected = asList(d3, d2, d1);
//
//        assertEquals(actual, expected, "The aggregated gauge data does not match");
//    }

    @Test
    public void updateCounterAndFindCounter() throws Exception {
        Counter counter = new Counter("t1", "simple-test", "c1", 1);

        ResultSetFuture future = dataAccess.updateCounter(counter);
        getUninterruptibly(future);

        ResultSetFuture queryFuture = dataAccess.findCounters("t1", "simple-test", asList("c1"));
        List<Counter> actual = getUninterruptibly(Futures.transform(queryFuture, new CountersMapper()));
        List<Counter> expected = asList(counter);

        assertEquals(actual, expected, "The counters do not match");
    }

    @Test
    public void updateCounters() throws Exception {
        String tenantId = "t1";
        String group = "batch-test";
        List<Counter> expected = ImmutableList.of(
            new Counter(tenantId, group, "c1", 1),
            new Counter(tenantId, group, "c2", 2),
            new Counter(tenantId, group, "c3", 3)
        );

        ResultSetFuture future = dataAccess.updateCounters(expected);
        getUninterruptibly(future);

        ResultSetFuture queryFuture = dataAccess.findCounters(tenantId, group);
        List<Counter> actual = getUninterruptibly(Futures.transform(queryFuture, new CountersMapper()));

        assertEquals(actual, expected, "The counters do not match the expected values");
    }

    @Test
    public void findCountersByGroup() throws Exception {
        Counter c1 = new Counter("t1", "group1", "c1", 1);
        Counter c2 = new Counter("t1", "group1", "c2", 2);
        Counter c3 = new Counter("t2", "group2", "c1", 1);
        Counter c4 = new Counter("t2", "group2", "c2", 2);

        ResultSetFuture future = dataAccess.updateCounters(asList(c1, c2, c3, c4));
        getUninterruptibly(future);

        ResultSetFuture queryFuture = dataAccess.findCounters("t1", c1.getGroup());
        List<Counter> actual = getUninterruptibly(Futures.transform(queryFuture, new CountersMapper()));
        List<Counter> expected = asList(c1, c2);

        assertEquals(actual, expected, "The counters do not match the expected values when filtering by group");
    }

    @Test
    public void findCountersByGroupAndName() throws Exception {
        String tenantId = "t1";
        String group = "batch-test";
        Counter c1 = new Counter(tenantId, group, "c1", 1);
        Counter c2 = new Counter(tenantId, group, "c2", 2);
        Counter c3 = new Counter(tenantId, group, "c3", 3);

        ResultSetFuture future = dataAccess.updateCounters(asList(c1, c2, c3));
        getUninterruptibly(future);

        ResultSetFuture queryFuture = dataAccess.findCounters(tenantId, group, asList("c1", "c3"));
        List<Counter> actual = getUninterruptibly(Futures.transform(queryFuture, new CountersMapper()));
        List<Counter> expected = asList(c1, c3);

        assertEquals(actual, expected,
            "The counters do not match the expected values when filtering by group and by counter names");
    }

    @Test
    public void insertAndFindAvailabilitiesInOneBucket() throws Exception {
        DateTime start = now().minusMinutes(10);
        String tenantId = "avail-test";
        Availability metric = new Availability(tenantId, new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(1,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }      
        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findAvailabilityData(tenantId, new MetricId("m1"), start.getMillis(),
                now().getMillis());
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_DATA);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);
       

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test
    public void insertAndFindAvailabilitiesInTwoBuckets() throws Exception {
        DateTime start = now().minusMonths(1);
        String tenantId = "avail-test";
        Availability metric = new Availability(tenantId, new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(2,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }      
        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findAvailabilityData(tenantId, new MetricId("m1"), start.getMillis(),
                now().getMillis());
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_DATA);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);
       

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test
    public void insertAndFindAvailabilitiesInThreeBuckets() throws Exception {
        DateTime start = now().minusMonths(3);
        String tenantId = "avail-test";
        Availability metric = new Availability(tenantId, new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(3,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }      
        getUninterruptibly(dataAccess2.insertData(metric, 360));

        List<ResultSetFuture> future = dataAccess2.findAvailabilityData(tenantId, new MetricId("m1"), start.getMillis(),
                now().getMillis());
        List<AvailabilityData> actual = new ArrayList<AvailabilityData>();
        for(ResultSetFuture i: future){
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(i, Functions.MAP_AVAILABILITY_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    
    @Test
    public void insertAndFindAvailabilitiesWithoutWriteTimeInOneBucket() throws Exception{
        DateTime start = now().minusMinutes(10);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(1,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findData(metric, start.getMillis(),now().getMillis());
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_DATA);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test
    public void insertAndFindAvailabilitiesWithoutWriteTimeInTwoBuckets() throws Exception{
        DateTime start = now().minusMonths(1);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(2,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findData(metric, start.getMillis(),now().getMillis());
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_DATA);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    
    @Test
    public void insertAndFindAvailabilitiesWithoutWriteTimeInThreeBuckets() throws Exception{
        DateTime start = now().minusMonths(2);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityASC(3,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess2.insertData(metric, 360));

        List<ResultSetFuture> future = dataAccess2.findData(metric, start.getMillis(),now().getMillis());
        List<AvailabilityData> actual = new ArrayList<AvailabilityData>();
        for(ResultSetFuture i: future){
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(i, Functions.MAP_AVAILABILITY_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test 
    public void insertAndFindAvailbilitiesWithWriteTimeInOneBucket() throws Exception{
        DateTime start = now().minusMinutes(10);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityDESC(1,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findData(metric, start.getMillis(),now().getMillis(), true);
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_WITH_WRITE_TIME);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test 
    public void insertAndFindAvailbilitiesWithWriteTimeInTwoBuckets() throws Exception{
        DateTime start = now().minusMonths(1);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityDESC(2,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess.insertData(metric, 360));

        ResultSetFuture future = dataAccess.findData(metric, start.getMillis(),now().getMillis(), true);
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(future, Functions.MAP_AVAILABILITY_WITH_WRITE_TIME);
        List<AvailabilityData> actual = getUninterruptibly(dataFuture);

        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test 
    public void insertAndFindAvailbilitiesWithWriteTimeInThreeBuckets() throws Exception{
        DateTime start = now().minusMonths(2);
        
        Availability metric = new Availability("avail-test", new MetricId("m1"));
        List<AvailabilityData> list = generateTestAvailabilityDESC(3,start);
        for(AvailabilityData i: list){
            metric.addData(i);
        }        

        getUninterruptibly(dataAccess2.insertData(metric, 360));

        List<ResultSetFuture> future = dataAccess2.findData(metric, start.getMillis(),now().getMillis(), true);
        List<AvailabilityData> actual = new ArrayList<AvailabilityData>();
        for(ResultSetFuture i: future){
        ListenableFuture<List<AvailabilityData>> dataFuture = Futures
                .transform(i, Functions.MAP_AVAILABILITY_DATA);
        actual.addAll(getUninterruptibly(dataFuture));
        }


        assertEquals(actual, list, "The availability data does not match the expected values");
    }
    
    @Test
    public void findAllGaugeWithOneBucket() throws Exception{
        DateTime start = now().minusMinutes(10);
        
        Gauge metric1 = new Gauge("tenant-1", new MetricId("metric-1"));
        Gauge metric2 = new Gauge("tenant-1", new MetricId("metric-2"));
        
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list){
            metric1.addData(i);
        }    
        
        for(GaugeData i : list){
            metric2.addData(i);
        }
        
        getUninterruptibly(dataAccess.insertData(metric1, MetricsServiceCassandra.DEFAULT_TTL));
        getUninterruptibly(dataAccess.insertData(metric2, MetricsServiceCassandra.DEFAULT_TTL));
        
        ResultSetFuture queryFuture = dataAccess.findAllGuageMetrics();
        
        List<Row> rows = queryFuture.get().all();        
        
        assertEquals(rows.size(),2,"The number of partitioning key does not equal to the expected size, which is 2");
        
        ArrayList<String> metricList = new ArrayList<String>();
        
        for(Row i: rows){
            metricList.add(i.getString("metric"));
        }
        assertTrue(metricList.contains("metric-1"),"metric-1 is missing");
        assertTrue(metricList.contains("metric-2"),"metric-2 is missing");
        
    }
    
    @Test
    public void findGuageMeytricsWithTwoBuckets() throws Exception{
        DateTime start = now().minusMonths(1);
        
        Gauge metric1 = new Gauge("tenant-1", new MetricId("metric-1"));
        Gauge metric2 = new Gauge("tenant-1", new MetricId("metric-2"));
        
        List<GaugeData> list = generateTestGuageDESC(2,start);
        for(GaugeData i : list){
            metric1.addData(i);
        }    
        
        for(GaugeData i : list){
            metric2.addData(i);
        }
        
        getUninterruptibly(dataAccess.insertData(metric1, MetricsServiceCassandra.DEFAULT_TTL));
        getUninterruptibly(dataAccess.insertData(metric2, MetricsServiceCassandra.DEFAULT_TTL));
        
        ResultSetFuture queryFuture = dataAccess.findAllGuageMetrics();
        
        List<Row> rows = queryFuture.get().all();
        
        assertEquals(rows.size(),4,"The number of partitioning key does not equal to the expected size, which is 4");
        
        ArrayList<Long> metric1Dpart = new ArrayList<Long>();
        ArrayList<Long> metric2Dpart = new ArrayList<Long>();
        for(Row i:rows)
        {
            if(i.getString("metric").equals("metric-1")){
                metric1Dpart.add(i.getLong("dpart"));
            }
            else if(i.getString("metric").equals("metric-2")){
                metric2Dpart.add(i.getLong("dpart"));
            }
        }
        
        assertTrue(metric1Dpart.contains(start.getMillis()/timeSpan),"dpart-1 of metric-1 is missing");
        assertTrue(metric1Dpart.contains(start.plusWeeks(3).getMillis()/timeSpan), "dpart-2 of metric-1 is missing");
        assertTrue(metric2Dpart.contains(start.getMillis()/timeSpan), "dpart-1 of metric-2 is missing");
        assertTrue(metric2Dpart.contains(start.plusWeeks(3).getMillis()/timeSpan), "dpart-2 of metric-2 is missing");
    }
        
    
    @Test
    public void deleteGuageMetrics() throws Exception {
        DateTime start = now().minusMinutes(10);  
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));     
        
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list)
        {
            metric.addData(i);            
        }    
        
             
        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));
       
        ResultSetFuture queryFuture = dataAccess.findAllGuageMetrics();
        
        List<Row> rows = queryFuture.get().all();
        assertEquals(rows.size(),1,"wrong metric numbers");
        assertEquals(rows.get(0).getString("metric"),"metric-1","wrong metric id");
        
        dataAccess.deleteGuageMetric("tenant-1","metric-1",metric.getId().getInterval(), start.getMillis()/timeSpan);
        
        
        ResultSetFuture result = dataAccess.findAllGuageMetrics();
        assertEquals(result.get().all().size(),0,"records have not been deleted"); 
    }
    
    @Test 
    public void getMetricTags() throws Exception{
        DateTime start = now().minusMinutes(10);  
        
        Gauge metric = new Gauge("tenant-1", new MetricId("metric-1"));     
        
        List<GaugeData> list = generateTestGuageDESC(1,start);
        for(GaugeData i : list)
        {
            metric.addData(i);            
        }    
        
             
        getUninterruptibly(dataAccess.insertData(metric, MetricsServiceCassandra.DEFAULT_TTL));
        
        metric.setDpart(start.getMillis()/timeSpan);
        
        getUninterruptibly(dataAccess.addTags(metric, ImmutableMap.of("units", "KB", "env", "test")));
        
        ResultSetFuture queryFuture = dataAccess.getMetricTags(metric.getTenantId(), metric.getType(), metric.getId(), metric.getDpart());
        
        List<Row> rows = queryFuture.get().all();
        
        assertEquals(rows.size(),3,"found more than 3 records in this partition");
        for(Row i: rows){
            assertEquals(i.getMap("m_tags", String.class, String.class), ImmutableMap.of("units", "KB", "env", "test"));
        }
        
    }
    
    private List<GaugeData> generateTestGaugeASC(int numBuckets, DateTime time){
        ArrayList<GaugeData> list = new ArrayList<GaugeData>();
        DateTime dataPoint = time;
        
        for(int i=0;i<numBuckets;i++){
            list.add(new GaugeData(dataPoint.getMillis(),i));
            list.add(new GaugeData(dataPoint.plusMinutes(1).getMillis(),i+3));
            list.add(new GaugeData(dataPoint.plusMinutes(2).getMillis(),i+4));
            dataPoint = dataPoint.plusWeeks(3);
        }
        
        return list;
    }
    
    private List<GaugeData> generateTestGuageDESC(int numBuckets, DateTime time){
        ArrayList<GaugeData> list = new ArrayList<GaugeData>();
        DateTime dataPoint = time.plusWeeks(3*(numBuckets-1));
        for(int i=0;i<numBuckets;i++){
            list.add(new GaugeData(dataPoint.plusMinutes(2).getMillis(),i+4));
            list.add(new GaugeData(dataPoint.plusMinutes(1).getMillis(),i+3));
            list.add(new GaugeData(dataPoint.getMillis(),i));
            dataPoint= dataPoint.minusWeeks(3);
        }
        return list;
    }
    
    private List<AvailabilityData> generateTestAvailabilityDESC(int numBuckets, DateTime time){
        ArrayList<AvailabilityData> list = new ArrayList<AvailabilityData>();
        DateTime dataPoint = time.plusWeeks(3*(numBuckets-1));
        for(int i=0;i<numBuckets;i++){
            list.add(new AvailabilityData(dataPoint.plusMinutes(2).getMillis(),"unknown"));
            list.add(new AvailabilityData(dataPoint.plusMinutes(1).getMillis(),"down"));
            list.add(new AvailabilityData(dataPoint.getMillis(),"up"));
            dataPoint= dataPoint.minusWeeks(3);
        }
        return list;
    }
    
    private List<AvailabilityData> generateTestAvailabilityASC(int numBuckets, DateTime time){
        ArrayList<AvailabilityData> list = new ArrayList<AvailabilityData>();
        DateTime dataPoint = time;
        
        for(int i=0;i<numBuckets;i++){
            list.add(new AvailabilityData(dataPoint.getMillis(),"up"));
            list.add(new AvailabilityData(dataPoint.plusMinutes(1).getMillis(),"down"));
            list.add(new AvailabilityData(dataPoint.plusMinutes(2).getMillis(),"unknown"));
            dataPoint = dataPoint.plusWeeks(3);
        }
        
        return list;
    }

}