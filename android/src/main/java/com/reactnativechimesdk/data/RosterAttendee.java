package com.reactnativechimesdk.data;

public class RosterAttendee {

  private String attendeeId;
  private String attendeeName;
  private boolean isMuted;

  public RosterAttendee(String attendeeId, String attendeeName) {
    this.attendeeId = attendeeId;
    this.attendeeName = attendeeName;
  }

  public RosterAttendee(String attendeeId, String attendeeName, boolean isMuted) {
    this.attendeeId = attendeeId;
    this.attendeeName = attendeeName;
    this.isMuted = isMuted;
  }

  public String getAttendeeId() {
    return attendeeId;
  }

  public void setAttendeeId(String attendeeId) {
    this.attendeeId = attendeeId;
  }

  public String getAttendeeName() {
    return attendeeName;
  }

  public void setAttendeeName(String attendeeName) {
    this.attendeeName = attendeeName;
  }

  public boolean isMuted() {
    return isMuted;
  }

  public void setMuted(boolean muted) {
    isMuted = muted;
  }
}
