package com.web.memoire.user.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
public class FaceRecognitionService {

    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // @Qualifier 어노테이션을 사용하여 "imtimRestTemplate" 이름의 빈을 주입하도록 명시
    public FaceRecognitionService(@Qualifier("imtimRestTemplate") RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * FastAPI 서버의 얼굴 인식 API를 호출합니다. (이 메서드는 새로운 아키텍처에서 사용되지 않을 수 있습니다.)
     * @param imageData 인식할 얼굴 이미지의 바이트 배열
     * @return FastAPI에서 반환된 얼굴 인식 결과 (JSON 형태)
     */
    public List<Map<String, Object>> recognizeFace(byte[] imageData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "webcam_frame.jpg"; // FastAPI에서 사용할 파일 이름
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    fastapiBaseUrl + "/recognize-face/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("faces");
            } else {
                System.err.println("FastAPI 얼굴 인식 실패: " + response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("FastAPI 얼굴 인식 요청 중 오류 발생: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * FastAPI 서버의 얼굴 등록 API를 호출합니다. (이 메서드는 새로운 아키텍처에서 사용되지 않을 수 있습니다.)
     * @param userName 등록할 사용자 이름
     * @param imageData 등록할 얼굴 이미지의 바이트 배열
     * @return 등록 성공 여부
     */
    public boolean registerFace(String userName, byte[] imageData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return userName + ".jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    fastapiBaseUrl + "/register-face/" + userName,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println("FastAPI 얼굴 등록 요청 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    /**
     * 이미지 파일로부터 얼굴 임베딩(embedding) 값을 추출하여 반환합니다.
     * FastAPI의 /get-face-embedding/ 엔드포인트를 호출합니다.
     * @param imageData 임베딩을 추출할 얼굴 이미지의 바이트 배열
     * @return 추출된 얼굴 임베딩 값 (List<Float>)
     * @throws IOException 이미지 처리 중 오류 발생 시
     */
    public List<Float> getFaceEmbedding(byte[] imageData) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "face_image.jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    fastapiBaseUrl + "/get-face-embedding/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.convertValue(response.getBody().get("embedding"), new TypeReference<List<Float>>() {});
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.err.println("FastAPI에서 얼굴을 찾을 수 없습니다.");
                return Collections.emptyList();
            } else {
                System.err.println("FastAPI 임베딩 추출 실패: " + response.getStatusCode() + ", " + response.getBody());
                throw new IOException("FastAPI 임베딩 추출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("FastAPI 임베딩 추출 요청 중 오류 발생: " + e.getMessage());
            throw new IOException("FastAPI 임베딩 추출 요청 중 오류 발생", e);
        }
    }

    /**
     * 현재 얼굴 임베딩과 알려진 얼굴 임베딩들을 비교하여 가장 일치하는 얼굴을 찾습니다.
     * FastAPI의 /compare-embeddings/ 엔드포인트를 호출합니다.
     * @param currentEmbedding 현재 얼굴의 임베딩
     * @param knownEmbeddings 데이터베이스에 저장된 알려진 얼굴들의 임베딩 리스트
     * @param knownUserIds 각 알려진 임베딩에 해당하는 사용자 ID 리스트
     * @return 일치하는 사용자 ID, 일치 여부, 거리 정보를 담은 Map
     */
    public Map<String, Object> compareEmbeddings(List<Float> currentEmbedding, List<List<Float>> knownEmbeddings, List<String> knownUserIds) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 본문 생성 (FastAPI Pydantic 모델에 맞춤)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("current_embedding", currentEmbedding);
        requestBody.put("known_embeddings", knownEmbeddings);
        requestBody.put("known_user_ids", knownUserIds);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    fastapiBaseUrl + "/compare-embeddings/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                System.err.println("FastAPI 임베딩 비교 실패: " + response.getStatusCode() + ", " + response.getBody());
                throw new IOException("FastAPI 임베딩 비교 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("FastAPI 임베딩 비교 요청 중 오류 발생: " + e.getMessage());
            throw new IOException("FastAPI 임베딩 비교 요청 중 오류 발생", e);
        }
    }
}
