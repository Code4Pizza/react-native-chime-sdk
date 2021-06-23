package com.reactnativechimesdk.data;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;

public class VideoCollectionTile {

  private String attendeeName;
  private VideoTileState videoTileState;

  public VideoCollectionTile(String attendeeName, VideoTileState videoTileState) {
    this.attendeeName = attendeeName;
    this.videoTileState = videoTileState;
  }

  public String getAttendeeName() {
    return attendeeName;
  }

  public void setAttendeeName(String attendeeName) {
    this.attendeeName = attendeeName;
  }

  public VideoTileState getVideoTileState() {
    return videoTileState;
  }

  public void setVideoTileState(VideoTileState videoTileState) {
    this.videoTileState = videoTileState;
  }
}
