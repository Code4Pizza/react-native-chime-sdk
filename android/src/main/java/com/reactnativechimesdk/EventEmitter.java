package com.reactnativechimesdk;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativechimesdk.data.RosterAttendee;

public class EventEmitter {

  private static final String MEETING_EVENT = "onChimeMeetingEvent";
  private static final String MEETING_STATE_CHANGE = "meetingStateChange";

  public static final String MEETING_USER_JOIN = "sinkMeetingUserJoin";
  public static final String MEETING_USER_LEFT = "sinkMeetingUserLeft";
  public static final String MEETING_ACTIVE_SHARE = "sinkMeetingActiveShare";
  public static final String MEETING_AUDIO_STATUS_CHANGE = "onSinkMeetingAudioStatusChange";
  public static final String MEETING_VIDEO_STATUS_CHANGE = "onSinkMeetingVideoStatusChange";

  public static final String KEY_EVENT = "event";
  public static final String KEY_DES = "des";
  public static final String KEY_USER_NAME = "userName";
  public static final String KEY_USER_ID = "userID";
  public static final String KEY_VIDEO_STATUS = "videoStatus";
  public static final String KEY_AUDIO_STATUS = "audioStatus";
  public static final String KEY_SHARE_STATUS = "shareStatus";

  public static final int SHARE_STATUS_START = 1;
  public static final int SHARE_STATUS_STOP = 4;

  public static void sendMeetingStateEvent(ReactContext context, String des) {
    WritableMap map = new WritableNativeMap();
    map.putString(KEY_EVENT, MEETING_STATE_CHANGE);
    map.putString(KEY_DES, des);
    sendEvent(context, map);
  }

  public static void sendMeetingUserEvent(ReactContext context, String event, RosterAttendee attendee, boolean isCameraOn) {
    if (attendee == null) {
      return;
    }
    WritableMap map = new WritableNativeMap();
    map.putString(KEY_EVENT, event);
    map.putString(KEY_USER_ID, attendee.getAttendeeId());
    map.putString(KEY_USER_NAME, attendee.getAttendeeName());
    map.putBoolean(KEY_AUDIO_STATUS, attendee.isMuted());
    map.putBoolean(KEY_VIDEO_STATUS, isCameraOn);
    sendEvent(context, map);
  }

  public static void sendMeetingUserShareEvent(ReactContext context, String event, RosterAttendee attendee, boolean isCameraOn, int shareStatus) {
    if (attendee == null) {
      return;
    }
    WritableMap map = new WritableNativeMap();
    map.putString(KEY_EVENT, event);
    map.putString(KEY_USER_ID, attendee.getAttendeeId());
    map.putString(KEY_USER_NAME, attendee.getAttendeeName());
    map.putBoolean(KEY_AUDIO_STATUS, attendee.isMuted());
    map.putBoolean(KEY_VIDEO_STATUS, isCameraOn);
    map.putInt(KEY_SHARE_STATUS, shareStatus);
    sendEvent(context, map);
  }

  private static void sendEvent(ReactContext context, WritableMap params) {
    context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(MEETING_EVENT, params);
  }
}
