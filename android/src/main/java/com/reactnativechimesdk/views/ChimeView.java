package com.reactnativechimesdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView;
import com.annimon.stream.Stream;
import com.facebook.react.bridge.ReactContext;
import com.reactnativechimesdk.MeetingModel;
import com.reactnativechimesdk.R;

public class ChimeView extends FrameLayout {

  private static final String TAG = "ChimeView";

  private DefaultVideoRenderView renderView;
  private String attendeeId;
  private boolean viewVisible, bindSuccess;

  public ChimeView(@NonNull Context context) {
    this(context, null);
  }

  public ChimeView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ChimeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    LifecycleOwner owner = (AppCompatActivity) ((ReactContext) context).getCurrentActivity();
    assert owner != null;
    MeetingModel.meetingModel().getVideoTilesLive().observe(owner, videoCollectionTiles -> bind());
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    renderView = findViewById(R.id.videoView);
  }

  public void bindVideoTile(String attendeeId) {
    viewVisible = !attendeeId.isEmpty();
    if (viewVisible) {
      this.attendeeId = attendeeId;
      bind();
    } else {
      unbind();
    }
  }

  private void bind() {
    Stream.of(MeetingModel.meetingModel().getVideoTiles())
      .filter(it -> it.getVideoTileState().getAttendeeId().equals(ChimeView.this.attendeeId))
      .findSingle()
      .executeIfAbsent(() -> {
        setVisibility(GONE);
      })
      .executeIfPresent(v -> {
        if (viewVisible) {
          setVisibility(VISIBLE);
          MeetingModel.meetingModel().getAudioVideo().bindVideoView(renderView, v.getVideoTileState().getTileId());
        }
      });
  }

  private void unbind() {
    setVisibility(GONE);
  }
}
