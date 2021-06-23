package com.reactnativechimesdk.itf;

import android.util.Log;

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
    Log.d(TAG, "onAttendeesJoined: " + attendeeInfos.length);
  }

  @Override
  default void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
    Log.d(TAG, "onAttendeesLeft: " + attendeeInfos.length);
  }

  @Override
  default void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Log.d(TAG, "onAttendeesMuted: " + attendeeInfos.length);
  }

  @Override
  default void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Log.d(TAG, "onAttendeesUnmuted: " + attendeeInfos.length);
  }

  @Override
  default void onSignalStrengthChanged(@NonNull SignalUpdate[] signalUpdates) {

  }

  @Override
  default void onVolumeChanged(@NonNull VolumeUpdate[] volumeUpdates) {

  }
}
