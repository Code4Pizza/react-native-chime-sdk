package com.reactnativechimesdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView;
import com.reactnativechimesdk.R;
import com.reactnativechimesdk.data.VideoCollectionTile;

import static com.reactnativechimesdk.MeetingModel.meetingModel;

public class ChimeShareView extends FrameLayout {

  private static final String TAG = "ChimeShareView";

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
    DefaultVideoRenderView renderView = findViewById(R.id.videoView);
    VideoCollectionTile shareTile = meetingModel().getShareVideoTile();
    if (shareTile != null) {
      meetingModel().getAudioVideo().bindVideoView(renderView, shareTile.getVideoTileState().getTileId());
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    Log.d(TAG, "onDetachedFromWindow: ");
    VideoCollectionTile shareTile = meetingModel().getShareVideoTile();
    if (shareTile != null) {
      meetingModel().getAudioVideo().unbindVideoView(shareTile.getVideoTileState().getTileId());
    }
  }
}
