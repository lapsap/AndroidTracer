#include <jni.h>
#include <string>
#include <iostream>
#include <unistd.h>
#include <fcntl.h>
#include <map>
#include <stdlib.h>
#include <stdio.h>
#include <sstream>
#include <fstream>

using namespace std;


extern "C"
JNIEXPORT void JNICALL Java_com_example_lapsap_a6_1final_1project2_RealtimeLineChartActivity_cpptracer(JNIEnv *env, jobject Obj) {
    jclass cls = env->GetObjectClass(Obj);  // instead of FindClass
    jmethodID method_addentry = env->GetMethodID(cls, "addEntry", "(IF)V");


    // variables for parsing
    map<string, float> map_rq_latency;
    string pid, cpu, ttime, event, data;
    float curtime, current_sec;
    float sum_elapse = 0.0, avg_latency = 0.0, sum_iosize = 0.0, avg_iosize = 0.0;
    int num_elapse = 0;
    string tmp;


    ifstream fin("/data/lapsap/log");            // read tmp file
    int count = 0;
    while (getline(fin, tmp)) {
        if (tmp[0] == '#') continue;
        count++;
        stringstream ss(tmp);
        ss >> pid >> cpu >> ttime >> event;
        string ttmp(ttime.begin(), ttime.end() - 1);
        curtime = atof(ttmp.c_str());
        data = "";
        while (ss >> tmp)
            data += tmp + " ";
        if (event == "block_rq_issue:") {
            stringstream ds(data);
            ds >> tmp >> tmp >> tmp >> tmp >> tmp;
            map_rq_latency[tmp] = curtime;  //sector as key, time as value
        } else if (event == "block_rq_complete:") {
            stringstream ds(data);
            string rwbs, sector;
            int nr_sector;
            ds >> tmp >> rwbs >> tmp >> sector >> tmp >> nr_sector;
            float t1 = map_rq_latency[sector]; //time of rq_issue
            if (t1 != 0) { // calculate hardware latency
                float elapse = curtime - t1;
                if (elapse > 0) {
                    sum_iosize += nr_sector;
                    sum_elapse += elapse;
                    num_elapse++;
                }
                map_rq_latency[sector] = 0;
            }

        }

        if( current_sec != (int)curtime%10 ){

            if (num_elapse == 0) continue ;
            avg_latency = sum_elapse / num_elapse * 1000;
            avg_iosize = sum_iosize / num_elapse;
            env->CallVoidMethod(Obj, method_addentry, 0, (float) avg_latency);
            env->CallVoidMethod(Obj, method_addentry, 1, (float) avg_iosize );
            env->CallVoidMethod(Obj, method_addentry, 2, (float) curtime);
            current_sec = (int)curtime%10;
            sleep(1);
        }

    }
    fin.close();


}

