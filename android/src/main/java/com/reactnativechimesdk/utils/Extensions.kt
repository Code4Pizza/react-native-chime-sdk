/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactnativechimesdk.utils

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import java.net.URLEncoder

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun encodeURLParam(string: String?): String {
    return URLEncoder.encode(string, "utf-8")
}

fun isLandscapeMode(context: Context?): Boolean {
    return context?.let { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE } ?: false
}

// Append to attendee name if it's for content share
private const val CONTENT_NAME_SUFFIX = "<<Content>>"

fun getAttendeeName(attendeeId: String, externalUserId: String): String {
  if (!externalUserId.contains("#")) {
    return externalUserId;
  }
  val attendeeName = externalUserId.split('#')[1]

  return if (DefaultModality(attendeeId).hasModality(ModalityType.Content)) {
    "$attendeeName $CONTENT_NAME_SUFFIX"
  } else {
    attendeeName
  }
}

