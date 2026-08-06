package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRoleOrderByIdAsc(String role);

    @Query("select u from User u where u.role = 'STUDENT' and u.clazzId = ?1")
    List<User> findStudentsByClazzId(Long clazzId);

    @Query("select u from User u where u.role = 'STUDENT' and (u.clazzId is null or u.clazzId <> ?1)")
    List<User> findStudentsExcludeClazz(Long clazzId);

    @Query("select u from User u where u.role = 'STUDENT'")
    List<User> findAllStudents();
}
