package com.web.memoire.user.model.service;

import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.dto.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    public User selectUser(String userId){
        return userRepository.findByUserid(userId).toDto();
    }
}
