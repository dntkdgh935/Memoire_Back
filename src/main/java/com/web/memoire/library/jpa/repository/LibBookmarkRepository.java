package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.BookmarkEntity;
import com.web.memoire.common.entity.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibBookmarkRepository extends JpaRepository<BookmarkEntity, BookmarkId> {

    BookmarkEntity findByUseridAndCollectionid(String userId, String collectionId);

    // 유저가 북마크한 전체 컬렉션 조회
    @Query(value = "SELECT b FROM BookmarkEntity b where b.userid = :userid")
    List<BookmarkEntity> findAllUserBookmarks(@Param("userid") String userid);

    // 컬렉션의 북마크 수 조회
    @Query(value = "SELECT count(b) from BookmarkEntity b where b.collectionid = :collectionid")
    int countCollectionBookmarks(@Param("collectionid") String collectionid);

    // 컬렉션의 북마크 상세 조회
    @Query(value = "SELECT b from BookmarkEntity b where b.collectionid = :collectionid")
    List<BookmarkEntity> findAllCollectionBookmarks(@Param("collectionid") String collectionid);

}