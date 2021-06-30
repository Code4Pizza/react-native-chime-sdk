package com.reactnativechimesdk.itf;

import androidx.annotation.NonNull;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate;
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver;

public interface SimpleRealTimeObserver extends RealtimeObserver {

  String TAG = "SimpleRealTimeObserver";

  @Override
  default void onAttendeesDropped(@NonNull AttendeeInfo[] attendeeInfos) {

  }

  @Override
  default void onAttendeesJoined(@NonNull AttendeeInfo[] attendeeInfos) {
  }

  @Override
  default void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
  }

  @Override
  default void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
  }

  @Override
  default void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
  }

  @Override
  default void onSignalStrengthChanged(@NonNull SignalUpdate[] signalUpdates) {
    // Stream.of(signalUpdates).forEach(it -> Log.d(TAG, "onSignalStrengthChanged " + it.getAttendeeInfo().getExternalUserId() + " - " + it.getSignalStrength().name()));
  }

  @Override
  default void onVolumeChanged(@NonNull VolumeUpdate[] volumeUpdates) {
    // Stream.of(volumeUpdates).forEach(it -> Log.d(TAG, "onVolumeChanged: "  + it.getAttendeeInfo().getExternalUserId() + " - " + it.getVolumeLevel()));
  }
}
