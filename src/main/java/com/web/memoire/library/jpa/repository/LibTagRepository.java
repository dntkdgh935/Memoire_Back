package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibTagRepository extends JpaRepository<TagEntity, Integer> {
}