package com.web.memoire.security.jwt.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="TB_TOKEN")
public class Token {
    @Id
    @Column(name="USERID", length=50, nullable = false)
    private String userId;
    @Column(name="TOKENID", nullable = false, length=512)
    private String tokenId;

}
