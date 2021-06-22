package com.example.reactnativechimesdk;

import android.content.Intent;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.facebook.react.ReactActivity;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "ChimeSdkExample";
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Intent intent = new Intent("onConfigurationChanged");
    intent.putExtra("newConfig", newConfig);
    this.sendBroadcast(intent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (permissions == null || grantResults == null) {
      return;
    }
    Intent intent = new Intent("onRequestPermissionsResult");
    intent.putExtra("permissions", permissions);
    intent.putExtra("grantResults", grantResults);
    this.sendBroadcast(intent);
  }
}
