package com.martinmimigames.tinymusicplayer;

import static java.lang.Thread.currentThread;

import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

interface SongFindResult{
    void OnResult(Uri uri);
}

class SongFinder {
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

    private final ReentrantLock lock = new ReentrantLock();

    private SongFindResult songFindResultCallBack;

    void NextSong(SongFindResult cb){
        songFindResultCallBack = cb;
        executor.execute(findNextSongRunnable);
    }

    void Stop()
    {
        executor.shutdownNow();
    }

    Runnable findNextSongRunnable = new Runnable() {
        File song;
        int loop = 0;
        int index = 0;
        File[] files = null;

//        File root = new File(Environment.getExternalStorageDirectory().toString());
        File root = new File("/storage/emulated/0/kkk");
        @Override
        public void run() {
            loop++;
            mmxLog("loop:"+ loop);
            mmxLog("start find: root "+root.getAbsolutePath() );
            if (files == null)
            {
                files = root.listFiles();
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
                    if (songFindResultCallBack != null)
                    {
                        songFindResultCallBack.OnResult(Uri.parse(files[index].toURI().toString()));
                    }
                    index++;
                }
                else {
                    mmxLog("find finish ");
                }
//                    try {
//                        Thread.sleep(10000);
//                        mmxLog("next round");
//                        executor.execute(this);
//                    } catch (InterruptedException e) {
//                        mmxLog("sleep exception" + e);
//                    }
            }
        }
    };

    private boolean Init(){
//        executor = Executors.newSingleThreadExecutor();
//        executor.execute(findNextSongRunnable);
        return true;
    };

    ExecutorService executor = Executors.newSingleThreadExecutor();
}
