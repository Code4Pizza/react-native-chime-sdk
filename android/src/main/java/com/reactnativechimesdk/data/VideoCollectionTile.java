package com.reactnativechimesdk.data;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState;

public class VideoCollectionTile {

  private final VideoTileState videoTileState;

  public VideoCollectionTile(VideoTileState videoTileState) {
    this.videoTileState = videoTileState;
  }

  public VideoTileState getVideoTileState() {
    return videoTileState;
  }
}
