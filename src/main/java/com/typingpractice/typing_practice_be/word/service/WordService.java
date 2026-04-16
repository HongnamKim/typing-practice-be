package com.typingpractice.typing_practice_be.word.service;

import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordProfile;
import com.typingpractice.typing_practice_be.word.exception.WordNotFoundException;
import com.typingpractice.typing_practice_be.word.repository.WordRepository;
import java.util.*;

import com.typingpractice.typing_practice_be.word.service.difficulty.WordDifficultySeedCalculator;
import com.typingpractice.typing_practice_be.word.service.difficulty.WordProfileCalculator;
import com.typingpractice.typing_practice_be.word.statistics.domain.GlobalWordStatistics;
import com.typingpractice.typing_practice_be.word.statistics.service.GlobalWordStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {
  private final WordIdCacheService wordIdCacheService;
  private final WordRepository wordRepository;
  private final WordLanguageValidator validator;

  private final WordProfileCalculator profileCalculator;
  private final WordDifficultySeedCalculator seedCalculator;
  private final GlobalWordStatisticsService globalWordStatisticsService;

  public List<Word> findWords(WordLanguage language, WordDifficultyTier tier, int count) {
    List<Long> ids = wordIdCacheService.getIdsByTier(language, tier);

    List<Long> shuffled = new ArrayList<>(ids);
    Collections.shuffle(shuffled);

    List<Long> selectedIds = shuffled.subList(0, Math.min(count, shuffled.size()));

    List<Word> fetched = wordRepository.findByIds(selectedIds, language);
    Collections.shuffle(fetched);

    return fetched;
  }

  @Transactional
  public Word createWord(String word, WordLanguage language) {
    validator.validate(word, language);

    Word w = Word.create(word, language);

    // 난이도 계산
    WordProfile profile = profileCalculator.calculate(word, language);
    GlobalWordStatistics stats = globalWordStatisticsService.findByLanguage(language);
    float seed = seedCalculator.calculate(profile, stats, language);
    profile.setDifficultySeed(seed);

    w.updateProfile(profile);
    w.updateDifficulty(seed);

    wordRepository.save(w);
    wordIdCacheService.add(language, w.getId(), seed);
    return w;
  }

  @Transactional
  public Word updateWord(Long id, String word) {
    Word w = wordRepository.findById(id).orElseThrow(WordNotFoundException::new);
    w.updateWord(word);

    return w;
  }

  @Transactional
  public void deleteWord(Long id) {
    Word w = wordRepository.findById(id).orElseThrow(WordNotFoundException::new);
    wordIdCacheService.remove(w.getLanguage(), w.getId());

    wordRepository.deleteWord(w);
  }
}
