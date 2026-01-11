package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import java.util.*;

public class MockMemberRepository implements MemberRepository {
  private final Map<Long, Member> store = new HashMap<>();
  private Long sequence = 1L;

  @Override
  public List<Member> findAll(MemberPaginationQuery request) {
    return new ArrayList<>(store.values());
  }

  @Override
  public Optional<Member> findById(Long memberId) {
    return Optional.ofNullable(store.get(memberId));
  }

  @Override
  public Optional<Member> findByProviderId(String providerId) {
    return store.values().stream()
        .filter(member -> member.getProviderId().equals(providerId))
        .findFirst();
  }

  @Override
  public Long save(Member member) {
    // ID가 없으면 새로 할당 (신규 저장)
    if (member.getId() == null) {
      // Reflection으로 ID 설정 (Member에 setter 없어서)
      try {
        var idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, sequence);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      store.put(sequence, member);
      return sequence++;
    }

    // ID가 있으면 업데이트
    store.put(member.getId(), member);
    return member.getId();
  }

  @Override
  public void deleteMember(Member member) {
    store.remove(member.getId());
  }

  public void clear() {
    store.clear();
    sequence = 1L;
  }
}
