package com.typingpractice.typing_practice_be.typingrecord.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingInfo {
  private String sessionId;
  private String referrer;
  private DeviceType deviceType;

  public static TrackingInfo create(String sessionId, String referer, DeviceType deviceType) {
    TrackingInfo info = new TrackingInfo();
    info.sessionId = sessionId;
    info.referrer = referer;
    info.deviceType = deviceType;

    return info;
  }
}
