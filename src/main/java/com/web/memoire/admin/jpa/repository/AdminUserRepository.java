package com.web.memoire.admin.jpa.repository;

import com.web.memoire.common.entity.CollectionEntity;
import com.web.memoire.user.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<UserEntity, String>, AdminUserRepositoryCustom {
}
