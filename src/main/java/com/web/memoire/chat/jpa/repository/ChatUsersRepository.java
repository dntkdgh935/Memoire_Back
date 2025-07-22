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

    // TODO: 유저가 속해있는 전체 채팅방들 최신 대화 순서대로 조회
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

    // 관리자와의 채팅방이 있는지 확인
    @Query(value = "SELECT c FROM ChatUsersEntity c WHERE c.userid = :userid AND c.chatroomid LIKE 'admin%'")
    ChatUsersEntity findAdminChatroom(@Param("userid") String userid);

    // 관리자의 채팅방 조회
    @Query(value = "SELECT c FROM ChatUsersEntity c WHERE c.chatroomid LIKE 'admin%'")
    List<ChatUsersEntity> findAdminChatrooms();

    // 특정유저가 채팅방에 있는지 조회
    @Query(value = "SELECT c FROM ChatUsersEntity c WHERE c.userid = :userid AND c.chatroomid = :chatroomid")
    ChatUsersEntity findByUserIdAndChatroomid(@Param("userid") String userid, @Param("chatroomid") String chatroomid);

    void deleteAllByChatroomid(String chatroomid);
}
