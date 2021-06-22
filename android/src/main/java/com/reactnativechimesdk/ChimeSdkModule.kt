package com.reactnativechimesdk

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.session.*
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.google.gson.Gson
import com.reactnativechimesdk.EventEmitter.Companion.KEY_AUDIO_STATUS
import com.reactnativechimesdk.EventEmitter.Companion.KEY_USER_ID
import com.reactnativechimesdk.EventEmitter.Companion.KEY_USER_NAME
import com.reactnativechimesdk.EventEmitter.Companion.MEETING_AUDIO_STATUS_CHANGE
import com.reactnativechimesdk.EventEmitter.Companion.MEETING_USER_JOIN
import com.reactnativechimesdk.EventEmitter.Companion.MEETING_USER_LEFT
import com.reactnativechimesdk.EventEmitter.Companion.sendMeetingStateEvent
import com.reactnativechimesdk.EventEmitter.Companion.sendMeetingUserEvent
import com.reactnativechimesdk.data.JoinMeetingResponse
import com.reactnativechimesdk.data.RosterAttendee
import com.reactnativechimesdk.data.VideoCollectionTile
import com.reactnativechimesdk.itf.SimpleAudioVideoObserver
import com.reactnativechimesdk.itf.SimpleRealTimeObserver
import com.reactnativechimesdk.itf.SimpleVideoTileObserver
import com.reactnativechimesdk.utils.encodeURLParam
import com.reactnativechimesdk.utils.getAttendeeName
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@ReactModule(name = ChimeSdkModule.NAME)
class ChimeSdkModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext),
  LifecycleEventListener,
  SimpleRealTimeObserver,
  SimpleAudioVideoObserver,
  SimpleVideoTileObserver {

  companion object {
    const val TAG = "ChimeSdkModule"
    const val NAME = "ChimeSdk"
    const val WEBRTC_PERMISSION_REQUEST_CODE = 1
    const val MEETING_REGION = "us-east-1"
  }

  private val logger = ConsoleLogger(LogLevel.INFO)
  private val gson = Gson()
  private val uiScope = CoroutineScope(Dispatchers.Main)
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

  private val WEBRTC_PERM = arrayOf(
    Manifest.permission.MODIFY_AUDIO_SETTINGS,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA
  )

  private val filter: IntentFilter = object : IntentFilter() {
    init {
      addAction("onConfigurationChanged")
      addAction("onRequestPermissionsResult")
    }
  }

  init {
    reactContext.addLifecycleEventListener(this@ChimeSdkModule)
  }

  private lateinit var meetingUrl: String
  private lateinit var meetingId: String
  private lateinit var selfAttendeeName: String

  override fun getName(): String = NAME

  @ReactMethod
  private fun showToast(msg: String) {
    Toast.makeText(
      reactApplicationContext,
      msg,
      Toast.LENGTH_LONG
    ).show()
  }

  @ReactMethod
  fun multiply(a: Int, b: Int, promise: Promise) {
    promise.resolve(a * b)
  }

  @ReactMethod
  fun joinMeeting(map: ReadableMap) {
    meetingUrl = map.getString("meetingUrl").toString()
    meetingId = map.getString("meetingId").toString()
    selfAttendeeName = map.getString("attendeeName").toString()

    if (hasPermissionsAlready()) {
      joinMeeting()
    } else {
      reactContext.currentActivity?.let {
        ActivityCompat.requestPermissions(
          it,
          WEBRTC_PERM,
          WEBRTC_PERMISSION_REQUEST_CODE
        )
      }
    }
  }

  private fun hasPermissionsAlready(): Boolean {
    return WEBRTC_PERM.all {
      reactContext.currentActivity?.let { it1 ->
        ContextCompat.checkSelfPermission(
          it1,
          it
        )
      } == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun joinMeeting() {
    uiScope.launch {
      val meetingResponseJson: String? = requestJoin()
      createSession(meetingResponseJson)
    }
  }

  private suspend fun requestJoin(): String? {
    return withContext(ioDispatcher) {
      val serverUrl =
        URL(
          "${meetingUrl}join?title=${encodeURLParam(meetingId)}" +
            "&name=${encodeURLParam(selfAttendeeName)}" +
            "&region=${encodeURLParam(MEETING_REGION)}"
        )

      try {
        val response = StringBuffer()
        with(serverUrl.openConnection() as HttpURLConnection) {
          requestMethod = "POST"
          doInput = true
          doOutput = true

          BufferedReader(InputStreamReader(inputStream)).use {
            var inputLine = it.readLine()
            while (inputLine != null) {
              response.append(inputLine)
              inputLine = it.readLine()
            }
            it.close()
          }

          if (responseCode == 201) {
            response.toString()
          } else {
            logger.error(TAG, "Unable to join meeting. Response code: $responseCode")
            null
          }
        }
      } catch (exception: Exception) {
        logger.error(TAG, "There was an exception while joining the meeting: $exception")
        null
      }
    }
  }

  private fun createSession(meetingResponseJson: String?) {
    if (meetingResponseJson == null) {
      showToast("Error no meeting response")
    } else {
      val sessionConfig = createSessionConfiguration(meetingResponseJson)
      val meetingSession = sessionConfig?.let {
        DefaultMeetingSession(
          it,
          logger,
          reactApplicationContext,
          // Note if the following isn't provided app will (as expected) crash if we use custom video source
          // since an EglCoreFactory will be internal created and will be using a different shared EGLContext.
          // However the internal default capture would work fine, since it is initialized using
          // that internally created default EglCoreFactory, and can be smoke tested by removing this
          // argument and toggling use of custom video source before starting video
          DefaultEglCoreFactory()
        )
      }

      if (meetingSession == null) {
        showToast("Error null meeting session")
        return
      }

      MeetingSessionModel.setMeetingSession(meetingSession)
      MeetingSessionModel.audioVideo.apply {
        addRealtimeObserver(this@ChimeSdkModule)
        addAudioVideoObserver(this@ChimeSdkModule)
        addVideoTileObserver(this@ChimeSdkModule)
        start()
        startLocalVideo()
        startRemoteVideo()
      }
    }
  }

  private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
    if (response.isNullOrBlank()) return null

    return try {
      val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
      MeetingSessionConfiguration(
        CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
        CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
        ::urlRewriter
      )
    } catch (exception: Exception) {
      logger.error(
        TAG,
        "Error creating session configuration: ${exception.localizedMessage}"
      )
      null
    }
  }

  private fun urlRewriter(url: String): String {
    // You can change urls by url.replace("example.com", "my.example.com")
    return url
  }

  @ReactMethod
  fun leaveCurrentMeeting() {
    sendMeetingStateEvent(reactContext, "meeting_end")
    Handler().postDelayed({
      sendMeetingStateEvent(reactContext, "idle")
    }, 1500);
    endMeeting()
  }

  private fun endMeeting() {
    MeetingSessionModel.audioVideo.apply {
      stopLocalVideo()
      stopRemoteVideo()
      stop()
    }
    MeetingSessionModel.currentRoster.clear()
  }

  @ReactMethod
  fun getParticipants(callback: Callback) {
    val array: WritableArray = WritableNativeArray()
    MeetingSessionModel.currentRoster.values.forEach {
      val map: WritableMap = WritableNativeMap()
      map.putString(KEY_USER_ID, it.attendeeId)
      map.putString(KEY_USER_NAME, it.attendeeName)
      // map.putBoolean(KEY_VIDEO_STATUS, it.isCameraOn)
      map.putBoolean(KEY_AUDIO_STATUS, it.isMuted)
      array.pushMap(map)
    }
    callback.invoke(array)
  }

  @ReactMethod
  fun getUserInfo(attendeeId: String, callback: Callback) {
    MeetingSessionModel.currentRoster.values.find { it.attendeeId == attendeeId }?.let {
      val map: WritableMap = WritableNativeMap()
      map.putString(KEY_USER_ID, it.attendeeId)
      map.putString(KEY_USER_NAME, it.attendeeName)
      // map.putBoolean(KEY_VIDEO_STATUS, it.isCameraOn)
      map.putBoolean(KEY_AUDIO_STATUS, it.isMuted)
      callback.invoke(map)
    }
  }

  @ReactMethod
  fun onMyAudio() {
    MeetingSessionModel.audioVideo.realtimeLocalMute()
  }

  @ReactMethod
  fun offMyAudio() {
    MeetingSessionModel.audioVideo.realtimeLocalUnmute()
  }

  @ReactMethod
  fun onOffMyVideo() {
    if (MeetingSessionModel.isCameraOn) {
      MeetingSessionModel.audioVideo.stopLocalVideo()
    } else {
      MeetingSessionModel.audioVideo.startLocalVideo()
    }
  }

  @ReactMethod
  fun switchMyCamera() {
  }

  override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
    super.onAttendeesMuted(attendeeInfo)
    attendeeInfo.forEach { attendee ->
      MeetingSessionModel.currentRoster[attendee.attendeeId]?.let {
        MeetingSessionModel.currentRoster[it.attendeeId] =
          RosterAttendee(
            it.attendeeId,
            it.attendeeName,
            it.volumeLevel,
            it.signalStrength,
            it.isActiveSpeaker,
            true
          )
        sendMeetingUserEvent(reactContext, MEETING_AUDIO_STATUS_CHANGE, it)
      }
    }
  }

  override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
    super.onAttendeesUnmuted(attendeeInfo)
    attendeeInfo.forEach { attendee ->
      MeetingSessionModel.currentRoster[attendee.attendeeId]?.let {
        MeetingSessionModel.currentRoster[it.attendeeId] =
          RosterAttendee(
            it.attendeeId,
            it.attendeeName,
            it.volumeLevel,
            it.signalStrength,
            it.isActiveSpeaker,
            false
          )
        sendMeetingUserEvent(reactContext, MEETING_AUDIO_STATUS_CHANGE, it)
      }
    }
  }

  override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
    super.onAttendeesJoined(attendeeInfo)
    attendeeInfo.forEach {
      MeetingSessionModel.currentRoster.getOrPut(
        it.attendeeId,
        { RosterAttendee(it.attendeeId, getAttendeeName(it.attendeeId, it.externalUserId)) })

      if (MeetingSessionModel.localVideoTileState?.videoTileState?.attendeeId == it.attendeeId) {
        sendMeetingStateEvent(reactContext, "meeting_ready")
      } else {
        sendMeetingUserEvent(reactContext, MEETING_USER_JOIN, MeetingSessionModel.currentRoster[it.attendeeId]!!)
      }
    }
  }

  override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
    super.onAttendeesLeft(attendeeInfo)
    attendeeInfo.forEach {
      MeetingSessionModel.currentRoster[it.attendeeId]?.let { attendee ->
        sendMeetingUserEvent(reactContext, MEETING_USER_LEFT, attendee)
      }
      MeetingSessionModel.currentRoster.remove(
        it.attendeeId
      )
    }
  }

  override fun onAudioSessionStarted(reconnecting: Boolean) {
    super.onAudioSessionStarted(reconnecting)
    val success = MeetingSessionModel.audioVideo.realtimeSetVoiceFocusEnabled(true)
    if (success) {
      Log.d(TAG, "Voice Focus enabled")
    } else {
      Log.d(TAG, "Failed to enable Voice Focus")
    }
  }

  override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
    super.onAudioSessionStopped(sessionStatus)
  }

  override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
    super.onVideoSessionStarted(sessionStatus)
  }

  override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
    super.onVideoSessionStopped(sessionStatus)
  }

  override fun onVideoTileAdded(tileState: VideoTileState) {
    super.onVideoTileAdded(tileState)
    uiScope.launch {
      val videoCollectionTile = createVideoCollectionTile(tileState)
      if (tileState.isLocalTile) {
        MeetingSessionModel.localAttendeeId = tileState.attendeeId
        MeetingSessionModel.isCameraOn = true
        MeetingSessionModel.localVideoTileState = videoCollectionTile
      } else {
        MeetingSessionModel.addRemoteVideoTile(videoCollectionTile)
      }
    }
  }

  private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
    val attendeeId = tileState.attendeeId
    val attendeeName = MeetingSessionModel.currentRoster[attendeeId]?.attendeeName ?: ""
    return VideoCollectionTile(attendeeName, tileState)
  }

  override fun onVideoTileRemoved(tileState: VideoTileState) {
    super.onVideoTileRemoved(tileState)
    uiScope.launch {
      if (tileState.isLocalTile) {
        MeetingSessionModel.localVideoTileState = null
        MeetingSessionModel.isCameraOn = false
      } else {
        MeetingSessionModel.removeRemoteVideoTile(tileState.tileId)
      }
    }
  }

  override fun onHostResume() {
    reactContext.registerReceiver(moduleConfigReceiver, filter)
  }

  override fun onHostPause() {
    // Ignore
  }

  override fun onHostDestroy() {
    reactContext.unregisterReceiver(moduleConfigReceiver)
  }

  private val moduleConfigReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if ("onConfigurationChanged" == intent.action) {
        val configuration = intent.getParcelableExtra<Configuration>("newConfig")
        if (configuration != null) {
          // refreshRotation()
        }
      } else if ("onRequestPermissionsResult" == intent.action) {
        val permissions = intent.getStringArrayExtra("permissions")
        val grantResults = intent.getIntArrayExtra("grantResults")
        if (permissions == null || grantResults == null) {
          return
        }
        joinMeeting()
      }
    }
  }
}
