package com.reactnativechimesdk;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChimeSdkPackage implements ReactPackage {
  @NonNull
  @Override
  public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactApplicationContext) {
    return Collections.singletonList(new ChimeSdkModule(reactApplicationContext));
  }

  @NonNull
  @Override
  public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactApplicationContext) {
    return Arrays.asList(new ChimeVideoViewManager(), new ChimeShareViewManager());
  }
}
