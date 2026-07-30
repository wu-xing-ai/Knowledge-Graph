package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByCreateBy(Long createBy);
}
