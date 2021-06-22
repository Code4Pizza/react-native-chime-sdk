package com.reactnativechimesdk.itf

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus

interface SimpleAudioVideoObserver : AudioVideoObserver {

  companion object {
    const val TAG = "SimpleAudioListener"
  }

  override fun onAudioSessionCancelledReconnect() {
    Log.d(TAG, "onAudioSessionCancelledReconnect: ")
  }

  override fun onAudioSessionDropped() {
    Log.d(TAG, "onAudioSessionDropped: ")
  }

  override fun onAudioSessionStarted(reconnecting: Boolean) {
    Log.d(TAG, "onAudioSessionStarted: ")
  }

  override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
    Log.d(TAG, "onAudioSessionStartedConnecting: ")
  }

  override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
    Log.d(TAG, "onAudioSessionStopped: ")
  }

  override fun onConnectionBecamePoor() {
    Log.d(TAG, "onConnectionBecamePoor: ")
  }

  override fun onConnectionRecovered() {
    Log.d(TAG, "onConnectionRecovered: ")
  }

  override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
    Log.d(TAG, "onVideoSessionStarted: ")
  }

  override fun onVideoSessionStartedConnecting() {
    Log.d(TAG, "onVideoSessionStartedConnecting: ")
  }

  override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
    Log.d(TAG, "onVideoSessionStopped: ")
  }
}
