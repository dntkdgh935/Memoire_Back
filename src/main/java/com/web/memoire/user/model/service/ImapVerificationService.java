package com.web.memoire.user.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.SearchTerm; // ReceivedDateTerm은 더 이상 직접 사용하지 않지만, SearchTerm은 필요합니다.
import java.text.SimpleDateFormat; // 새로 추가: 날짜 포맷팅용
import java.util.Date;
import java.util.Locale; // 새로 추가: 날짜 포맷팅 로케일용
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
// import static jakarta.mail.search.ReceivedDateTerm.GT; // 더 이상 직접 사용하지 않으므로 제거 가능

@Slf4j
@Service
public class ImapVerificationService {

    @Value("${imap.host}")
    private String imapHost;
    @Value("${imap.port}")
    private int imapPort;
    @Value("${imap.username}")
    private String imapUsername;
    @Value("${imap.password}")
    private String imapPassword;

    private static final Pattern PHONE_DOMAIN_PATTERN = Pattern.compile("<?([0-9]{10,11})\\s*@\\s*([a-zA-Z0-9.-]+)>?");
    private static final String[] ALLOWED_DOMAINS = {"vmms.nate.com", "ktfmms.magicn.com", "lguplus.com", "lguplus.co.kr", "sktelecom.com", "mms.skt.com"};

    public String verifyCodeViaEmail(String verificationCode, String targetPhone) throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        // 배포 시에는 반드시 제거하거나 특정 인증서만 신뢰하도록 변경
        // props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");

        // 디버그 로깅 활성화 (테스트 시 매우 유용)
        props.put("mail.debug", "true");

        Session session = Session.getInstance(props, null);

        Store store = null;
        Folder inbox = null;

        try {
            store = session.getStore("imaps");
            store.connect(imapHost, imapUsername, imapPassword);
            log.info("IMAP connected to: {} successfully.", imapHost);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            log.info("Opened INBOX. Total messages: {}", inbox.getMessageCount());

            // 15분 이내의 이메일만 검색하기 위한 기준 시간 설정
            Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - 15 * 60 * 1000L);

            // IMAP RFC에 정의된 날짜 형식으로 직접 변환합니다.
            // 'DD-Mon-YYYY' 형식으로 포맷팅합니다. 예: 14-Jul-2025
            SimpleDateFormat imapDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
            String imapDateString = imapDateFormat.format(fifteenMinutesAgo);

            // 직접 IMAP 'SINCE' 검색 명령을 문자열로 만듭니다.
            // 이 SearchTerm은 로컬에서 필터링하지 않고, toString() 메서드를 통해 IMAP 서버로 직접 명령을 전달합니다.
            SearchTerm rawSearchTerm = new SearchTerm() {
                @Override
                public boolean match(Message message) {
                    // 이 메서드는 JavaMail이 로컬에서 필터링할 때 사용됩니다.
                    // 우리는 서버에서 필터링할 것이므로 항상 true를 반환하도록 두어도 무방합니다.
                    return true;
                }

                @Override
                public String toString() {
                    // IMAP 서버로 전송될 실제 SEARCH 명령을 반환합니다.
                    return "SINCE \"" + imapDateString + "\"";
                }
            };

            // 변경된 SearchTerm을 사용하여 검색합니다.
            Message[] messages = inbox.search(rawSearchTerm);

            log.info("IMAP search found {} messages received since {}", messages.length, fifteenMinutesAgo);

            if (messages.length == 0) {
                log.warn("CODE_NOT_FOUND_IN_EMAIL: No recent emails found within 15 minutes.");
                throw new Exception("CODE_NOT_FOUND_IN_EMAIL: No recent emails found.");
            }

            // 최신 메시지부터 확인하여 유효한 이메일을 찾음
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                String fromAddress = null;
                try {
                    fromAddress = message.getFrom()[0].toString();
                } catch (MessagingException | NullPointerException e) {
                    log.warn("Could not get From address for message ID {}: {}", message.getMessageNumber(), e.getMessage());
                    continue;
                }

                String emailContent = getTextFromMessage(message);

