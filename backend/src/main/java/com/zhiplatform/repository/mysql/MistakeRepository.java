package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Mistake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MistakeRepository extends JpaRepository<Mistake, Long> {

    List<Mistake> findByUserIdOrderByLastTimeDesc(Long userId);

    Optional<Mistake> findByUserIdAndExerciseId(Long userId, Long exerciseId);
}
