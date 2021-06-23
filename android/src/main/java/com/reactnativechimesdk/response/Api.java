package com.reactnativechimesdk.response;

import android.util.Log;

import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse;
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse;
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.reactnativechimesdk.utils.Util.encodeURLParam;

public class Api {

  private static final String TAG = "Api";

  public static String requestCreateSession(String meetingUrl, String meetingId, String attendeeName) {
    HttpURLConnection urlConnection = null;
    try {
      URL serverUrl = new URL(meetingUrl + "join?" +
        "title=" + encodeURLParam(meetingId) +
        "&name=" + encodeURLParam(attendeeName) +
        "&region=" + encodeURLParam("us-east-1"));

      urlConnection = (HttpURLConnection) serverUrl.openConnection();
      urlConnection.setRequestMethod("POST");
      urlConnection.setDoInput(true);
      urlConnection.setDoOutput(true);

      StringBuilder response = new StringBuilder();
      BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      String inputLine = in.readLine();
      while (inputLine != null) {
        response.append(inputLine);
        inputLine = in.readLine();
      }
      in.close();

      if (urlConnection.getResponseCode() == 201) {
        return response.toString();
      } else {
        Log.e(TAG, "Unable to join meeting. Response code: " + urlConnection.getResponseCode());
      }
    } catch (IOException e) {
      Log.e(TAG, "There was an exception while joining the meeting: ", e);
    } finally {
      if (urlConnection != null)
        urlConnection.disconnect();
    }
    return null;
  }

  public static MeetingSessionConfiguration createSession(String response) {
    JoinMeetingResponse joinMeetingResponse = new Gson().fromJson(response, JoinMeetingResponse.class);
    return new MeetingSessionConfiguration(
      new CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
      new CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee));
  }

}
