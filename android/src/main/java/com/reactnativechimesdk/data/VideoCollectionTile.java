package com.reactnativechimesdk.data;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;

import java.util.Objects;

public class VideoCollectionTile {

  private final VideoTileState videoTileState;

  public VideoCollectionTile(VideoTileState videoTileState) {
    this.videoTileState = videoTileState;
  }

  public VideoTileState getVideoTileState() {
    return videoTileState;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof VideoCollectionTile)) {
      return false;
    }
    VideoCollectionTile that = (VideoCollectionTile) o;
    return videoTileState.getAttendeeId().equals(that.videoTileState.getAttendeeId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(videoTileState.getAttendeeId());
  }
}
