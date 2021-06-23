package com.reactnativechimesdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger;
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel;
import com.annimon.stream.Stream;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.reactnativechimesdk.data.RosterAttendee;
import com.reactnativechimesdk.data.VideoCollectionTile;
import com.reactnativechimesdk.itf.SimpleAudioVideoObserver;
import com.reactnativechimesdk.itf.SimpleRealTimeObserver;
import com.reactnativechimesdk.itf.SimpleVideoTileObserver;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.MODIFY_AUDIO_SETTINGS;
import static android.Manifest.permission.RECORD_AUDIO;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.reactnativechimesdk.EventEmitter.KEY_AUDIO_STATUS;
import static com.reactnativechimesdk.EventEmitter.KEY_USER_ID;
import static com.reactnativechimesdk.EventEmitter.KEY_USER_NAME;
import static com.reactnativechimesdk.EventEmitter.MEETING_AUDIO_STATUS_CHANGE;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_JOIN;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_LEFT;
import static com.reactnativechimesdk.EventEmitter.sendMeetingStateEvent;
import static com.reactnativechimesdk.EventEmitter.sendMeetingUserEvent;
import static com.reactnativechimesdk.response.Api.createSession;
import static com.reactnativechimesdk.response.Api.requestCreateSession;
import static com.reactnativechimesdk.utils.Util.getAttendeeName;

