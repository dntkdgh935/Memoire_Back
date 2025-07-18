package com.web.memoire.chat.jpa.repository;

import com.web.memoire.chat.jpa.entity.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, String> {

    // 기존 대화내용 불러오기
    @Query(value = "SELECT c FROM ChatEntity c WHERE c.chatroomid = :chatroomid ORDER BY c.sentTime ASC")
    List<ChatEntity> findByChatroomidOrderBySentTimeAsc(@Param("chatroomid") String chatroomid);
}
