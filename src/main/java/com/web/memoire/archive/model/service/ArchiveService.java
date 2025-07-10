package com.web.memoire.archive.model.service;

import com.web.memoire.archive.jpa.repository.*;
import com.web.memoire.common.dto.*;
import com.web.memoire.common.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    @Autowired
    private final ArchiveBookmarkRepository archiveBookmarkRepository;
    @Autowired
    private final ArchiveCollectionRepository archiveCollectionRepository;
    @Autowired
    private final ArchiveLikeRepository archiveLikeRepository;
    @Autowired
    private final ArchiveMemoryRepository archiveMemoryRepository;
    @Autowired
    private final ArchiveRelationshipRepository archiveRelationshipRepository;

    // ArchiveBookmarkRepository
    public ArrayList<Bookmark> findAllUserBookmarks(String userid) {
        List<BookmarkEntity> entityList = archiveBookmarkRepository.findAllUserBookmarks(userid);
        ArrayList<Bookmark> list = new ArrayList<>();
        for (BookmarkEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countCollectionBookmarks(String collectionid) {
        return archiveBookmarkRepository.countCollectionBookmarks(collectionid);
    }

    public ArrayList<Bookmark> findAllCollectionBookmarks(String collectionid) {
        List<BookmarkEntity> entityList = archiveBookmarkRepository.findAllCollectionBookmarks(collectionid);
        ArrayList<Bookmark> list = new ArrayList<>();
        for (BookmarkEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    // ArchiveCollectionRepository
    public ArrayList<Collection> findAllUserCollections(String userid) {
        List<CollectionEntity> entityList = archiveCollectionRepository.findAllUserCollections(userid);
        ArrayList<Collection> list = new ArrayList<>();
        for (CollectionEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countAllCollectionsByUserId(String userid) {
        return archiveCollectionRepository.countAllCollectionsByUserId(userid);
    }

    // ArchiveCollectionTagRepository

    // ArchiveLikeRepository
    public ArrayList<Like> findAllUserLikes(String userid) {
        List<LikeEntity> entityList = archiveLikeRepository.findAllUserLikes(userid);
        ArrayList<Like> list = new ArrayList<>();
        for (LikeEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countCollectionLikes(String collectionid) {
        return archiveLikeRepository.countCollectionLikes(collectionid);
    }

    public ArrayList<Like> findAllCollectionLikes(String collectionid) {
        List<LikeEntity> entityList = archiveLikeRepository.findAllCollectionLikes(collectionid);
        ArrayList<Like> list = new ArrayList<>();
        for (LikeEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    // ArchiveMemoryRepository
    public ArrayList<Memory> findAllUserMemories(String userid, String collectionid) {
        List<MemoryEntity> entityList = archiveMemoryRepository.findAllUserMemories(userid, collectionid);
        ArrayList<Memory> list = new ArrayList<>();
        for (MemoryEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countAllMemoriesByUserId(String userid) {
        return archiveMemoryRepository.countAllMemoriesByUserId(userid);
    }

    // ArchiveRelationshipRepository
    public ArrayList<Relationship> findAllUserFollowing(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserFollowing(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countUserFollowing(String userid) {
        return archiveRelationshipRepository.countAllFollowingByUserId(userid);
    }

    public ArrayList<Relationship> findAllUserFollower(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserFollower(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public int countUserFollower(String userid) {
        return archiveRelationshipRepository.countAllFollowerByUserId(userid);
    }

    public ArrayList<Relationship> findAllUserRequestFollowing(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserRequestFollowing(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public ArrayList<Relationship> findAllUserRequestFollower(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserRequestFollower(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    public ArrayList<Relationship> findAllUserBlock(String userid) {
        List<RelationshipEntity> entityList = archiveRelationshipRepository.findAllUserBlock(userid);
        ArrayList<Relationship> list = new ArrayList<>();
        for (RelationshipEntity entity : entityList) {
            list.add(entity.toDto());
        }
        return list;
    }

    // ArchiveReportRepository

    // ArchiveTagRepository


}