public class ChimeSdkModule extends ReactContextBaseJavaModule
  implements LifecycleEventListener, SimpleRealTimeObserver, SimpleAudioVideoObserver, SimpleVideoTileObserver {

  private static final String TAG = "ChimeSdkModule";
  private static final ConsoleLogger logger = new ConsoleLogger(LogLevel.INFO);

  private final int WEBRTC_PERMISSION_REQUEST_CODE = 1;
  private final List<String> WEBRTC_PERM = Arrays.asList(MODIFY_AUDIO_SETTINGS, RECORD_AUDIO, CAMERA);
  private final IntentFilter intentFilter = new IntentFilter() {
    {
      addAction("onConfigurationChanged");
      addAction("onRequestPermissionsResult");
    }
  };

  private final BroadcastReceiver moduleConfigReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if ("onRequestPermissionsResult".equals(intent.getAction())) {
        String[] permissions = intent.getStringArrayExtra("permissions");
        int[] grantResults = intent.getIntArrayExtra("grantResults");
        if (permissions == null || grantResults == null) {
          return;
        }
        joinMeeting();
      }
    }
  };

  private String meetingUrl;
  private String meetingId;
  private String attendeeName;

  public ChimeSdkModule(@Nullable ReactApplicationContext reactContext) {
    super(reactContext);
    assert reactContext != null;
    reactContext.addLifecycleEventListener(this);
  }

  @NonNull
  @Override
  public String getName() {
    return "ChimeSdk";
  }

  @ReactMethod
  public void showToast(String msg) {
    Toast.makeText(getReactApplicationContext(), msg, Toast.LENGTH_LONG).show();
  }

  @ReactMethod
  public void initSdk() {
    // Ignore
  }

  @ReactMethod
  public void joinMeeting(ReadableMap map) {
    meetingUrl = map.getString("meetingUrl");
    meetingId = map.getString("meetingId");
    attendeeName = map.getString("attendeeName");

    if (hasPermissionsAlready()) {
      joinMeeting();
    } else {
      ActivityCompat.requestPermissions(
        Objects.requireNonNull(getCurrentActivity()),
        Stream.of(WEBRTC_PERM).toArray(String[]::new),
        WEBRTC_PERMISSION_REQUEST_CODE);
    }
  }

  private boolean hasPermissionsAlready() {
    return Stream.of(WEBRTC_PERM)
      .filter(p -> checkSelfPermission(getReactApplicationContext(), p) != PackageManager.PERMISSION_GRANTED)
      .toList()
      .isEmpty();
  }

  private void joinMeeting() {
    String response = requestCreateSession(meetingUrl, meetingId, attendeeName);
    if (response != null) {
      MeetingSessionConfiguration config = createSession(response);
      MeetingSession meetingSession = new DefaultMeetingSession(config, logger, getReactApplicationContext(), new DefaultEglCoreFactory());
      MeetingModel.getInstance().setMeetingSession(meetingSession);
      MeetingModel.getInstance().getAudioVideo().addRealtimeObserver(this);
      MeetingModel.getInstance().getAudioVideo().addAudioVideoObserver(this);
      MeetingModel.getInstance().getAudioVideo().addVideoTileObserver(this);
      MeetingModel.getInstance().startMeeting();
    } else {
      showToast("Failed to join meeting");
    }
  }

  @ReactMethod
  public void leaveCurrentMeeting() {
    sendMeetingStateEvent(getReactApplicationContext(), "meeting_end");
    new Handler().postDelayed(() -> sendMeetingStateEvent(getReactApplicationContext(), "idle"), 1500);
    MeetingModel.getInstance().endMeeting();
  }

  @ReactMethod
  public void getParticipants(Callback callback) {
    WritableArray array = new WritableNativeArray();
    Stream.of(MeetingModel.getInstance().getCurrentRoster().values())
      .forEach(it -> {
        WritableMap map = new WritableNativeMap();
        map.putString(KEY_USER_ID, it.getAttendeeId());
        map.putString(KEY_USER_NAME, it.getAttendeeName());
        map.putBoolean(KEY_AUDIO_STATUS, it.isMuted());
        array.pushMap(map);
      });
    callback.invoke(array);
  }

  @ReactMethod
  public void getUserInfo(String attendeeId, Callback callback) {
    Stream.of(MeetingModel.getInstance().getCurrentRoster().values())
      .filter(it -> it.getAttendeeId().equals(attendeeId))
      .findFirst()
      .ifPresent(attendee -> {
        WritableMap map = new WritableNativeMap();
        map.putString(KEY_USER_ID, attendee.getAttendeeId());
        map.putString(KEY_USER_NAME, attendee.getAttendeeName());
        map.putBoolean(KEY_AUDIO_STATUS, attendee.isMuted());
        callback.invoke(map);
      });
  }

  @ReactMethod
  public void onMyAudio() {
    MeetingModel.getInstance().getAudioVideo().realtimeLocalMute();
  }

  @ReactMethod
  public void offMyAudio() {
    MeetingModel.getInstance().getAudioVideo().realtimeLocalUnmute();
  }

  @ReactMethod
  public void onOffMyVideo() {
    if (MeetingModel.getInstance().isCameraOn()) {
      MeetingModel.getInstance().getAudioVideo().stopLocalVideo();
    } else {
      MeetingModel.getInstance().getAudioVideo().startLocalVideo();
    }
  }

  @ReactMethod
  public void switchMyCamera() {
  }

  @Override
  public void onAttendeesJoined(@NonNull AttendeeInfo[] attendeeInfos) {
    Log.d(TAG, "onAttendeesJoined: " + attendeeInfos.length);
    Stream.of(attendeeInfos).forEach(it -> {
      MeetingModel.getInstance().getCurrentRoster().put(
        it.getAttendeeId(),
        new RosterAttendee(it.getAttendeeId(), getAttendeeName(it.getAttendeeId(), it.getExternalUserId()))
      );
      if (it.getAttendeeId().equals(MeetingModel.getInstance().getLocalId())) {
        sendMeetingStateEvent(getReactApplicationContext(), "meeting_ready");
      }
    });
  }

  @Override
  public void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
    Log.d(TAG, "onAttendeesLeft: " + attendeeInfos.length);
    Stream.of(attendeeInfos).forEach(it -> {
      RosterAttendee removal = MeetingModel.getInstance().getCurrentRoster().get(it.getAttendeeId());
      sendMeetingUserEvent(getReactApplicationContext(), MEETING_USER_LEFT, removal);
      MeetingModel.getInstance().getCurrentRoster().remove(it.getAttendeeId());
    });
  }

  @Override
  public void onVideoTileAdded(@NonNull VideoTileState tileState) {
    Log.d(TAG, "onVideoTileAdded: " + tileState.getTileId());
    VideoCollectionTile videoCollectionTile;
    if (tileState.isLocalTile()) {
      videoCollectionTile = new VideoCollectionTile(attendeeName, tileState);
      MeetingModel.getInstance().setLocalId(tileState.getAttendeeId());
      MeetingModel.getInstance().setCameraOn(true);
    } else {
      String attendeeName = MeetingModel.getInstance().getCurrentRoster().get(tileState.getAttendeeId()).getAttendeeName();
      videoCollectionTile = new VideoCollectionTile(attendeeName, tileState);
      RosterAttendee newAttendee = MeetingModel.getInstance().getCurrentRoster().get(tileState.getAttendeeId());
      sendMeetingUserEvent(getReactApplicationContext(), MEETING_USER_JOIN, newAttendee);
    }
    MeetingModel.getInstance().addVideoTile(videoCollectionTile);
  }

  @Override
  public void onVideoTileRemoved(@NonNull VideoTileState tileState) {
    Log.d(TAG, "onVideoTileRemoved: " + tileState.getTileId());
    if (tileState.isLocalTile()) {
      MeetingModel.getInstance().setLocalId(null);
      MeetingModel.getInstance().setCameraOn(false);
    } else {
      MeetingModel.getInstance().remoteVideTile(tileState.getTileId());
    }
  }

  @Override
  public void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = MeetingModel.getInstance().getAttendee(it.getAttendeeId());
        MeetingModel.getInstance().getCurrentRoster().put(it.getAttendeeId(), new RosterAttendee(current.getAttendeeId(), current.getAttendeeName(), true));
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_AUDIO_STATUS_CHANGE, MeetingModel.getInstance().getAttendee((it.getAttendeeId())));
      }
    );
  }

  @Override
  public void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = MeetingModel.getInstance().getAttendee(it.getAttendeeId());
        MeetingModel.getInstance().getCurrentRoster().put(it.getAttendeeId(), new RosterAttendee(current.getAttendeeId(), current.getAttendeeName(), false));
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_AUDIO_STATUS_CHANGE, MeetingModel.getInstance().getAttendee((it.getAttendeeId())));
      }
    );
  }

  @Override
  public void onHostResume() {
    getReactApplicationContext().registerReceiver(moduleConfigReceiver, intentFilter);
  }

  @Override
  public void onHostPause() {
    getReactApplicationContext().unregisterReceiver(moduleConfigReceiver);
  }

  @Override
  public void onHostDestroy() {
    // TODO
  }
}
