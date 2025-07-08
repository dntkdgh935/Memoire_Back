package com.web.memoire.library.model.service;

import com.web.memoire.common.dto.Tag;
import com.web.memoire.common.entity.TagEntity;
import com.web.memoire.library.jpa.repository.LibTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {
    @Autowired
    private final LibTagRepository libTagRepository;

    public List<Tag> getAllTags() {
        List<TagEntity> tagEntities = libTagRepository.findAll();
        return tagEntities.stream()
                .map(TagEntity::toDto)
                .toList();
    }
}