                // 디버그 로그에 이메일 본문 전체 또는 더 길게 출력
                log.debug("Processing message from: '{}', full content: '{}'", fromAddress, emailContent != null ? emailContent : "[No Content]");

                // Java 코드에서 본문에 인증 코드가 포함되어 있는지 확인합니다.
                if (emailContent != null && emailContent.contains(verificationCode)) {
                    Matcher matcher = PHONE_DOMAIN_PATTERN.matcher(fromAddress);
                    if (matcher.find()) {
                        String extractedPhone = matcher.group(1); // 첫 번째 그룹 (전화번호)
                        String extractedDomain = matcher.group(2); // 두 번째 그룹 (도메인)
                        log.debug("Extracted phone: {}, domain: {}", extractedPhone, extractedDomain);

                        boolean isAllowedDomain = false;
                        for (String domain : ALLOWED_DOMAINS) {
                            if (extractedDomain.equalsIgnoreCase(domain)) {
                                isAllowedDomain = true;
                                break;
                            }
                        }

                        if (isAllowedDomain) {
                            // 인증 코드와 FROM 전화번호가 일치하는지 최종 확인
                            if (targetPhone != null && targetPhone.equals(extractedPhone)) {
                                log.info("Verification email found from allowed domain and matching phone number. Phone: {}", extractedPhone);
                                return "success";
                            } else {
                                log.warn("Email found but phone number mismatch. Expected: {}, Found: {}", targetPhone, extractedPhone);
                            }
                        } else {
                            log.warn("Email from disallowed domain: {}", extractedDomain);
                        }
                    } else {
                        log.warn("Failed to extract phone number/domain from email 'From' header: {}", fromAddress);
                    }
                }
            }
            log.warn("NO_VALID_EMAIL_FOUND: No matching email found with valid sender, code, and phone number.");
            throw new Exception("NO_VALID_EMAIL_FOUND: No matching email found with valid sender and code.");

        } catch (NoClassDefFoundError e) {
            log.error("JavaMail API Class Not Found. Please check your Gradle/Maven dependencies. Error: {}", e.getMessage());
            throw new RuntimeException("CLASS_NOT_FOUND: JavaMail API is missing. " + e);
        } catch (AuthenticationFailedException e) {
            log.error("IMAP authentication failed. Check IMAP username/password and app password. Username: {}", imapUsername, e);
            throw new Exception("IMAP_AUTH_FAILED: Check IMAP username/password and app password.");
        } catch (MessagingException e) {
            log.error("IMAP messaging error during connection or operation: {}", e.getMessage(), e);
            throw new Exception("IMAP_MESSAGING_ERROR: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during IMAP verification: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(false);
                } catch (MessagingException e) {
                    log.error("Error closing INBOX folder: {}", e.getMessage());
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    log.error("Error closing IMAP store: {}", e.getMessage());
                }
            }
        }
    }

    private String getTextFromMessage(Message message) throws Exception {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            // HTML을 텍스트로 변환하는 라이브러리 (Jsoup 등)를 사용하거나,
            // 간단히 HTML 태그를 제거하는 로직을 추가
            String htmlContent = (String) message.getContent();
            result = htmlContent.replaceAll("<[^>]*>", ""); // 기본적인 HTML 태그 제거
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result = (String) bodyPart.getContent();
                    // text/plain을 찾으면 더 이상 찾을 필요 없이 반환
                    return result;
                } else if (bodyPart.isMimeType("text/html")) {
                    String htmlContent = (String) bodyPart.getContent();
                    result = htmlContent.replaceAll("<[^>]*>", ""); // HTML 태그 제거
                    // HTML도 우선적으로 처리할 수 있음. 필요에 따라 text/plain보다 먼저 검색할 수도 있습니다.
                }
                // 다른 multipart 내용이 있을 경우 recursive call 할 수도 있습니다.
                // else if (bodyPart.getContent() instanceof MimeMultipart) {
                //    result = getTextFromMessage(new MimeMessage(null, ((MimeMultipart) bodyPart.getContent()).getInputStream()));
                //    if (!result.isEmpty()) return result;
                // }
            }
        }
        return result;
    }
}
