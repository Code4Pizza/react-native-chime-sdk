package com.reactnativechimesdk.itf;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus;

import java.util.Objects;

public interface SimpleAudioVideoObserver extends AudioVideoObserver {

  String TAG = "SimpleAudioVideoObserve";

  @Override
  default void onAudioSessionCancelledReconnect() {

  }

  @Override
  default void onAudioSessionDropped() {

  }

  @Override
  default void onAudioSessionStarted(boolean b) {
  }

  @Override
  default void onAudioSessionStartedConnecting(boolean b) {

  }

  @Override
  default void onAudioSessionStopped(@NonNull MeetingSessionStatus meetingSessionStatus) {
    Log.d(TAG, "onAudioSessionStopped: " + Objects.requireNonNull(meetingSessionStatus.getStatusCode()).name());
  }

  @Override
  default void onConnectionBecamePoor() {

  }

  @Override
  default void onConnectionRecovered() {

  }

  @Override
  default void onVideoSessionStarted(@NonNull MeetingSessionStatus meetingSessionStatus) {
    Log.d(TAG, "onVideoSessionStarted: " + Objects.requireNonNull(meetingSessionStatus.getStatusCode()).name());
  }

  @Override
  default void onVideoSessionStartedConnecting() {

  }

  @Override
  default void onVideoSessionStopped(@NonNull MeetingSessionStatus meetingSessionStatus) {
    Log.d(TAG, "onVideoSessionStopped: " + Objects.requireNonNull(meetingSessionStatus.getStatusCode()).name());
  }
}
