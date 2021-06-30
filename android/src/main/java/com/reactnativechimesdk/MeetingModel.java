package com.reactnativechimesdk;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory;
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice;
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession;
import com.annimon.stream.Stream;
import com.reactnativechimesdk.data.RosterAttendee;
import com.reactnativechimesdk.data.VideoCollectionTile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MeetingModel {

  private static final MeetingModel ourInstance = new MeetingModel();

  public static MeetingModel meetingModel() {
    return ourInstance;
  }

  private MeetingModel() {
  }

  public final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  public final EglCoreFactory eglCoreFactory = new DefaultEglCoreFactory();

  private CameraCaptureSource cameraCaptureSource;
  private AudioVideoFacade audioVideo;

  private String localId;
  private boolean pauseLocalCamera;

  private final Map<String, RosterAttendee> currentRoster = new HashMap<>();

  private Set<VideoCollectionTile> videoTiles = new HashSet<>();
  private final MutableLiveData<Set<VideoCollectionTile>> videoTilesLive = new MutableLiveData<>();

  private VideoCollectionTile shareVideoTile;

  public Set<VideoCollectionTile> getVideoTiles() {
    return videoTiles;
  }

  public MutableLiveData<Set<VideoCollectionTile>> getVideoTilesLive() {
    return videoTilesLive;
  }

  public void addVideoTile(VideoCollectionTile videoCollectionTile) {
    videoTiles.add(videoCollectionTile);
    videoTilesLive.postValue(videoTiles);
  }

  public void removeVideoTile(int tileId) {
    videoTiles = new HashSet<>(Stream.of(videoTiles).filter(v -> v.getVideoTileState().getTileId() != tileId).toList());
    videoTilesLive.postValue(videoTiles);
    if (audioVideo != null) {
      audioVideo.unbindVideoView(tileId);
    }
  }

  public void setCameraCaptureSource(CameraCaptureSource cameraCaptureSource) {
    this.cameraCaptureSource = cameraCaptureSource;
  }

  public void setMeetingSession(MeetingSession meetingSession) {
    this.audioVideo = meetingSession.getAudioVideo();
  }

  public AudioVideoFacade getAudioVideo() {
    return audioVideo;
  }

  public void setLocalId(String localId) {
    this.localId = localId;
  }

  public boolean isLocal(String attendeeId) {
    return attendeeId.equals(localId);
  }

  public boolean isCameraLocalOn() {
    return Stream.of(videoTiles).filter(v -> v.getVideoTileState().isLocalTile()).findSingle().isPresent();
  }

  public boolean isCameraAttendeeOn(String attendeeId) {
    return Stream.of(videoTiles).filter(v -> v.getVideoTileState().getAttendeeId().equals(attendeeId)).findSingle().isPresent();
  }

  public Map<String, RosterAttendee> getCurrentRoster() {
    return currentRoster;
  }

  public RosterAttendee getAttendee(String attendeeId) {
    return currentRoster.get(attendeeId);
  }

  public void startMeeting() {
    if (audioVideo == null) {
      return;
    }
    audioVideo.start();
    audioVideo.startLocalVideo(cameraCaptureSource);
    audioVideo.startRemoteVideo();
    cameraCaptureSource.start();
  }

  public void pauseMeeting() {
    if (audioVideo == null) {
      return;
    }
    Stream.of(videoTiles).forEach(it -> {
      if (it.getVideoTileState().getPauseState() == VideoPauseState.Unpaused) {
        if (it.getVideoTileState().isLocalTile()) {
          audioVideo.stopLocalVideo();
          pauseLocalCamera = true;
        } else {
          audioVideo.pauseRemoteVideoTile(it.getVideoTileState().getTileId());
        }
      }
    });
  }

  public void resumeMeeting() {
    if (audioVideo == null) {
      return;
    }
    Stream.of(videoTiles).forEach(it -> {
      if (it.getVideoTileState().getPauseState() == VideoPauseState.PausedByUserRequest) {
        audioVideo.resumeRemoteVideoTile(it.getVideoTileState().getTileId());
      }
    });
    if (pauseLocalCamera) {
      audioVideo.startLocalVideo(cameraCaptureSource);
      cameraCaptureSource.start();
      pauseLocalCamera = false;
    }
  }

  public void endMeeting() {
    if (audioVideo == null) {
      return;
    }
    Stream.of(videoTiles).forEach(it -> audioVideo.unbindVideoView(it.getVideoTileState().getTileId()));
    if (shareVideoTile != null) {
      audioVideo.unbindVideoView(shareVideoTile.getVideoTileState().getTileId());
    }
    audioVideo.stopLocalVideo();
    audioVideo.stopRemoteVideo();
    audioVideo.stopContentShare();
    audioVideo.stop();
    cameraCaptureSource.stop();
    cleanup();
  }

  public void initMediaDevice() {
    if (audioVideo == null) {
      return;
    }
    // audioVideo.realtimeSetVoiceFocusEnabled(true);
    selectAudioDevice(audioVideo.listAudioDevices());
  }

  public void selectAudioDevice(List<MediaDevice> list) {
    if (audioVideo == null) {
      return;
    }
    Stream.of(list)
      .filter(it -> it.getType() != MediaDeviceType.OTHER)
      .findLast()
      .executeIfPresent(it -> {
        Log.d("ChimeSdkModule", "choose audio device " + it.getType());
        audioVideo.chooseAudioDevice(it);
      });
  }

  public void onOffAudio(boolean on) {
    if (audioVideo == null) {
      return;
    }
    if (on) {
      audioVideo.realtimeLocalUnmute();
    } else {
      audioVideo.realtimeLocalMute();
    }
  }

  public void onOffVideo() {
    if (audioVideo == null) {
      return;
    }
    if (isCameraLocalOn()) {
      audioVideo.stopLocalVideo();
    } else {
      audioVideo.startLocalVideo(cameraCaptureSource);
      cameraCaptureSource.start();
    }
  }

  public void switchCamera() {
    if (audioVideo == null) {
      return;
    }
    audioVideo.switchCamera();
  }

  private void cleanup() {
    currentRoster.clear();
    videoTiles.clear();
    videoTilesLive.postValue(new HashSet<>(Collections.emptyList()));
  }

  public VideoCollectionTile getShareVideoTile() {
    return shareVideoTile;
  }

  public void setShareVideoTile(VideoCollectionTile shareVideoTile) {
    this.shareVideoTile = shareVideoTile;
  }
}
