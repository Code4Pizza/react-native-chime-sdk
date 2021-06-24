package com.reactnativechimesdk;

import androidx.lifecycle.MutableLiveData;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession;
import com.annimon.stream.Stream;
import com.reactnativechimesdk.data.RosterAttendee;
import com.reactnativechimesdk.data.VideoCollectionTile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingModel {
  private static final MeetingModel ourInstance = new MeetingModel();

  public static MeetingModel getInstance() {
    return ourInstance;
  }

  private MeetingModel() {
  }

  private MeetingSession meetingSession;

  private String localId;
  private boolean isCameraOn;

  private final Map<String, RosterAttendee> currentRoster = new HashMap<>();

  private List<VideoCollectionTile> videoTiles = new ArrayList<>();
  private final MutableLiveData<List<VideoCollectionTile>> videoTilesLive = new MutableLiveData<>();

  public MutableLiveData<List<VideoCollectionTile>> getVideoTilesLive() {
    return videoTilesLive;
  }

  public void addVideoTile(VideoCollectionTile videoCollectionTile) {
    videoTiles.add(videoCollectionTile);
    videoTilesLive.postValue(videoTiles);
  }

  public void removeVideoTile(int tileId) {
    videoTiles = Stream.of(videoTiles).filter(v -> v.getVideoTileState().getTileId() != tileId).toList();
    videoTilesLive.postValue(videoTiles);
  }

  public void setMeetingSession(MeetingSession meetingSession) {
    this.meetingSession = meetingSession;
  }

  public AudioVideoFacade getAudioVideo() {
    if (meetingSession == null) return null;
    return meetingSession.getAudioVideo();
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

  public List<VideoCollectionTile> getVideoTiles() {
    return videoTiles;
  }

  public void startMeeting() {
    if (getAudioVideo() == null) return;
    getAudioVideo().start();
    getAudioVideo().startLocalVideo();
    getAudioVideo().startRemoteVideo();
  }

  public void endMeeting() {
    if (getAudioVideo() == null) return;
    getAudioVideo().stopLocalVideo();
    getAudioVideo().stopRemoteVideo();
    getAudioVideo().stop();
    currentRoster.clear();
    videoTiles.clear();
    videoTilesLive.postValue(Collections.emptyList());
  }

  public RosterAttendee getAttendee(String attendeeId) {
    return currentRoster.get(attendeeId);
  }

}
