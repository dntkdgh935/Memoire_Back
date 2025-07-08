package com.web.memoire.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionTagId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String collectionid;
    private String tagid;
}
