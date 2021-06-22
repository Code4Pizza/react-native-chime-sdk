package com.reactnativechimesdk.itf

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver

interface SimpleRealTimeObserver : RealtimeObserver {

  companion object {
    const val TAG = "SimpleRealTimeObserver"
  }

  override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
    Log.d(TAG, "onAttendeesDropped: ")
  }

  override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
    Log.d(TAG, "onAttendeesJoined: ")
  }

  override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
    Log.d(TAG, "onAttendeesLeft: ")
  }

  override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
    Log.d(TAG, "onAttendeesMuted: ")
  }

  override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
    Log.d(TAG, "onAttendeesUnmuted: ")
  }

  override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
  }

  override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
  }
}
