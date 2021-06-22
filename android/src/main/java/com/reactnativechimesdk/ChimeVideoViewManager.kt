package com.reactnativechimesdk

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.reactnativechimesdk.views.ChimeView

class ChimeVideoViewManager : SimpleViewManager<ChimeView>() {

  companion object {
    const val NAME = "ChimeVideoView"
    const val TAG = "SimpleChimeViewLog"
  }

  override fun getName(): String = NAME

  @SuppressLint("InflateParams")
  override fun createViewInstance(reactContext: ThemedReactContext): ChimeView {
    val inflater = reactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return inflater.inflate(R.layout.video_chime_view, null) as ChimeView
  }

  @ReactProp(name = "userID")
  fun bindVideoTile(renderView: ChimeView, attendeeId: String) {
    renderView.bindVideoTile(attendeeId)
  }
}
