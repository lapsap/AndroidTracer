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
#include <iostream>

using namespace std;


template <typename T>
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}

extern "C"

JNIEXPORT jstring JNICALL Java_com_example_lapsap_a6_1final_1project2_OverlayShowingService_overlaydata(JNIEnv *env, jobject Obj) {

    string pid, cpu, ttime, event, data;
    int write=0, read=0, sync=0, async=0, flush=0, force=0;
    string tmp, output="";

    // write stats for graph viewing
    ifstream stat("/data/lapsap/stat");
    int statWrite, statRead, statSync, statAsync, statFlush, statForce;
    int rq_abort, rq_requeue, rq_complete, rq_insert, rq_issue, bio_bounce, bio_complete, bio_backmerge, bio_frontmerge, bio_queue;
    int getrq, sleeprq, plug, unplug, split, bio_remap, rq_remap;
    int sizeRead, sizeWrite;
    stat >> statRead >> statWrite;
    stat >> rq_abort >> rq_requeue>> rq_complete>> rq_insert>> rq_issue>> bio_bounce>> bio_complete>> bio_backmerge>> bio_frontmerge>> bio_queue;
    stat >> getrq>> sleeprq>> plug>> unplug>> split>> bio_remap>> rq_remap >> statSync >> statAsync >> statFlush >> statForce >> sizeRead >> sizeWrite;
    stat.close();
    //

    ifstream fin("/data/lapsap/t1");
    ofstream fout("/data/lapsap/log", ofstream::app);
    while(getline(fin,tmp)){
        if (tmp[0] == '#') continue;
        stringstream ss(tmp);
        ss >> pid >> cpu >> ttime >> event;
        // so we dont overflow log file with events we aren't intrested in
        if( event == "block_rq_issue:" || event == "block_rq_complete:" )
            output += tmp + "\n";
        data = "";
        while (ss >> tmp)
            data += tmp + " ";
        if( event == "block_rq_complete:"){
            stringstream ds(data);
            string rwbs, sector;
            int nr_sector;
            ds >> tmp >> rwbs >> tmp >> sector >> tmp >> nr_sector;

            if (rwbs[0] == 'F' ){
                flush++;
                write++;
                sizeWrite += nr_sector;
                if (rwbs.size() > 2 ){
                    if(rwbs[2] == 'F') force++;
                    else if(rwbs[2] == 'S') sync++;
                }
                if (rwbs.size() >3) {
                    if (rwbs[3] == 'S') sync++;
                }
            } else if (rwbs[0] == 'W'){
                write++;
                sizeWrite += nr_sector;
                if(rwbs.size() > 1 ){
                    if(rwbs[1] == 'F') force++;
                    else if(rwbs[1] == 'S') sync++;
                }
            } else if (rwbs[0] == 'R') {
                read++;
                sizeRead += nr_sector;
                if(rwbs.size() > 1 ){
                    if(rwbs[1] == 'A') async++;
                }
            }
            rq_complete++;
        }
        else if (event == "block_rq_abort:") rq_abort++;
        else if (event == "block_rq_requeue:") rq_requeue++;
        else if (event == "block_rq_insert:") rq_issue++;
        else if (event == "block_rq_insert:") rq_insert++;
        else if (event == "block_bio_bounce:") bio_bounce++;
        else if (event == "block_bio_complete:") bio_complete++;
        else if (event == "block_bio_backmerge:") bio_backmerge++;
        else if (event == "block_bio_frontmerge:") bio_frontmerge++;
        else if (event == "block_bio_queue:") bio_queue++;
        else if (event == "block_getrq:") getrq++;
        else if (event == "block_sleeprq:") sleeprq++;
        else if (event == "block_plug:") plug++;
        else if (event == "block_unplug:") unplug++;
        else if (event == "block_split:") split++;
        else if (event == "block_bio_remap:") bio_remap++;
        else if (event == "block_rq_remap") rq_remap++;

    }
    fout << output;
    fin.close();
    fout.close();
    string res = "test";
    res = "W: " + to_string(write) + " R: " + to_string(read) + " WS: " + to_string(sync)  + " RA: " + to_string(async) + "\n";

    // stat
    statWrite += write ;
    statRead += read ;
    statSync += sync;
    statAsync += async;
    statFlush += flush;
    statForce += force;
    ofstream sout("/data/lapsap/stat");
    sout << statRead << endl  << statWrite << endl ;
    sout << rq_abort<< endl  << rq_requeue<< endl << rq_complete<< endl << rq_insert<< endl << rq_issue<< endl ;
    sout << bio_bounce<< endl << bio_complete<< endl << bio_backmerge<< endl << bio_frontmerge<< endl << bio_queue<< endl ;
    sout << getrq<< endl << sleeprq<< endl << plug<< endl << unplug<< endl << split<< endl << bio_remap<< endl << rq_remap << endl;
    sout << statSync << endl << statAsync << endl << statFlush << endl << statForce << endl;
    sout << sizeRead << endl << sizeWrite << endl;
    sout.close();

    return env->NewStringUTF(res.c_str());
}