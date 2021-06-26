package com.reactnativechimesdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.VideoCaptureFormat;
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType;
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode;
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

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
import static com.reactnativechimesdk.EventEmitter.MEETING_ACTIVE_SHARE;
import static com.reactnativechimesdk.EventEmitter.MEETING_AUDIO_STATUS_CHANGE;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_JOIN;
import static com.reactnativechimesdk.EventEmitter.MEETING_USER_LEFT;
import static com.reactnativechimesdk.EventEmitter.MEETING_VIDEO_STATUS_CHANGE;
import static com.reactnativechimesdk.EventEmitter.SHARE_STATUS_START;
import static com.reactnativechimesdk.EventEmitter.SHARE_STATUS_STOP;
import static com.reactnativechimesdk.EventEmitter.sendMeetingStateEvent;
import static com.reactnativechimesdk.EventEmitter.sendMeetingUserEvent;
import static com.reactnativechimesdk.EventEmitter.sendMeetingUserShareEvent;
import static com.reactnativechimesdk.MeetingModel.meetingModel;
import static com.reactnativechimesdk.response.Api.createSession;
import static com.reactnativechimesdk.response.Api.requestCreateSession;
import static com.reactnativechimesdk.utils.Util.getAttendeeName;

public class ChimeSdkModule extends ReactContextBaseJavaModule
  implements LifecycleEventListener, SimpleRealTimeObserver, SimpleAudioVideoObserver, SimpleVideoTileObserver, DeviceChangeObserver, ContentShareObserver {

  private static final String TAG = "ChimeSdkModule";
  private static final ConsoleLogger logger = new ConsoleLogger(LogLevel.INFO);

  private static final int MAX_VIDEO_FORMAT_HEIGHT = 240;
  private static final int MAX_VIDEO_FORMAT_FPS = 15;

  private final int WEBRTC_PERMISSION_REQUEST_CODE = 1030;
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
        int requestCode = intent.getIntExtra("requestCode", -1);
        if (permissions == null || grantResults == null || requestCode != WEBRTC_PERMISSION_REQUEST_CODE) {
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

      // For example only
      if (joinMeetingResponse.joinInfo == null) {
        String url = map.getString("meetingUrl");
        String id = map.getString("meetingId");
        String name = map.getString("attendeeName");
        String rs = requestCreateSession(url, id, name);
        joinMeetingResponse = gson.fromJson(rs, JoinMeetingResponse.class);
      }

      if (joinMeetingResponse != null && joinMeetingResponse.joinInfo != null) {
        meetingModel().setLocalId(joinMeetingResponse.joinInfo.attendeeResponse.attendee.getAttendeeId());
      } else {
        throw new IllegalArgumentException("Error parsing meeting response");
      }
    } catch (IOException | JSONException | IllegalArgumentException e) {
      Log.e(TAG, "Failed to create meeting response", e);
      return;
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
      MeetingSession meetingSession = new DefaultMeetingSession(config, logger, getReactApplicationContext(), meetingModel().eglCoreFactory);
      meetingModel().setMeetingSession(meetingSession);
      meetingModel().setCameraCaptureSource(initCameraCaptureSource());
      meetingModel().getAudioVideo().addRealtimeObserver(this);
      meetingModel().getAudioVideo().addAudioVideoObserver(this);
      meetingModel().getAudioVideo().addVideoTileObserver(this);
      meetingModel().getAudioVideo().addDeviceChangeObserver(this);
      meetingModel().getAudioVideo().addContentShareObserver(this);
      meetingModel().startMeeting();
    } else {
      showToast("Failed to join meeting");
    }
  }

  private CameraCaptureSource initCameraCaptureSource() {
    DefaultSurfaceTextureCaptureSourceFactory surface = new DefaultSurfaceTextureCaptureSourceFactory(logger, meetingModel().eglCoreFactory);
    CameraCaptureSource cameraCaptureSource = new DefaultCameraCaptureSource(getReactApplicationContext(), logger, surface);
    try {
      CameraManager cameraManager = (CameraManager) getReactApplicationContext().getSystemService(Context.CAMERA_SERVICE);
      List<MediaDevice> mediaDevices = MediaDevice.Companion.listVideoDevices(cameraManager);
      MediaDevice mediaDevice = Stream.of(mediaDevices)
        .filter(it -> it.getType() == MediaDeviceType.VIDEO_FRONT_CAMERA)
        .findFirstOrElse(Stream.of(mediaDevices).findFirst().orElseThrow());
      cameraCaptureSource.setDevice(mediaDevice);
      assert cameraCaptureSource.getDevice() != null;
      List<VideoCaptureFormat> formats = MediaDevice.Companion.listSupportedVideoCaptureFormats(cameraManager, cameraCaptureSource.getDevice());
      Stream.of(formats)
        .filter(it -> it.getHeight() <= MAX_VIDEO_FORMAT_HEIGHT)
        .findFirst()
        .ifPresent(it -> cameraCaptureSource.setFormat(new VideoCaptureFormat(it.getWidth(), it.getHeight(), MAX_VIDEO_FORMAT_FPS)));
    } catch (Exception e) {
      Log.e(TAG, "Media devices not found");
    }
    return cameraCaptureSource;
  }

  @ReactMethod
  public void leaveCurrentMeeting() {
    sendMeetingStateEvent(getReactApplicationContext(), "meeting_end");
    meetingModel().endMeeting();
  }

  @ReactMethod
  public void getParticipants(Callback callback) {
    WritableArray array = new WritableNativeArray();
    Stream.of(meetingModel().getCurrentRoster().values())
      .forEach(it -> {
        WritableMap map = new WritableNativeMap();
        map.putString(KEY_USER_ID, it.getAttendeeId());
        map.putString(KEY_USER_NAME, it.getAttendeeName());
        map.putBoolean(KEY_AUDIO_STATUS, it.isMuted());
        map.putBoolean(KEY_VIDEO_STATUS, meetingModel().isCameraAttendeeOn(it.getAttendeeId()));
        array.pushMap(map);
      });
    callback.invoke(array);
  }

  @ReactMethod
  public void getUserInfo(String attendeeId, Callback callback) {
    Stream.of(meetingModel().getCurrentRoster().values())
      .filter(it -> it.getAttendeeId().equals(attendeeId))
      .findFirst()
      .ifPresent(attendee -> {
        WritableMap map = new WritableNativeMap();
        map.putString(KEY_USER_ID, attendee.getAttendeeId());
        map.putString(KEY_USER_NAME, attendee.getAttendeeName());
        map.putBoolean(KEY_AUDIO_STATUS, attendee.isMuted());
        map.putBoolean(KEY_VIDEO_STATUS, meetingModel().isCameraAttendeeOn(attendee.getAttendeeId()));
        callback.invoke(map);
      });
  }

  @ReactMethod
  public void onMyAudio() {
    meetingModel().onOffAudio(true);
  }

  @ReactMethod
  public void offMyAudio() {
    meetingModel().onOffAudio(false);
  }

  @ReactMethod
  public void onOffMyVideo() {
    meetingModel().onOffVideo();
  }

  @ReactMethod
  public void switchMyCamera() {
    meetingModel().switchCamera();
  }

  @Override
  public void onAudioSessionStarted(boolean reconnecting) {
    meetingModel().initMediaDevice();
  }

  @Override
  public void onAudioSessionStopped(@NonNull @NotNull MeetingSessionStatus sessionStatus) {
    if (sessionStatus.getStatusCode() != MeetingSessionStatusCode.OK) {
      meetingModel().endMeeting();
    }
  }

  @Override
  public void onAudioDeviceChanged(@NotNull List<MediaDevice> list) {
    meetingModel().selectAudioDevice(list);
  }

  @Override
  public void onAttendeesJoined(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
      meetingModel().getCurrentRoster().put(
        it.getAttendeeId(),
        new RosterAttendee(it.getAttendeeId(), getAttendeeName(it.getExternalUserId()))
      );
      RosterAttendee newAttendee = meetingModel().getAttendee(it.getAttendeeId());
      if (meetingModel().isLocal(newAttendee.getAttendeeId())) {
        Log.d(TAG, "local attendee joined: " + it.getAttendeeId());
        sendMeetingStateEvent(getReactApplicationContext(), "meeting_ready");
      } else {
        Log.d(TAG, "remote attendee joined: " + it.getAttendeeId());
        boolean isCameraOn = meetingModel().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_USER_JOIN, newAttendee, isCameraOn);
      }
    });
  }

  @Override
  public void onAttendeesLeft(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
      RosterAttendee removal = meetingModel().getCurrentRoster().remove(it.getAttendeeId());
      if (removal != null) {
        boolean isCameraOn = meetingModel().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(ChimeSdkModule.this.getReactApplicationContext(), MEETING_USER_LEFT, removal, isCameraOn);
      }
    });
  }

  @Override
  public void onVideoTileAdded(@NonNull VideoTileState tileState) {
    if (tileState.isContent()) {
      handleShareVideoTileAdded(tileState.getAttendeeId());
    } else {
      handleVideoTileAdded(tileState);
    }
  }

  private void handleShareVideoTileAdded(String attendeeId) {
    RosterAttendee attendee = meetingModel().getAttendee(attendeeId);
    boolean isCameraOn = meetingModel().isCameraAttendeeOn(attendeeId);
    sendMeetingUserShareEvent(getReactApplicationContext(), MEETING_ACTIVE_SHARE, attendee, isCameraOn, SHARE_STATUS_START);
  }

  private void handleVideoTileAdded(VideoTileState tile) {
    Log.d(TAG, "onVideoTileAdded tile id: " + tile.getTileId());
    meetingModel().addVideoTile(new VideoCollectionTile(tile));
    sendMeetingUserEvent(getReactApplicationContext(), MEETING_VIDEO_STATUS_CHANGE, meetingModel().getAttendee(tile.getAttendeeId()), true);
  }

  @Override
  public void onVideoTilePaused(@NonNull @NotNull VideoTileState videoTileState) {
    Log.d(TAG, "onVideoTilePaused: " + videoTileState.getTileId() + " state " + videoTileState.getPauseState().name());
  }

  @Override
  public void onVideoTileResumed(@NonNull @NotNull VideoTileState videoTileState) {
    Log.d(TAG, "onVideoTileResumed: " + videoTileState.getTileId());
  }

  @Override
  public void onVideoTileRemoved(@NonNull VideoTileState tileState) {
    if (tileState.isContent()) {
      handleShareVideoTileRemoved(tileState.getAttendeeId());
    } else {
      handleVideoTileRemoved(tileState);
    }
  }

  private void handleShareVideoTileRemoved(String attendeeId) {
    RosterAttendee attendee = meetingModel().getAttendee(attendeeId);
    boolean isCameraOn = meetingModel().isCameraAttendeeOn(attendeeId);
    sendMeetingUserShareEvent(getReactApplicationContext(), MEETING_ACTIVE_SHARE, attendee, isCameraOn, SHARE_STATUS_STOP);
  }

  private void handleVideoTileRemoved(VideoTileState tile) {
    Log.d(TAG, "onVideoTileRemoved tile id: " + tile.getTileId());
    sendMeetingUserEvent(getReactApplicationContext(), MEETING_VIDEO_STATUS_CHANGE, meetingModel().getAttendee(tile.getAttendeeId()), false);
    meetingModel().removeVideoTile(tile.getTileId());
  }

  @Override
  public void onAttendeesMuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = meetingModel().getAttendee(it.getAttendeeId());
        RosterAttendee attendee = meetingModel().getCurrentRoster().put(it.getAttendeeId(),
          new RosterAttendee(
            current.getAttendeeId(),
            current.getAttendeeName(),
            true
          ));
        boolean isCameraOn = meetingModel().isCameraAttendeeOn(it.getAttendeeId());
        sendMeetingUserEvent(getReactApplicationContext(), MEETING_AUDIO_STATUS_CHANGE, attendee, isCameraOn);
      }
    );
  }

  @Override
  public void onAttendeesUnmuted(@NonNull AttendeeInfo[] attendeeInfos) {
    Stream.of(attendeeInfos).forEach(it -> {
        RosterAttendee current = meetingModel().getAttendee(it.getAttendeeId());
        RosterAttendee attendee = meetingModel().getCurrentRoster().put(it.getAttendeeId(),
          new RosterAttendee(
            current.getAttendeeId(),
            current.getAttendeeName(),
            true
          ));
        boolean isCameraOn = meetingModel().isCameraAttendeeOn(it.getAttendeeId());
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
  }

  @Override
  public void onHostDestroy() {
    getReactApplicationContext().unregisterReceiver(moduleConfigReceiver);
    meetingModel().endMeeting();
  }

  @Override
  public void onContentShareStarted() {
  }

  @Override
  public void onContentShareStopped(@NotNull ContentShareStatus contentShareStatus) {

  }
}
