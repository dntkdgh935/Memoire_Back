package com.web.memoire.user.jpa.entity;

import com.web.memoire.user.model.dto.Pwd;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_PWD_HISTORY") // 테이블 이름 변경 권장
@Entity
public class PwdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 자동 증가 전략에 맞춤 (MySQL: IDENTITY, Oracle: SEQUENCE 등)
    @Column(name = "ID")
    private Long id; // 새로운 고유 ID 필드

    @Column(name="USERID", length = 50, nullable = false) // userId는 이제 일반 컬럼
    private String userId;

    @Column(name="CH_PWD", nullable = false)
    @Temporal(TemporalType.TIMESTAMP) // 날짜와 시간 모두 저장
    private Date chPwd;

    @Column(name="PREV_PWD", length = 100) // 이전 비밀번호 (암호화된 상태)
    private String prevPwd;

    @Column(name="CURR_PWD", nullable = false, length = 100) // 변경된 새 비밀번호 (암호화된 상태)
    private String currPwd;

    @PrePersist
    public void prePersist() {
        if (chPwd == null) {
            chPwd = new Date(); // 현재 시간으로 설정
        }
    }

    public Pwd toDto(){
        return Pwd.builder()
                .userId(this.userId)
                .chPwd(this.chPwd)
                .prevPwd(this.prevPwd)
                .currPwd(this.currPwd)
                .build();
    }
}