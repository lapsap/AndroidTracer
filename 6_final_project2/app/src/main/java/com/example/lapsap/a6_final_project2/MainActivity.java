package com.example.lapsap.a6_final_project2;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Intent svc;
    public static int OVERLAY_PERMISSION_REQ_CODE = 1;
    boolean bool_trace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // button to chart activity
        Button btn_realtime = (Button) findViewById(R.id.btn_realtime);
        btn_realtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RealtimeLineChartActivity.class);
                startActivity(intent);
            }
        });
        Button btn_starttrace = (Button) findViewById(R.id.btn_starttrace);
        btn_starttrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                starttrace();
                Toast.makeText(MainActivity.this, "Trace start", Toast.LENGTH_LONG).show();
            }
        });
        Button btn_stoptrace = (Button) findViewById(R.id.btn_stoptrace);
        btn_stoptrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bool_trace = false;
                stopService(svc);
                Toast.makeText(MainActivity.this, "Trace stopped", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    // init ftrace and other files used for tracing
    public void inittracer() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("mount -t debugfs nodev /sys/kernel/debug \n");
            os.writeBytes("echo 0 > /sys/kernel/debug/tracing/tracing_on \n");
            os.writeBytes("chmod 666 /sys/kernel/debug/tracing/trace \n");
            os.writeBytes("echo '# bla ' > /data/lapsap/t1 \n");
            os.writeBytes("echo '# bla ' > /data/lapsap/log \n");
            os.writeBytes("echo '0' > /data/lapsap/stat \n");
            os.writeBytes("for i in $(seq 1 24); do echo 0 >> /data/lapsap/stat; done \n");
            os.writeBytes("chmod 666 /data/lapsap/t1 \n");
            os.writeBytes("chmod 666 /data/lapsap/log \n");
            os.writeBytes("chmod 666 /data/lapsap/stat \n");
            os.writeBytes("echo blk > /sys/kernel/debug/tracing/current_tracer \n");
            os.writeBytes("echo '0' > /sys/kernel/debug/tracing/trace \n");
            os.writeBytes("echo 1 > /sys/kernel/debug/tracing/events/block/enable \n");
            os.writeBytes("echo 1 > /sys/kernel/debug/tracing/events/enable \n");
            os.writeBytes(" cat /sys/kernel/debug/tracing/available_events | grep block: > /sys/kernel/debug/tracing/set_event \n");
            os.writeBytes("echo 1 > /sys/kernel/debug/tracing/tracing_on \n");
            os.flush();

        } catch (IOException e) {
            Log.d("lapsap", String.valueOf(e));
        }

    }

    // for higher version of android api, user need to permit to user overlay
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(MainActivity.this, "Permit me", Toast.LENGTH_LONG).show();
            Intent intentSettings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intentSettings, OVERLAY_PERMISSION_REQ_CODE);
            return false;
        }
        return true;
    }

    private void starttrace() {
        inittracer(); // init  ftrace

        // setup overlay
        boolean canoverlay = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canoverlay = checkPermissionOverlay();
        } else
            canoverlay = true;

        if (canoverlay) {
            if (svc == null)
                svc = new Intent(this, OverlayShowingService.class);
            startService(svc);
        }

        bool_trace = true;
        final Handler handler = new Handler();
        final Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            // 勞trace，然後放在t1 之後處理
                            Process p = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(p.getOutputStream());
                            os.writeBytes("cat /sys/kernel/debug/tracing/trace  > /data/lapsap/t1 \n");
                            os.flush();
                            os.writeBytes("echo 0 > /sys/kernel/debug/tracing/trace \n");
                            os.flush();
                            if (!bool_trace)
                                timer.cancel();
                        } catch (IOException e) {
                            Log.d("lapsap", String.valueOf(e));
                        }


                    }
                });
            }
        };
        timer.schedule(task, 0, 1500); //it executes this every 1500ms
    }

}
