package com.web.memoire.user.jpa.entity;


import com.web.memoire.user.model.dto.Pwd;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.GregorianCalendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "TB_PWD")
@Entity
public class PwdEntity {
    @Id
    @Column(name="USERID", length = 50)
    private String userId;

    @Column(name="CH_PWD", nullable = false)
    private Date chPwd;

    @Column(name="PREV_PWD", length = 100)
    private String prevPwd;

    @Column(name="CURR_PWD", nullable = false, length = 100)
    private String currPwd;

    @PrePersist
    public void prePersist() {
        chPwd = new GregorianCalendar().getGregorianChange();
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
