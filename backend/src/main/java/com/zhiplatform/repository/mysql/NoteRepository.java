package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Note> findByUserIdAndCategory(Long userId, String category);
}
