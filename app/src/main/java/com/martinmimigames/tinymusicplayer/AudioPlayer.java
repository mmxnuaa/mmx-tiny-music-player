package com.martinmimigames.tinymusicplayer;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

class AudioPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

  protected void mmxLog(String msg)
  {
    Log.i("mmx", "audioplayer tid(" + Thread.currentThread().getId()+"):"+ Thread.currentThread().getName() +" =>"+msg );
  }

  private final Service service;
  private MediaPlayer mediaPlayer;

  /**
   * Initiate an audio player, throws exceptions if failed.
   *
   * @param service       the service initialising this.
   */
  public AudioPlayer(Service service)  {
    mmxLog("thread construct : "+this.toString());

    this.service = service;
    InitMediaPlayer();

    mmxLog("media play init");
  }

  private void InitMediaPlayer()
  {
    if (mediaPlayer != null)
    {
      // release old one
      mediaPlayer.release();
    }
    /* initiate new audio player */
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setOnCompletionListener(this);
    mediaPlayer.setOnErrorListener(this);
    mediaPlayer.setOnPreparedListener(this);
  }

  /**
   * play song, throws exceptions if failed.
   *
   * @param audioLocation the Uri containing the location of the audio.
   * @throws IllegalArgumentException when the media player need cookies, but we do not supply it.
   * @throws IllegalStateException    when the media player is not in the correct state.
   * @throws SecurityException        when the audio file is protected and cannot be played.
   * @throws IOException              when the audio file cannot be read.
   */
  public void Play(Uri audioLocation) throws IllegalArgumentException, IllegalStateException, SecurityException, IOException
  {
    if (mediaPlayer == null)
    {
      InitMediaPlayer();
    }

    mediaPlayer.reset();
    /* setup player variables */
    mediaPlayer.setDataSource(service, audioLocation);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    } else {
      mediaPlayer.setAudioAttributes(
              new AudioAttributes.Builder()
                      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                      .setUsage(AudioAttributes.USAGE_MEDIA)
                      .build()
      );
    }

    mediaPlayer.setLooping(false);

    mediaPlayer.prepareAsync();
  }

  /**
   * check if audio is playing
   */
  public boolean isPlaying() {
    return mediaPlayer.isPlaying();
  }

  /**
   * check if audio is looping, always false on < android cupcake (sdk 3)
   */
  public boolean isLooping() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
      return mediaPlayer.isLooping();
    } else {
      return false;
    }
  }

  /**
   * set player state
   *
   * @param playing is audio playing
   * @param looping is audio looping
   */
  void setState(boolean playing, boolean looping) {
    if (playing) {
      mediaPlayer.start();
    } else {
      mediaPlayer.pause();
    }
    mediaPlayer.setLooping(looping);
  }

  /**
   * release resource when playback finished
   */
  @Override
  public void onCompletion(MediaPlayer mp) {
//    service.stopSelf();
    mmxLog("complete : "+this.toString());
    mediaPlayer.reset();
    service.TryNextSong();
  }

  @Override
  protected void finalize() throws Throwable {
    mmxLog("finalize : "+this.toString());
    Release();
    super.finalize();
  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    mmxLog("error : "+ what);
    //re-init player on any errors
    InitMediaPlayer();
    //return false to let onComplete being called to continue
    return false;
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    service.setState(true, false);
  }

  public void Release() {
    if (mediaPlayer != null)
    {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }
}
