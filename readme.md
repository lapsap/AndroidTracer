Android Tracer Setup

# Introduction
This is an app that trace block layer information for a period of time. While using this app, we can also see real time read/write through an overlay button on the top.

As this app reads block layer information, we need to use a custom kernel which block layer tracing enabled, and root privilege.

This post will explain how to use this app. 

# Building Kernel
This is a simple [blog](https://chris0417.blogspot.tw/2017/03/building-android-kernel.html) post of one of our member explaining step by step on how to build the kernel.

* For nexus 7, i used `make -j8 tegra3_android_defconfig`

# Fast Boot Custom Kernel
After building our kernel, the next step is to produce a bootable image for out android device. This [blog](https://chris0417.blogspot.tw/2017/04/fastboot-custom-kernel.html) post is a step by step explanation.

# Using The App
After booting with the custom kernel and having root access, we can now use our app. 

To install the app, just open the Android Studio project. On the first boot, gradle will help with installing some prerequired packages.

After gradle is done installing and initializing, just click the 'Run app' button.

# How app code's work

## Intro
Some logging files as store in /data/lapsap of the android device. So make sure the diretory exists.

## MainActivity.java

### inittracer()
this will initialize ftrace, set our filter of ftrace to block layer logs and initialize our log files.

### starttracer()
a timertask will invoke a function every 1500ms. in this function, we get trace log from ftrace(debugfs) and output it to our log file for futher parsing.

### Overlay test
In newer version of android, overlay buttons need to ask for permission before we can use them, this code will test the android version, and if it's a newer version without permission, we will ask for permission.

## OverlayShowingService.java
This is the code where we create our overlay button. Every 1000ms, the button will invoke a function overlaydata(), which is a native function. The purpose of this function is to parse the log data that we previously obtain from ftrace.

The function parse all logs, and only keep some stats and 2 events(block_rq_issue & block_rq_complete).
The stats are stored in `/data/lapsap/stat` and those 2 event's logs are stoed in `/data/lapsap/log`.

Inside stats log, each stats take 1 line. And the order is as bellow:
```
statRead
statWrite
rq_abort
rq_requeue
rq_complete
rq_insert
rq_issue
bio_bounce
bio_complete
bio_backmerge
bio_frontmerge
bio_queue
getrq
sleeprq
plug
unplug
split
bio_remap
rq_remap
statSync
statAsync
statFlush
statForce
sizeRead 
sizeWrite
```

### Future work
The overlay button can also be moved and click, currently there are no function when clicking. In the future, we may add a start/stop function to it.

## Graphs

### Intro
https://github.com/PhilJay/MPAndroidChart , we are using this library for our grpahs.

RealtimeLineChartActivity.java is the java that controls, parse and call our graphs.

{CharItem, BarChartItem, LineChartItem, PieChartItem, ScatterChartItem}.java are the classes needed for that library.

MyValueFormartter.java is used when we want to change to display formatting in graphs. Currently, it is not applied.