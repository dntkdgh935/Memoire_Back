package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminMemoryRepository extends JpaRepository<MemoryEntity, Integer> {

    // 메모리 조회
    MemoryEntity findByMemoryid(int memoryid);
}
