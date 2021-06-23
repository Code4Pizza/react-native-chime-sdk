package com.reactnativechimesdk.response;

import com.google.gson.annotations.SerializedName;

public class MeetingInfo {
  @SerializedName("Meeting")
  MeetingResponse meetingResponse;

  @SerializedName("Attendee")
  AttendeeResponse attendeeResponse;
}
