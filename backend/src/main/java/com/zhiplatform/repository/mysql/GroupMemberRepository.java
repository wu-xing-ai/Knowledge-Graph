package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByUserId(Long userId);

    List<GroupMember> findByGroupIdIn(List<Long> groupIds);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);
}