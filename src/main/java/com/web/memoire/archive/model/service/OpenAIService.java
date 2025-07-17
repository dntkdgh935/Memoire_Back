package com.web.memoire.archive.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenAIService {

    private final String apiUrl = "https://api.openai.com/v1/embeddings"; // OpenAI 임베딩 API URL
    @Value("${openai.api.key}")
    private String apiKey;

    private final WebClient webClient;
    // WebClient 생성자 주입
    public OpenAIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
    }

    public Mono<String> getEmbedding(String text) {
        // 요청 보내기
        return webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(new EmbeddingRequest("text-embedding-3-small", text)) // 요청 본문 (EmbeddingRequest는 아래에서 설명)
                .retrieve()
                .bodyToMono(String.class);  // 응답을 String으로 반환
    }

    // 임베딩 요청을 위한 DTO 클래스
    static class EmbeddingRequest {
        private final String model;
        private final String input;

        public EmbeddingRequest(String model, String input) {
            this.model = model;
            this.input = input;
        }

        public String getModel() {
            return model;
        }

        public String getInput() {
            return input;
        }
    }
}
