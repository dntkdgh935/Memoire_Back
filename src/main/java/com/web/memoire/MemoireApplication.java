package com.web.memoire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.web.memoire")
@SpringBootApplication
public class MemoireApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoireApplication.class, args);
    }

}
