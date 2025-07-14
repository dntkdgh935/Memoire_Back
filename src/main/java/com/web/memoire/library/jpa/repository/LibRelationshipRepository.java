package com.web.memoire.library.jpa.repository;

import com.web.memoire.common.entity.RelationshipEntity;
import com.web.memoire.common.entity.RelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface LibRelationshipRepository extends JpaRepository<RelationshipEntity, RelationshipId> {



}
