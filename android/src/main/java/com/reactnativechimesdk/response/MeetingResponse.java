package com.reactnativechimesdk.response;

import com.amazonaws.services.chime.sdk.meetings.session.Meeting;
import com.google.gson.annotations.SerializedName;

public class MeetingResponse {
  @SerializedName("Meeting")
  public Meeting meeting;
}
