package com.web.memoire.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportId implements Serializable {

    private static final long serialVersionUID = 1L;

    private int memoryid;
    private String userid;
}
