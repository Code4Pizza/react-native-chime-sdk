package com.reactnativechimesdk.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Util {

  public static String encodeURLParam(String string) throws UnsupportedEncodingException {
    return URLEncoder.encode(string, "utf-8");
  }

  public static String getAttendeeName(String attendeeId, String externalUserId) {
    if (!externalUserId.contains("#")) {
      return externalUserId;
    }
    return externalUserId.split("#")[1];
  }
}
