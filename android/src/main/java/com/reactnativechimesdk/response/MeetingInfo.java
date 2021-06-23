package com.reactnativechimesdk.response;

import com.google.gson.annotations.SerializedName;

public class MeetingInfo {
  @SerializedName("Meeting")
  public MeetingResponse meetingResponse;

  @SerializedName("Attendee")
  public AttendeeResponse attendeeResponse;
}
