package com.web.memoire.user.jpa.repository;

import com.web.memoire.user.jpa.entity.UserEntity;


public interface UserRepositoryCustom {
    UserEntity findByUserid(String userId);

}
