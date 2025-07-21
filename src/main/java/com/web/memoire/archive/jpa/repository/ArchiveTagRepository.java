package com.web.memoire.archive.jpa.repository;

import com.web.memoire.common.entity.TagEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveTagRepository extends JpaRepository<TagEntity, Integer> {

    // 태그id로 태그 조회
    @Query("SELECT t FROM TagEntity t WHERE t.tagid = :tagid")
    TagEntity findTagById(@Param("tagid") int tagid);

    // 태그name으로 조회
    @Query("SELECT t FROM TagEntity t WHERE LOWER(t.tagName) = LOWER(:tagName)")
    TagEntity findTagByName(@Param("tagName") String tagName);

    //키워드가 포함된 태그들 조회
    @Query("SELECT t FROM TagEntity t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.searchCount DESC, t.likeCount DESC")
    List<TagEntity> findTop20TagsWithKeyword(@Param("keyword") String keyword, Pageable pageable);

}
