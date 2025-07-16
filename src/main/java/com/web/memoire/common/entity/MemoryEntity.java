package com.web.memoire.common.entity;

import com.web.memoire.common.dto.Memory;
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
@Table(name = "TB_MEMORY")
@Entity
public class MemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memory_seq")
    @SequenceGenerator(name = "memory_seq", sequenceName = "SEQ_TB_MEMORY_MEMORYID", allocationSize = 1)
    @Column(name = "MEMORYID", nullable = false)
    private int memoryid;

    @Column(name = "MEMORY_TYPE", length = 50, nullable = false)
    private String memoryType;

    @Column(name = "COLLECTIONID", nullable = false)
    private int collectionid;

    @Column(name = "TITLE", length = 100, nullable = false)
    private String title;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Column(name = "FILENAME")
    private String filename;

    @Column(name = "FILEPATH", length = 200)
    private String filepath;

    @Column(name = "CREATED_DATE", nullable = false)
    private Date createdDate;

    @Column(name = "MEMORY_ORDER", nullable = false)
    private int memoryOrder;

    public Memory toDto() {
        return Memory.builder()
                .memoryid(memoryid)
                .memoryType(memoryType)
                .collectionid(collectionid)
                .title(title)
                .content(content)
                .filename(filename)
                .filepath(filepath)
                .createdDate(createdDate)
                .memoryOrder(memoryOrder)
                .build();
    }
}
