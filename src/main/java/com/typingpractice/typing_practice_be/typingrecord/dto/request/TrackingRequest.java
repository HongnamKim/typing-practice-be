package com.typingpractice.typing_practice_be.typingrecord.dto.request;

import com.typingpractice.typing_practice_be.typingrecord.domain.DeviceType;
import lombok.Getter;

@Getter
public class TrackingRequest {
  private String sessionId;
  private String referrer;
  private DeviceType deviceType;

  public static TrackingRequest create(String sessionId, String referrer, DeviceType deviceType) {
    TrackingRequest request = new TrackingRequest();
    request.sessionId = sessionId;
    request.referrer = referrer;
    request.deviceType = deviceType;

    return request;
  }
}
