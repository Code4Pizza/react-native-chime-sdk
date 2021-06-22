package com.reactnativechimesdk.itf

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState

interface SimpleVideoTileObserver : VideoTileObserver {

  companion object {
    const val TAG = "SimpleVideoTileObserver"
  }

  override fun onVideoTileAdded(tileState: VideoTileState) {
    Log.d(TAG, "onVideoTileAdded: " + tileState.attendeeId)
  }

  override fun onVideoTilePaused(tileState: VideoTileState) {
  }

  override fun onVideoTileRemoved(tileState: VideoTileState) {
    Log.d(TAG, "onVideoTileRemoved: ")
  }

  override fun onVideoTileResumed(tileState: VideoTileState) {
  }

  override fun onVideoTileSizeChanged(tileState: VideoTileState) {
  }
}
