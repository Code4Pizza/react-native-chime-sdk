package com.reactnativechimesdk

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.reactnativechimesdk.data.RosterAttendee
import com.reactnativechimesdk.utils.getAttendeeName

class EventEmitter {

  companion object {
    private const val MEETING_EVENT = "onChimeMeetingEvent"
    private const val KEY_EVENT = "event"
    private const val KEY_DES = "des"
    var KEY_USER_NAME = "userName"
    var KEY_USER_ID = "userID"
    var KEY_VIDEO_STATUS = "videoStatus"
    var KEY_AUDIO_STATUS = "audioStatus"
    var KEY_VIDEO_RATIO = "videoRatio"
    var KEY_IS_HOST = "isHost"
    var KEY_SHARE_STATUS = "shareStatus"

    private const val MEETING_STATE_CHANGE = "meetingStateChange"
    const val MEETING_USER_JOIN = "sinkMeetingUserJoin"
    const val MEETING_USER_LEFT = "sinkMeetingUserLeft"
    const val MEETING_ACTIVE_SHARE = "sinkMeetingActiveShare"
    const val MEETING_AUDIO_STATUS_CHANGE = "onSinkMeetingAudioStatusChange"
    const val MEETING_VIDEO_STATUS_CHANGE = "onSinkMeetingVideoStatusChange"

    fun sendMeetingStateEvent(context: ReactContext, des: String) {
      val map: WritableMap = WritableNativeMap()
      map.putString(KEY_EVENT, MEETING_STATE_CHANGE)
      map.putString(KEY_DES, des)
      sendEvent(context, map)
    }

    fun sendMeetingUserEvent(context: ReactContext, event: String, attendee: RosterAttendee) {
      val map: WritableMap = WritableNativeMap()
      map.putString(KEY_EVENT, event)
      map.putString(KEY_USER_ID, attendee.attendeeId)
      map.putString(KEY_USER_NAME, attendee.attendeeName)
      map.putBoolean(KEY_AUDIO_STATUS, attendee.isMuted)
      // map.putBoolean(KEY_VIDEO_STATUS, attendee.isCameraOn)
      sendEvent(context, map)
    }

    private fun sendEvent(context: ReactContext, params: WritableMap?) {
      context.getJSModule(RCTDeviceEventEmitter::class.java)
        .emit(MEETING_EVENT, params)
    }
  }
}
