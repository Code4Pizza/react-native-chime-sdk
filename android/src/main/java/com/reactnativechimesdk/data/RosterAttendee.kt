/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactnativechimesdk.data

import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState

data class RosterAttendee(
  val attendeeId: String,
  val attendeeName: String,
  val volumeLevel: VolumeLevel = VolumeLevel.NotSpeaking,
  val signalStrength: SignalStrength = SignalStrength.High,
  val isActiveSpeaker: Boolean = false,
  var isMuted: Boolean = false
)
