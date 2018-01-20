package com.example.lapsap.a6_final_project2;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by lapsap on 28/10/2017.
 */

public class RealtimeLineChartActivity extends Activity {
    int[] statdata = new int[26];
    ArrayList<String> arr_flag = new ArrayList<>();
    ArrayList<String> arr_size = new ArrayList<>();
    ArrayList<String> arr_time = new ArrayList<>();
    ArrayList<String> arr_rwbs = new ArrayList<>();
    BarData bardata_iosize;
    BarData bardata_time;
    ScatterData spacialData;

    int iops_max=0, iops_min=99999, iops_avg=0, iops_count=0, latency_count=0;
    float latency_max=0, latency_min=999999, latency_avg=0, latency_sum=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_linechart);
        ListView lv = (ListView) findViewById(R.id.listView1);
        ArrayList<ChartItem> list = new ArrayList<ChartItem>();

        la_read();
        init_flag();

        // https://github.com/PhilJay/MPAndroidChart
        list.add(new PieChartItem(ReadWritePie(), this)); // read write pie
        list.add(new BarChartItem(rwBarData(), getApplicationContext(), arr_rwbs));
        list.add(new PieChartItem(writePie(), this));
        list.add(new PieChartItem(readPie(), this));
        list.add(new BarChartItem(generateFlagData(), getApplicationContext(), arr_flag));
        list.add(new PieChartItem(flagPie(), this));
        list.add(new BarChartItem(bardata_time, getApplicationContext(), arr_time));
        list.add(new BarChartItem(bardata_iosize, getApplicationContext(), arr_size));
        list.add(new ScatterChartitem(spacialData, getApplicationContext()));

        ChartDataAdapter cda = new ChartDataAdapter(getApplicationContext(), list);
        lv.setAdapter(cda);
    }

    private class ChartDataAdapter extends ArrayAdapter<ChartItem> {

        public ChartDataAdapter(Context context, List<ChartItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(position, convertView, getContext());
        }

        @Override
        public int getItemViewType(int position) {
            // return the views type
            return getItem(position).getItemType();
        }

        @Override
        public int getViewTypeCount() {
            return 4; // we have 3 different item-types
        }
    }

    public void init_flag(){
        arr_flag.add("abort");
        arr_flag.add("requeue");
        arr_flag.add("RQcomp");
        arr_flag.add("insert");
        arr_flag.add("issue");
        arr_flag.add("bounce");
        arr_flag.add("BIOcomp");
        arr_flag.add("back");
        arr_flag.add("front");
        arr_flag.add("queue");
        arr_flag.add("getrq");
        arr_flag.add("sleeprq");
        arr_flag.add("plug");
        arr_flag.add("unplug");
        arr_flag.add("split");
        arr_flag.add("bio_remap");
        arr_flag.add("rq_remap");
    }

    public void la_read(){
        try {
            File myFile = new File("/data/lapsap/stat");
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String input;
            for(int i=0; i<25; i++) {
                input = myReader.readLine();
                statdata[i] = Integer.parseInt(input);
            }
            myReader.close();
        } catch (Exception ex) {
            Log.d("lapsap", "realtimechart laread error");
        }

        // self parse
        Map<Integer, Integer> map_size = new HashMap<>();
        Map<String, Float> map_latency = new HashMap<>();
        try {
            File myFile = new File("/data/lapsap/log");
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String input;
            int currentsec=-1, timecount=0;
            float curtime;
            int iops=0;
            ArrayList<Entry> valSpacial = new ArrayList<Entry>(); // spacial scatter data
            int spacialCounter = 0;
            while( (input = myReader.readLine()) != null) {
                if(input.charAt(0) == '#') continue;
                String[] splited = input.split("\\s+");
                if(splited.length < 10 ) continue; // if tracer have bug

                curtime = Float.parseFloat(splited[3].substring(0, splited[3].length() - 1));

                if (splited[4].matches("block_rq_issue:")) {
                    map_latency.put(splited[9], curtime); // latency
                }
                else if( splited[4].matches("block_rq_complete:")){
                    iops++;
                    int range = 0;
                    if ( Integer.parseInt(splited[10]) > 100 && Integer.parseInt(splited[10]) < 200) range = 1;
                    else if ( Integer.parseInt(splited[10]) > 200 && Integer.parseInt(splited[10]) < 400 ) range = 2;
                    else if ( Integer.parseInt(splited[10]) > 400 && Integer.parseInt(splited[10]) < 600 ) range = 3;
                    else if ( Integer.parseInt(splited[10]) > 600 ) range = 4;
                    if (map_size.containsKey(range)) {   // io size
                        map_size.put(range, map_size.get(range) + 1);
                    } else {
                        map_size.put(range, 1);
                    }


                    if (map_latency.containsKey(splited[8])) { //latency
                        if (map_latency.get(splited[8]) != (float) 0) {
                            float latency = curtime - map_latency.get(splited[8]) ;
                            if (latency == 0) continue;
                            latency_min = Math.min(latency_min, latency);
                            latency_max = Math.max(latency_max, latency);
                            latency_sum += latency;
                            latency_count++;
                            map_latency.put(splited[8], (float) 0);
                        }
                    }


                    // spacial graph
                    if(valSpacial.size() < 500) {
                        valSpacial.add(new Entry(spacialCounter + 0.33f, Float.parseFloat(splited[8])));
                        spacialCounter++;
                    }
                }

                if (currentsec == -1) {
                    currentsec = ((int) curtime ) % 10;
                } else if (currentsec != ((int)curtime) % 10) {
                    if (iops == 0) continue;
                    currentsec = ((int) curtime) % 10;
                    timecount++;
                    iops_count += iops;
                    iops_min = Math.min(iops_min, iops);
                    iops_max = Math.max(iops_max, iops);
                    iops = 0;

                }
            }
            // spacial graph
            ScatterDataSet spacialSet = new ScatterDataSet(valSpacial, "Locality");
            spacialSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            spacialSet.setScatterShapeHoleColor(ColorTemplate.COLORFUL_COLORS[3]);
            spacialSet.setScatterShapeHoleRadius(3f);
            spacialSet.setColor(ColorTemplate.COLORFUL_COLORS[1]);
            spacialSet.setScatterShapeSize(8f);
            spacialData = new ScatterData(spacialSet);

            // time
            iops_avg = iops_count / timecount;
            latency_min *= 1000;
            latency_max *= 1000;
            latency_avg = latency_sum / latency_count * 1000;
            ArrayList<BarEntry> bar_time = new ArrayList<BarEntry>();
            BarDataSet barDataSet_time;
            bar_time.add(new BarEntry(0, iops_min) );
            bar_time.add(new BarEntry(1, iops_max) );
            bar_time.add(new BarEntry(2, iops_avg) );
            bar_time.add(new BarEntry(3, latency_min) );
            bar_time.add(new BarEntry(4, latency_max) );
            bar_time.add(new BarEntry(5, latency_avg) );
            barDataSet_time = new BarDataSet(bar_time, "Time ");
            barDataSet_time.setColors(ColorTemplate.VORDIPLOM_COLORS);
            barDataSet_time.setHighLightAlpha(255);
            bardata_time = new BarData(barDataSet_time);
            bardata_time.setBarWidth(0.9f);
            arr_time.add("Iops min");
            arr_time.add("Iops max");
            arr_time.add("Iops avg");
            arr_time.add("IO Latency min(ms)");
            arr_time.add("IO Latency max(ms)");
            arr_time.add("IO Latency avg(ms)");
            // io size
            ArrayList<BarEntry> bar_size = new ArrayList<BarEntry>();
            BarDataSet barDataSet_size;
            arr_size.add("<100");
            arr_size.add("100~200");
            arr_size.add("200~400");
            arr_size.add("400~600");
            arr_size.add(">600");
            for(int i=0; i<5; i++)
                if (map_size.get(i) != null)
                    bar_size.add(new BarEntry(i, map_size.get(i)));
            barDataSet_size = new BarDataSet(bar_size, "IO Size 分佈 (一個rq_complete為單位)");
            barDataSet_size.setColors(ColorTemplate.VORDIPLOM_COLORS);
            barDataSet_size.setHighLightAlpha(255);

            bardata_iosize = new BarData(barDataSet_size);
            bardata_iosize.setBarWidth(0.9f);

        } catch (Exception ex) {

        }
    }

    private BarData generateFlagData() {
        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();
        for (int i=2; i<19; i++){
            entries.add(new BarEntry(i-2, statdata[i]));
        }

        BarDataSet d = new BarDataSet(entries, "Flag 分佈 (一個rq_complete為單位)");
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);
        d.setHighLightAlpha(255);

        BarData cd = new BarData(d);
        cd.setBarWidth(0.9f);
        return cd;
    }

    private BarData rwBarData() {
        // conver from byte to megabytes
        statdata[23] /= 1024;
        statdata[24] /= 1024;

        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();
        entries.add(new BarEntry(0, statdata[0] - statdata[20] )); //read
        entries.add(new BarEntry(1, statdata[1] - statdata[19] )); //write
        entries.add(new BarEntry(2, statdata[19] ));
        entries.add(new BarEntry(3, statdata[20] ));
        entries.add(new BarEntry(4, statdata[23] ));
        entries.add(new BarEntry(5, statdata[24] ));

        arr_rwbs.add("Sync Read");
        arr_rwbs.add("Async Write");
        arr_rwbs.add("Sync Write");
        arr_rwbs.add("Async Read");
        arr_rwbs.add("Total read size(KB)");
        arr_rwbs.add("Total write size(KB)");

        BarDataSet d = new BarDataSet(entries, "rwbs (一個rq_complete為單位)");
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);
        d.setHighLightAlpha(255);

        BarData cd = new BarData(d);
        cd.setBarWidth(0.9f);
        return cd;
    }

    private PieData ReadWritePie() {
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();

        entries.add(new PieEntry(statdata[0], "Read") );
        entries.add(new PieEntry(statdata[1], "Write") );

        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        final int[] MY_COLORS = {Color.rgb(255,0,0), Color.rgb(255,192,0),
                Color.rgb(127,127,127), Color.rgb(146,208,80), Color.rgb(0,176,80), Color.rgb(79,129,189)};
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int c: MY_COLORS) colors.add(c);
        d.setColors(colors);

        PieData cd = new PieData(d);
        return cd;
    }

    private PieData writePie() {
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();

        entries.add(new PieEntry(statdata[19], "Sync Write") );
        entries.add(new PieEntry(statdata[1] - statdata[19], "Async Write") );

        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        //d.setColors(ColorTemplate.COLORFUL_COLORS);
        final int[] MY_COLORS = {Color.rgb(255,0,0), Color.rgb(255,192,0),
                Color.rgb(127,127,127), Color.rgb(146,208,80), Color.rgb(0,176,80), Color.rgb(79,129,189)};
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int c: MY_COLORS) colors.add(c);
        d.setColors(colors);

        PieData cd = new PieData(d);
        return cd;
    }

    private PieData readPie() {
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();

        entries.add(new PieEntry(statdata[20], "Async Read") );
        entries.add(new PieEntry(statdata[0] - statdata[20], "Sync Read") );
        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        final int[] MY_COLORS = {Color.rgb(255,0,0), Color.rgb(255,192,0),
                Color.rgb(127,127,127), Color.rgb(146,208,80), Color.rgb(0,176,80), Color.rgb(79,129,189)};
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int c: MY_COLORS) colors.add(c);
        d.setColors(colors);

        PieData cd = new PieData(d);
        return cd;
    }

    private PieData flagPie() {
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();

        if(statdata[2] >5)entries.add(new PieEntry(statdata[2], "abort") );
        if(statdata[3] >5)entries.add(new PieEntry(statdata[3], "requeue") );
        if(statdata[4] >5)entries.add(new PieEntry(statdata[4], "RQcomplete") );
        if(statdata[5] >5)entries.add(new PieEntry(statdata[5], "insert") );
        if(statdata[6] >5)entries.add(new PieEntry(statdata[6], "issue") );
        if(statdata[7] >5)entries.add(new PieEntry(statdata[7], "bounce") );
        if(statdata[8] >5)entries.add(new PieEntry(statdata[8], "BIOcomplete") );
        if(statdata[9] >5)entries.add(new PieEntry(statdata[9], "backmerge") );
        if(statdata[10] >5)entries.add(new PieEntry(statdata[10], "frontmerge") );
        if(statdata[11] >5)entries.add(new PieEntry(statdata[11], "queue") );
        if(statdata[12] >5)entries.add(new PieEntry(statdata[12], "getrq") );
        if(statdata[13] >5)entries.add(new PieEntry(statdata[13], "sleeprq") );
        if(statdata[14] >5)entries.add(new PieEntry(statdata[14], "plug") );
        if(statdata[15] >5)entries.add(new PieEntry(statdata[15], "unplug") );
        if(statdata[16] >5)entries.add(new PieEntry(statdata[16], "split") );
        if(statdata[17] >5)entries.add(new PieEntry(statdata[17], "BIOremap") );
        if(statdata[18] >5)entries.add(new PieEntry(statdata[18], "RQremap") );


        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        final int[] MY_COLORS = {Color.rgb(255,0,0), Color.rgb(255,192,0),
                Color.rgb(127,127,127), Color.rgb(146,208,80), Color.rgb(0,176,80), Color.rgb(79,129,189)};
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int c: MY_COLORS) colors.add(c);
        d.setColors(colors);

        PieData cd = new PieData(d);
        return cd;
    }
}

