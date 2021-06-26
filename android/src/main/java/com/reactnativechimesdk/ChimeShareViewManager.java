package com.reactnativechimesdk;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativechimesdk.views.ChimeShareView;

import org.jetbrains.annotations.NotNull;

public class ChimeShareViewManager extends SimpleViewManager<ChimeShareView> {

  @NonNull
  @NotNull
  @Override
  public String getName() {
    return "RNShareViewChimeSdk";
  }

  @NonNull
  @NotNull
  @Override
  protected ChimeShareView createViewInstance(@NonNull @NotNull ThemedReactContext reactContext) {
    LayoutInflater inflater = (LayoutInflater) reactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return (ChimeShareView) inflater.inflate(R.layout.video_share_chime_view, null);
  }

  @ReactProp(name = "userID")
  public void bindVideoTile(ChimeShareView renderView, String attendeeId) {
    renderView.bindVideoTile(attendeeId);
  }
}
