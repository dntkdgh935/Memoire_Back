package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LibTagRepository extends JpaRepository<TagEntity, Integer> {
    @Query(value = """
    SELECT *
    FROM (
        SELECT *
        FROM TB_TAG
        ORDER BY LIKE_COUNT + SEARCH_COUNT DESC
    )
    WHERE ROWNUM <= 5
    """, nativeQuery = true)
    List<TagEntity> findTop5TagsByRownum();

    TagEntity findByTagid(int tagid);

    TagEntity findByTagName(String query);

    @Query(value = "SELECT * FROM (SELECT t.* FROM TB_TAG t ORDER BY (t.SEARCH_COUNT + t.LIKE_COUNT) DESC) WHERE ROWNUM <= 5", nativeQuery = true)
    List<TagEntity> findTop5BySearchCountPlusLikeCount();
}