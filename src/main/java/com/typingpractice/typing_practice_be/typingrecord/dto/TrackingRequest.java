package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.domain.DeviceType;
import lombok.Getter;

@Getter
public class TrackingRequest {
  private String sessionId;
  private String referrer;
  private DeviceType deviceType;
}
