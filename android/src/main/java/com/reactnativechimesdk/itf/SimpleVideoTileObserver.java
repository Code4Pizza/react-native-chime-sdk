package com.reactnativechimesdk.itf;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;

public interface SimpleVideoTileObserver extends VideoTileObserver {

  String TAG = "SimpleVideoTileObserver";

  @Override
  default void onVideoTileAdded(@NonNull VideoTileState videoTileState) {
    Log.d(TAG, "onVideoTileAdded: " + videoTileState.getTileId());
  }

  @Override
  default void onVideoTilePaused(@NonNull VideoTileState videoTileState) {

  }

  @Override
  default void onVideoTileRemoved(@NonNull VideoTileState videoTileState) {
    Log.d(TAG, "onVideoTileRemoved: " + videoTileState.getTileId());
  }

  @Override
  default void onVideoTileResumed(@NonNull VideoTileState videoTileState) {

  }

  @Override
  default void onVideoTileSizeChanged(@NonNull VideoTileState videoTileState) {

  }
}
