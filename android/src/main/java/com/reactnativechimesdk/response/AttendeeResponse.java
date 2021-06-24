package com.reactnativechimesdk.response;

import com.amazonaws.services.chime.sdk.meetings.session.Attendee;
import com.google.gson.annotations.SerializedName;

public class AttendeeResponse {
  @SerializedName("Attendee")
  public Attendee attendee;
}
