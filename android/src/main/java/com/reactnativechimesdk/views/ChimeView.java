package com.reactnativechimesdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.facebook.react.bridge.ReactContext;
import com.reactnativechimesdk.MeetingModel;
import com.reactnativechimesdk.R;
import com.reactnativechimesdk.data.VideoCollectionTile;

import java.util.List;

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

    LifecycleOwner owner = (AppCompatActivity)((ReactContext) context).getCurrentActivity();
    assert owner != null;
    MeetingModel.getInstance().getVideoTilesLive().observe(owner, videoCollectionTiles -> {
      Stream.of(videoCollectionTiles).forEach(v -> {
        if (v.getVideoTileState().getAttendeeId().equals(ChimeView.this.attendeeId) && !bindSuccess && viewVisible) {
          Log.d(TAG, "observer bind remote video " + v.getVideoTileState().getTileId());
          MeetingModel.getInstance().getAudioVideo().bindVideoView(renderView, v.getVideoTileState().getTileId());
        }
      });
    });
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
    Stream.of(MeetingModel.getInstance().getVideoTiles()).forEach(it -> {
      if (it.getVideoTileState().getAttendeeId().equals(attendeeId)) {
        if (it.getVideoTileState().getTileId() == 0) {
          Log.d(TAG, "bind local video " + it.getVideoTileState().getTileId());
        } else {
          Log.d(TAG, "bind remote video " + it.getVideoTileState().getTileId());
        }
        Log.d(TAG, "========");
        bindSuccess = true;
        MeetingModel.getInstance().getAudioVideo().bindVideoView(renderView, it.getVideoTileState().getTileId());
      }
    });
    if (!bindSuccess) {
      Log.d(TAG, "Video tile is not available on attendee: " + attendeeId);
    }
  }

  private void unbind() {
    Stream.of(MeetingModel.getInstance().getVideoTiles()).forEach(it -> {
      if (it.getVideoTileState().getAttendeeId().equals(attendeeId)) {
        MeetingModel.getInstance().getAudioVideo().unbindVideoView(it.getVideoTileState().getTileId());
      }
    });
  }
}
