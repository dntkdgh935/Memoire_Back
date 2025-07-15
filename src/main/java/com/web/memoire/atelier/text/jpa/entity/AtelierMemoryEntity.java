package com.web.memoire.atelier.text.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MEMORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ✅ builder() 사용 가능하게 추가
public class AtelierMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memoryid")
    private Long memoryId;

    @Column(name = "memory_type", nullable = false)
    private String memoryType;

    @Column(name = "collectionid", nullable = false)
    private int collectionId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "filename")
    private String filename;

    @Column(name = "filepath")
    private String filepath;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "memory_order", nullable = false)
    private int memoryOrder;
}