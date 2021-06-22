package com.reactnativechimesdk.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.facebook.react.bridge.ReactContext
import com.reactnativechimesdk.ChimeVideoViewManager
import com.reactnativechimesdk.MeetingSessionModel
import com.reactnativechimesdk.R

class ChimeView : FrameLayout {

  private lateinit var renderView: DefaultVideoRenderView

  private var attendeeId: String? = null
  private var success: Boolean = false
  private var visible: Boolean = false

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  init {
      MeetingSessionModel.getRemoteVideosLiveData().observe(((context as ReactContext).currentActivity as AppCompatActivity), Observer { list ->
        list.forEach {
          if (it.videoTileState.attendeeId == attendeeId && !success && visible) {
            Log.d(ChimeVideoViewManager.TAG, "bind remote video $attendeeId")
            success = true
            MeetingSessionModel.audioVideo.bindVideoView(renderView, it.videoTileState.tileId)
          }
        }
      })
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    renderView = findViewById(R.id.videoView)

  }

  fun bindVideoTile(attendeeId: String) {
    visible = attendeeId.isNotEmpty()
    if (visible) {
      this.attendeeId = attendeeId
      bind()
    } else {
      unbind()
    }
  }

  private fun bind() {
    if (isLocal()) {
      MeetingSessionModel.localVideoTileState?.videoTileState?.let {
        Log.d(ChimeVideoViewManager.TAG, "bind local video")
        success = true
        MeetingSessionModel.audioVideo.bindVideoView(renderView, it.tileId)
      }
    } else {
      MeetingSessionModel.remoteVideoTileStates.forEach {
        if (it.videoTileState.attendeeId == attendeeId) {
          Log.d(ChimeVideoViewManager.TAG, "bind remote video $attendeeId")
          success = true
          MeetingSessionModel.audioVideo.bindVideoView(renderView, it.videoTileState.tileId)
        }
      }
    }

    if (!success) {
      Log.d(ChimeVideoViewManager.TAG, "Video tile is not available on attendee: $attendeeId")
    }
  }

  private fun unbind() {
    if (isLocal()) {
      MeetingSessionModel.localVideoTileState?.videoTileState?.let {
        MeetingSessionModel.audioVideo.unbindVideoView(it.tileId)
      }
    } else {
      MeetingSessionModel.remoteVideoTileStates.forEach {
        if (it.videoTileState.attendeeId == attendeeId) {
          MeetingSessionModel.audioVideo.unbindVideoView(it.videoTileState.tileId)
        }
      }
    }
  }

  private fun isLocal(): Boolean {
    return MeetingSessionModel.localAttendeeId == attendeeId
  }
}
