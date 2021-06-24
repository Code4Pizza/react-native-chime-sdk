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
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
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
import com.google.gson.Gson;
import com.reactnativechimesdk.data.RosterAttendee;
import com.reactnativechimesdk.data.VideoCollectionTile;
import com.reactnativechimesdk.itf.SimpleAudioVideoObserver;
import com.reactnativechimesdk.itf.SimpleRealTimeObserver;
import com.reactnativechimesdk.itf.SimpleVideoTileObserver;
import com.reactnativechimesdk.response.JoinMeetingResponse;
import com.reactnativechimesdk.utils.Util;

import org.json.JSONException;
import org.json.JSONObject;

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
import static com.reactnativechimesdk.EventEmitter.KEY_VIDEO_STATUS;
import static com.reactnativechimesdk.EventEmitter.MEETING_AUDIO_STATUS_CHANGE;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_JOIN;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_LEFT;
import static com.reactnativechimesdk.EventEmitter.MEETING_VIDEO_STATUS_CHANGE;
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

  public ChimeSdkModule(@Nullable ReactApplicationContext reactContext) {
    super(reactContext);
    assert reactContext != null;
    reactContext.addLifecycleEventListener(this);
  }

  private final Gson gson = new Gson();
  private JoinMeetingResponse joinMeetingResponse;

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
    try {
      JSONObject object = Util.convertMapToJson(map);
      joinMeetingResponse = gson.fromJson(object.toString(), JoinMeetingResponse.class);
      if (joinMeetingResponse.joinInfo == null) {
        String url = map.getString("meetingUrl");
        String id = map.getString("meetingId");
        String name = map.getString("attendeeName");
        String rs = requestCreateSession(url, id, name);
        joinMeetingResponse = gson.fromJson(rs, JoinMeetingResponse.class);
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to create meeting response", e);
      return;
    }

    if (joinMeetingResponse.joinInfo != null) {
      MeetingModel.getInstance().setLocalId(joinMeetingResponse.joinInfo.attendeeResponse.attendee.getAttendeeId());
    }

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
    if (joinMeetingResponse != null) {
      MeetingSessionConfiguration config = createSession(joinMeetingResponse);
      if (config == null) {
        showToast("Failed to join meeting");
        return;
      }
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
        map.putBoolean(KEY_VIDEO_STATUS, MeetingModel.getInstance().isCameraAttendeeOn(it.getAttendeeId()));
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
        map.putBoolean(KEY_VIDEO_STATUS, MeetingModel.getInstance().isCameraAttendeeOn(attendee.getAttendeeId()));
        callback.invoke(map);
      });
  }

  @ReactMethod
  public void onMyAudio() {
    if (MeetingModel.getInstance().getAudioVideo() != null) {
      MeetingModel.getInstance().getAudioVideo().realtimeLocalUnmute();
    }
  }

  @ReactMethod
  public void offMyAudio() {
    if (MeetingModel.getInstance().getAudioVideo() != null) {
      MeetingModel.getInstance().getAudioVideo().realtimeLocalMute();
    }
  }

  @ReactMethod
  public void onOffMyVideo() {
    if (MeetingModel.getInstance().getAudioVideo() == null) {
      return;
    }
    if (MeetingModel.getInstance().isCameraLocalOn()) {
      MeetingModel.getInstance().getAudioVideo().stopLocalVideo();
    } else {
      MeetingModel.getInstance().getAudioVideo().startLocalVideo();
    }
  }

  @ReactMethod
  public void switchMyCamera() {
  }

  @Override
  public void onAudioSessionStarted(boolean reconnecting) {
    if (MeetingModel.getInstance().getAudioVideo() == null) {
      return;
    }
    boolean success = MeetingModel.getInstance().getAudioVideo().realtimeSetVoiceFocusEnabled(true);
    if (success) {
      Log.d(TAG, "Set voice focus success");
    } else {
      Log.d(TAG, "Failed to set voice focus");
    }

    List<MediaDevice> devices = MeetingModel.getInstance().getAudioVideo().listAudioDevices();

    Stream.of(devices).forEach(it -> {
      Log.d(TAG, "Device media : " + it.getType() + " - " + it.getLabel());
    });
    MeetingModel.getInstance().getAudioVideo().chooseAudioDevice(devices.get(0));
  }

  @Override
  public void onAttendeesJoined(@NonNull AttendeeInfo[] attendeeInfos) {
    // Log.d(TAG, "onAttendeesJoined: " + attendeeInfos.length);
    Stream.of(attendeeInfos).forEach(it -> {
      RosterAttendee newAttendee = MeetingModel.getInstance().getCurrentRoster().put(
        it.getAttendeeId(),
        new RosterAttendee(it.getAttendeeId(), getAttendeeName(it.getAttendeeId(), it.getExternalUserId()))
      );
      if (MeetingModel.getInstance().isLocal(newAttendee.getAttendeeId())) {
        Log.d(TAG, "local attendee joined: " + it.getAttendeeId());
        sendMeetingStateEvent(getReactApplicationContext(), "meeting_ready");
      } else {
        Log.d(TAG, "remote attendee joined: " + it.getAttendeeId());
        boolean isCameraOn = MeetingModel.getInstance().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_USER_JOIN, newAttendee, isCameraOn);
      }
    });
  }

  @Override
  public void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
    // Log.d(TAG, "onAttendeesLeft: " + attendeeInfos.length);
    Stream.of(attendeeInfos).forEach(it -> {
      RosterAttendee removal = MeetingModel.getInstance().getCurrentRoster().remove(it.getAttendeeId());
      if (removal != null) {
        boolean isCameraOn = MeetingModel.getInstance().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(ChimeSdkModule.this.getReactApplicationContext(), MEETING_USER_LEFT, removal, isCameraOn);
      }
    });
  }

  @Override
  public void onVideoTileAdded(@NonNull VideoTileState tileState) {
    Log.d(TAG, "onVideoTileAdded tile id: " + tileState.getTileId());
    VideoCollectionTile videoCollectionTile = new VideoCollectionTile(tileState);
    MeetingModel.getInstance().addVideoTile(videoCollectionTile);
    RosterAttendee attendee = MeetingModel.getInstance().getCurrentRoster().get(tileState.getAttendeeId());
    sendMeetingUserEvent(getReactApplicationContext(), MEETING_VIDEO_STATUS_CHANGE, attendee, true);
  }

  @Override
  public void onVideoTileRemoved(@NonNull VideoTileState tileState) {
    Log.d(TAG, "onVideoTileRemoved tile id: " + tileState.getTileId());
    RosterAttendee attendee = MeetingModel.getInstance().getCurrentRoster().get(tileState.getAttendeeId());
    sendMeetingUserEvent(getReactApplicationContext(), MEETING_VIDEO_STATUS_CHANGE, attendee, false);
    MeetingModel.getInstance().remoteVideoTile(tileState.getTileId());
  }

  @Override
  public void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = MeetingModel.getInstance().getAttendee(it.getAttendeeId());
        RosterAttendee attendee = MeetingModel.getInstance().getCurrentRoster().put(it.getAttendeeId(),
          new RosterAttendee(
            current.getAttendeeId(),
            current.getAttendeeName(),
            true
          ));
        boolean isCameraOn = MeetingModel.getInstance().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_AUDIO_STATUS_CHANGE, attendee, isCameraOn);
      }
    );
  }

  @Override
  public void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = MeetingModel.getInstance().getAttendee(it.getAttendeeId());
        RosterAttendee attendee = MeetingModel.getInstance().getCurrentRoster().put(it.getAttendeeId(),
          new RosterAttendee(
            current.getAttendeeId(),
            current.getAttendeeName(),
            true
          ));
        boolean isCameraOn = MeetingModel.getInstance().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_AUDIO_STATUS_CHANGE, attendee, isCameraOn);
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
