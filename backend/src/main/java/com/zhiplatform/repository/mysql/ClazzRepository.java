package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {

    List<Clazz> findByOwnerId(Long ownerId);
}