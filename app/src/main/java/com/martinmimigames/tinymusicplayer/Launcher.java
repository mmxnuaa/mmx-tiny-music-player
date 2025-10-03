package com.martinmimigames.tinymusicplayer;

import static java.lang.Thread.currentThread;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.system.Os;
import android.util.Log;

import java.io.File;

/**
 * activity for controlling the playback by invoking different logics based on incoming intents
 */
public class Launcher extends Activity {

  protected void mmxLog(String msg)
  {
    Log.i("mmx", "launch tid(" + currentThread().getId()+"):"+ currentThread().getName() +" =>"+msg );
  }

  static final String TYPE = "type";
  static final byte NULL = 0;
  static final byte PLAY_PAUSE = 1;
  static final byte KILL = 2;
  static final byte PLAY = 3;
  static final byte PAUSE = 4;

  static final byte LOOP = 5;

  private static final int REQUEST_CODE = 3216487;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mmxLog("onCreate: activity: "+this.toString() + " intent: "+getIntent().getAction().toString());
    if (!Intent.ACTION_VIEW.equals(getIntent().getAction())
      && !Intent.ACTION_SEND.equals(getIntent().getAction())) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        this.getPackageManager()
          .checkPermission(
            Manifest.permission.POST_NOTIFICATIONS, this.getPackageName())
          != PackageManager.PERMISSION_GRANTED) {
        final var intent = new Intent();
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.getPackageName());
        this.startActivity(intent);
        finish();
        return;
      }

      /* request a file from the system */
//      var intent = new Intent(Intent.ACTION_GET_CONTENT);
//      intent.setType("audio/*"); // intent type to filter application based on your requirement
//      startActivityForResult(intent, REQUEST_CODE);

      var intent = new Intent(Intent.ACTION_VIEW);
//      var Uri = android.net.Uri.fromFile(new File("/storage/sdcard0/Download/09.mp3"));
      var Uri = android.net.Uri.fromFile(new File("/data/local/tmp/tf1/EnglishSong/Top Hits of 2019/100. Paulo Londra - Adan y Eva.mp3"));
      intent.setData(Uri);
      onIntent(intent);
      return;
    }
    onIntent(getIntent());
  }

  /**
   * redirect call to actual logic
   */
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    onIntent(intent);
  }

  /**
   * restarts service
   */
  private void onIntent(Intent intent) {
    intent.setClass(this, Service.class);
    stopService(intent);
    startService(intent);
    /* does not need to keep this activity */
    finish();
  }

  /**
   * call service control on receiving file
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    /* if result unusable, discard */
    if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      /* redirect to service */
      intent.setAction(Intent.ACTION_VIEW);
      onIntent(intent);
      return;
    }
    finish();
  }
}