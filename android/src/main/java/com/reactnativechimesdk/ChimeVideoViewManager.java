package com.reactnativechimesdk;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativechimesdk.views.ChimeView;

public class ChimeVideoViewManager extends SimpleViewManager<ChimeView> {

  @NonNull
  @Override
  public String getName() {
    return "ChimeVideoView";
  }

  @NonNull
  @Override
  protected ChimeView createViewInstance(@NonNull ThemedReactContext themedReactContext) {
    LayoutInflater inflater = (LayoutInflater) themedReactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return (ChimeView) inflater.inflate(R.layout.video_chime_view, null);
  }

  @ReactProp(name = "userID")
  public void bindVideoTile(ChimeView renderView, String attendeeId) {
    renderView.bindVideoTile(attendeeId);
  }
}
