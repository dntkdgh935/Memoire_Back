package com.web.memoire.user.controller;

import com.web.memoire.user.model.dto.User;
import com.web.memoire.user.model.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;

    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    @PostMapping(value = "/user/idcheck")
    public ResponseEntity<String>dupCheckId(@RequestParam("loginId") String loginId) {
        log.info("/user/idcheck : " + loginId);

        boolean exists = userService.selectCheckId(loginId);
        return ResponseEntity.ok(exists ? "duplicated" : "ok");
    }

    @PostMapping("user/signup")
    public ResponseEntity userInsertMethod(
            @RequestBody User user){
        log.info("/user/signup : " + user);

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.error("회원가입 실패: 비밀번호가 null이거나 비어있습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호를 입력해주세요.");
        }

        String newUserId = UUID.randomUUID().toString();
        user.setUserId(newUserId);

        user.setPassword(bcryptPasswordEncoder.encode(user.getPassword()));
        log.info("incoding : " + user.getPassword()+", length : " + user.getPassword().length());

        user.setRole("USER");
        log.info("userInsertMethod : " + user);

        try{
            userService.insertUser(user);
            return ResponseEntity.status(HttpStatus.OK).build();
        }catch(Exception e){
            log.error("회원가입 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
