package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Relationship;
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
@Table(name = "TB_RELATIONSHIP")
@Entity
@IdClass(RelationshipId.class)
public class RelationshipEntity {

    @Id
    @Column(name = "USERID", nullable = false)
    private String userid;
    @Id
    @Column(name = "TARGETID", nullable = false)
    private String targetid;
    @Column(name = "FOLLOW_DATE", nullable = false, columnDefinition = "date default sysdate")
    private Date followDate;
    @Column(name = "STATUS", nullable = false)
    private String status;

    public Relationship toDto() {
        return Relationship.builder()
                .userid(userid)
                .targetid(targetid)
                .followDate(followDate)
                .status(status)
                .build();
    }
}
