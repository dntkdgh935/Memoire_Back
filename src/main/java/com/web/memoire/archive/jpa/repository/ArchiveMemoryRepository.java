package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveMemoryRepository extends JpaRepository<MemoryEntity, Integer> {

    // 유저가 선택한 컬렉션의 메모리 전체 조회
    @Query(value = "SELECT m FROM MemoryEntity m JOIN CollectionEntity c ON m.collectionid = c.collectionid WHERE m.collectionid = :collectionid AND c.authorid = :userid")
    List<MemoryEntity> findAllUserMemories(@Param("userid") String userid, @Param("collectionid") int collectionid);

    // 유저가 작성한 메모리 총 개수 조회
    @Query(value = "SELECT count(m) FROM MemoryEntity m JOIN CollectionEntity c ON m.collectionid = c.collectionid WHERE c.authorid = :userid")
    int countAllMemoriesByUserId(@Param("userid") String userid);

    // collectionid로 memoryOrder가 1인 메모리 조회
    @Query(value = "SELECT m FROM MemoryEntity m WHERE m.collectionid = :collectionid AND m.memoryOrder = 1")
    MemoryEntity findFirstMemoryByCollectionId(@Param("collectionid") int collectionid);


}
