package com.martinmimigames.tinymusicplayer;

import static java.lang.Thread.currentThread;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

/**
 * service for playing music
 */
public class Service extends android.app.Service {

  protected void mmxLog(String msg)
  {
    Log.i("mmx", "servie tid(" + currentThread().getId()+"):"+ currentThread().getName() +" =>"+msg );
  }

  final HWListener hwListener;
  final Notifications notifications;
  /**
   * audio playing logic class
   */
  private AudioPlayer audioPlayer = null;

  private Boolean isWaitingForNextSong = new Boolean(false);

  SongFinder songFinder;

  public Service() {
    hwListener = new HWListener(this);
    notifications = new Notifications(this);
    mmxLog("onConstruct: service "+this.toString());
    songFinder = SongFinder.Inst();
  }

  @Override
  protected void finalize() throws Throwable {
    mmxLog("onDelete: service "+this.toString());
    SongFinder.Inst().Stop();
    super.finalize();
  }


  /**
   * unused
   */
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /**
   * setup
   */
  @Override
  public void onCreate() {
    hwListener.create();
    notifications.create();
    mmxLog("onCreate: service "+this.toString() + " notifi: "+notifications.toString());

    super.onCreate();
  }

  /**
   * startup logic
   */
  @SuppressWarnings("deprecation") //for old version
  @Override
  public void onStart(Intent intent, int startId )
  {
    mmxLog("onStart: service "+this.toString() + " notifi: "+notifications.toString());
    if (intent == null){
      mmxLog("service start with null intent, just play next song ");
      TryNextSong();
    }else if (intent.getAction() == null) {/* check if called from self */
      var isPLaying = audioPlayer.isPlaying();
      var isLooping = audioPlayer.isLooping();
      switch (intent.getByteExtra(Launcher.TYPE, Launcher.NULL)) {
        /* start or pause audio playback */
        case Launcher.PLAY_PAUSE -> setState(!isPLaying, isLooping);
        case Launcher.PLAY -> setState(true, isLooping);
        case Launcher.PAUSE -> setState(false, isLooping);
        case Launcher.LOOP -> setState(isPLaying, !isLooping);
        /* cancel audio playback and kill service */
        case Launcher.KILL -> stopSelf();
        case Launcher.NEXT -> TryNextSong();
        case Launcher.PREVIOUS -> {}
      }
    } else {
      switch (intent.getAction()) {
        case Intent.ACTION_VIEW -> setAudio(intent.getData());
        case Intent.ACTION_SEND -> setAudio(intent.getParcelableExtra(Intent.EXTRA_STREAM));
        default -> {
          mmxLog("unknown action: " + intent.getAction());
          TryNextSong();
        }
      }
    }
  }

  void setAudio(final Uri audioLocation) {
    boolean result=false;
    try {
      /* get audio playback logic and start async */
      if (audioPlayer == null) {
        audioPlayer = new AudioPlayer(this);
      }

      //noinspection ConstantConditions
      if (audioPlayer != null) {
        audioPlayer.Play(audioLocation);
      }

      /* create notification for playback control */
      notifications.getNotification(audioLocation);

      /* start service as foreground */
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR)
        startForeground(Notifications.NOTIFICATION_ID, notifications.notification);

      result = true;
      mmxLog("start play");
    } catch (IllegalArgumentException e) {
      mmxLog("exception:"+e);
      Exceptions.throwError(this, Exceptions.IllegalArgument);
    } catch (SecurityException e) {
      mmxLog("exception:"+e);
      Exceptions.throwError(this, Exceptions.Security);
    } catch (IllegalStateException e) {
      mmxLog("exception:"+e);
      Exceptions.throwError(this, Exceptions.IllegalState);
    } catch (IOException e) {
      mmxLog("exception:"+e);
      Exceptions.throwError(this, Exceptions.IO);
    }
    finally {
      if (!result)
      {
        TryNextSong();
      }
    }
  }

  void TryNextSong()
  {
    Context context = this;

    synchronized (isWaitingForNextSong)
    {
      if (isWaitingForNextSong)
      {
        return;
      }
      isWaitingForNextSong = true;
    }
    SongFinder.Inst().NextSong(new SongFindResult() {
      @Override
      public void OnResult(Uri uri) {
        mmxLog("Find a song: "+uri);
        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setClass(context, Service.class);
//        stopService(intent);

        synchronized (isWaitingForNextSong)
        {
          isWaitingForNextSong = false;
        }

        startService(intent);
      }
    });
  }

  /**
   * Switch to player component state
   */
  void setState(boolean playing, boolean looping) {
    try {
      audioPlayer.setState(playing, looping);
      hwListener.setState(playing, looping);
      notifications.setState(playing, looping);
    }
    catch (IllegalStateException e)
    {
      mmxLog("error during set state:"+ e.toString());
      TryNextSong();
    }
  }

  /**
   * forward to startup logic for newer androids
   */
  @TargetApi(Build.VERSION_CODES.ECLAIR)
  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    onStart(intent, startId);
    return START_STICKY;
  }

  /**
   * service killing logic
   */
  @Override
  public void onDestroy() {
    mmxLog("onDestroy: service "+this.toString() + " notifi: "+notifications.toString());
    notifications.destroy();
    hwListener.destroy();
    /* interrupt audio playback logic */

    if (audioPlayer!=null) {
      audioPlayer.Release();
      audioPlayer = null;
    }

    super.onDestroy();
  }
}
