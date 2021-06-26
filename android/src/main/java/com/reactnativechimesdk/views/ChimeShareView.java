package com.reactnativechimesdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView;
import com.annimon.stream.Stream;
import com.reactnativechimesdk.MeetingModel;
import com.reactnativechimesdk.R;

public class ChimeShareView extends FrameLayout {

  private DefaultVideoRenderView renderView;
  private String attendeeId;

  public ChimeShareView(@NonNull Context context) {
    super(context);
  }

  public ChimeShareView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ChimeShareView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    renderView = findViewById(R.id.videoView);
  }

  public void bindVideoTile(String attendeeId) {
    boolean viewVisible = !attendeeId.isEmpty();
    if (viewVisible) {
      this.attendeeId = attendeeId;
      bind();
    } else {
      unbind();
    }
  }

  private void bind() {
    Stream.of(MeetingModel.meetingModel().getVideoTiles())
      .filter(it -> it.getVideoTileState().getAttendeeId().equals(ChimeShareView.this.attendeeId))
      .findSingle()
      .executeIfPresent(v -> {
        MeetingModel.meetingModel().getAudioVideo().bindVideoView(renderView, v.getVideoTileState().getTileId());
      });
  }

  private void unbind() {
    Stream.of(MeetingModel.meetingModel().getVideoTiles())
      .filter(it -> it.getVideoTileState().getAttendeeId().equals(ChimeShareView.this.attendeeId))
      .findSingle()
      .executeIfPresent(v -> MeetingModel.meetingModel().getAudioVideo().unbindVideoView(v.getVideoTileState().getTileId()));
  }

}
