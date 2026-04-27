package com.typingpractice.typing_practice_be.word.dto.response;

import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordProfile;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.WordTypingStats;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminWordResponse {
  private Long wordId;
  private String word;
  private WordLanguage language;
  private Float difficulty;
  private LocalDateTime createdAt;

  private ProfileInfo profile;
  private TypingStatsInfo typingStats;

  public static AdminWordResponse from(Word word) {
    AdminWordResponse response = new AdminWordResponse();

    response.wordId = word.getId();
    response.word = word.getWord();
    response.language = word.getLanguage();
    response.difficulty = word.getDifficulty();
    response.createdAt = word.getCreatedAt();
    response.profile = ProfileInfo.from(word.getProfile());

    if (word.getTypingStats() != null) {
      response.typingStats = TypingStatsInfo.from(word.getTypingStats());
    }

    return response;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class ProfileInfo {
    private int length;
    private float difficultySeed;
    private Float jamoComplex;
    private Float diphthongRate;
    private Float shiftJamoRate;
    private Float caseFlipRate;

    public static ProfileInfo from(WordProfile profile) {
      ProfileInfo info = new ProfileInfo();
      info.length = profile.getLength();
      info.difficultySeed = profile.getDifficultySeed();
      info.jamoComplex = profile.getJamoComplex();
      info.diphthongRate = profile.getDiphthongRate();
      info.shiftJamoRate = profile.getShiftJamoRate();
      info.caseFlipRate = profile.getCaseFlipRate();
      return info;
    }
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TypingStatsInfo {
    private int totalAttemptsCount;
    private int validAttemptsCount;
    private float avgTimeMs;
    private float correctRate;

    public static TypingStatsInfo from(WordTypingStats stats) {
      TypingStatsInfo info = new TypingStatsInfo();
      info.totalAttemptsCount = stats.getTotalAttemptsCount();
      info.validAttemptsCount = stats.getValidAttemptsCount();
      info.avgTimeMs = stats.getAvgTimeMs();
      info.correctRate = stats.getCorrectRate();
      return info;
    }
  }
}
