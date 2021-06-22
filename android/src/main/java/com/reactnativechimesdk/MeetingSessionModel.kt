package com.reactnativechimesdk

import androidx.lifecycle.MutableLiveData
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession
import com.reactnativechimesdk.data.RosterAttendee
import com.reactnativechimesdk.data.VideoCollectionTile

object MeetingSessionModel {
  private lateinit var meetingSession: MeetingSession

  fun setMeetingSession(meetingSession: MeetingSession) {
    this.meetingSession = meetingSession
  }

  val audioVideo: AudioVideoFacade
    get() = meetingSession.audioVideo

  var localAttendeeId: String? = null

  val currentRoster = mutableMapOf<String, RosterAttendee>()
  var localVideoTileState: VideoCollectionTile? = null
  var isCameraOn: Boolean = false

  val remoteVideoTileStates = mutableListOf<VideoCollectionTile>()
  private val remoteVideoTile: MutableLiveData<MutableList<VideoCollectionTile>> = MutableLiveData()

  fun addRemoteVideoTile(videoCollectionTile: VideoCollectionTile) {
    remoteVideoTileStates.add(videoCollectionTile)
    remoteVideoTile.value = remoteVideoTileStates
  }

  fun removeRemoteVideoTile(tileId: Int) {
    remoteVideoTileStates.removeAll { it.videoTileState.tileId == tileId }
    remoteVideoTile.value = remoteVideoTileStates
  }

  fun getRemoteVideosLiveData() : MutableLiveData<MutableList<VideoCollectionTile>> {
    return remoteVideoTile
  }
}
