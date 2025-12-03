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
        com.mmx.tool.SongFinder finder = null;
        int loop = 0;

        @Override
        public void run() {
            loop++;
            if (finder == null)
            {
                finder = new com.mmx.tool.SongFinder();
                finder.Init();
            }
            finder.GetNext(
                    new com.mmx.tool.SongFinder.NextSongCallBack() {
                        @Override
                        public void OnNextSong(File song) {
                            if (songFindResultCallBack != null)
                            {
                                songFindResultCallBack.OnResult(Uri.parse(song.toURI().toString()));
                            }
                        }
                    }
            );
            finder.NextSong();

            // set minimum delay between songs to avoid CPU burn out
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
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
