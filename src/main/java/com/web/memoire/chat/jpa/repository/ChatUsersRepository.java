package com.web.memoire.chat.jpa.repository;

import com.web.memoire.chat.jpa.entity.ChatUsersEntity;
import com.web.memoire.chat.jpa.entity.ChatUsersId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatUsersRepository extends JpaRepository<ChatUsersEntity, ChatUsersId> {

    // 유저가 속해있는 전체 채팅방들 조회
    @Query(value = "SELECT c FROM ChatUsersEntity c WHERE c.userid = :userid")
    List<ChatUsersEntity> findAllByUserId(@Param("userid") String userid);

    // 채팅방에 속해있는 유저들 조회
    @Query(value = "SELECT c FROM ChatUsersEntity c WHERE c.chatroomid = :chatroomid")
    List<ChatUsersEntity> findAllByChatroomid(@Param("chatroomid") String chatroomid);

    // 1:1 채팅할 때 userid와 otherUserid가 있는지 확인
    @Query(value = """
            SELECT chatroomid
            FROM TB_CHAT_USERS
            GROUP BY chatroomid
                    HAVING COUNT(*) = 2
                       AND SUM(CASE 
                               WHEN userid = :userid THEN 1 
                               WHEN userid = :otherUserid THEN 1 
                               ELSE 0 
                           END) = 2
            """, nativeQuery = true)
    String findByUserIdAndOtherUserId(@Param("userid") String userid, @Param("otherUserid") String otherUserid);

}
