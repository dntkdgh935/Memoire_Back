package com.web.memoire.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userid;
    private int collectionid;
}
