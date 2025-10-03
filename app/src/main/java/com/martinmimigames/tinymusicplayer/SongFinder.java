package com.martinmimigames.tinymusicplayer;

import static java.lang.Thread.currentThread;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class SongFinder {
    protected void mmxLog(String msg)
    {
        Log.i("mmx", "servie tid(" + currentThread().getId()+"):"+ currentThread().getName() +" =>"+msg );
    }

    static SongFinder Inst(){
        if (null == singleTon){
            SongFinder newObj = new SongFinder();
            if (newObj.Init())
            {
                singleTon = newObj;
            }
        }
        return singleTon;
    };
    private static SongFinder singleTon = null;

    Uri NextSong(){
        return null;
    }

    Runnable findNextSongRunnable = new Runnable() {
        File song;
        int loop = 0;
        int index = 0;
        String[] files = null;

        File root = new File(Environment.getExternalStorageDirectory().toString());
        @Override
        public void run() {
            loop++;
            mmxLog("loop:"+ loop);
            mmxLog("start find: root "+root.getAbsolutePath() );
            if (files == null)
            {
                files = root.list();
                if (files == null)
                {
                    mmxLog("list file fail");

                }
            }
            mmxLog("files "+files);
            if (files != null) {
                mmxLog("file numbers: " + files.length);
                if (index < files.length) {
                    mmxLog("idx " + index + " : " + files[index].toString());
                    index++;
                    try {
                        Thread.sleep(10000);
                        mmxLog("next round");
                        executor.execute(this);
                    } catch (InterruptedException e) {
                        mmxLog("sleep exception" + e);
                    }
                } else {
                    mmxLog("find finish ");
                }
            }
        }
    };

    private boolean Init(){
        executor = Executors.newSingleThreadExecutor();
        executor.execute(findNextSongRunnable);
        return true;
    };

    Executor executor;
}
