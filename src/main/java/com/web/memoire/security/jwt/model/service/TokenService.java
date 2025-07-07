package com.web.memoire.security.jwt.model.service;

import com.web.memoire.security.jwt.jpa.entity.Token;
import com.web.memoire.security.jwt.jpa.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TokenService {
    private final TokenRepository tokenRepository;

    @Transactional
    public void saveRefreshToken(Token token) {
        tokenRepository.save(token);
    }

    @Transactional
    public void deleteRefreshToken(String userId) {
        tokenRepository.deleteById(userId);
    }

    @Transactional
    public int updateRefreshToken(String userId, String tokenId) {
        return tokenRepository.updateTokenById(userId, tokenId);
    }

    public String selectId(String userId, String tokenId) {
        return tokenRepository.findByUserIdAndTokenId(userId, tokenId);
    }

    public String selectToken(String userId){
        return tokenRepository.findTokenValueByUserId(userId);
    }

}
