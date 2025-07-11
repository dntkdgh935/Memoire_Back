package com.web.memoire.user.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import java.util.Date; // Date 타입 사용
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ImapVerificationService {

    @Value("${imap.host}")
    private String imapHost;
    @Value("${imap.port}")
    private String imapPort;
    @Value("${imap.username}")
    private String imapUsername;
    @Value("${imap.password}")
    private String imapPassword;

    private static final Pattern PHONE_DOMAIN_PATTERN = Pattern.compile("<?([0-9]{10,11})\\s*@\\s*([a-zA-Z0-9.]+)>?");
    private static final String[] ALLOWED_DOMAINS = {"vmms.nate.com", "ktfmms.magicn.com", "lguplus.com"};

    public String verifyCodeViaEmail(String verificationCode) throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*"); // 개발용: 실제 서비스에서는 유효한 인증서 필요

        Session session = Session.getInstance(props, null);
        Store store = null;
        Folder inbox = null;

        try {
            store = session.getStore("imaps");
            store.connect(imapHost, imapUsername, imapPassword);
            log.info("IMAP connected to: {}", imapHost);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            log.info("Opened INBOX, total messages: {}", inbox.getMessageCount());

            // 15분 이내의 이메일만 검색 (Node.js 코드와 동일한 조건)
            Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - 15 * 60 * 1000L);

            // 본문에 인증 코드가 포함된 이메일 검색
            SearchTerm searchTerm = new AndTerm(
                    new ReceivedDateTerm(ReceivedDateTerm.GT, fifteenMinutesAgo),
                    new BodyTerm(verificationCode)
            );

            Message[] messages = inbox.search(searchTerm);
            log.info("IMAP search found {} messages containing code '{}' since {}", messages.length, verificationCode, fifteenMinutesAgo);

            if (messages.length == 0) {
                throw new Exception("CODE_NOT_FOUND_IN_EMAIL: No recent emails found with the code.");
            }

            // 최신 메시지부터 확인
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                String fromAddress = message.getFrom()[0].toString();

                String emailContent = getTextFromMessage(message);
                log.debug("Processing message from: '{}', content snippet: '{}'", fromAddress, emailContent != null ? emailContent.substring(0, Math.min(emailContent.length(), 200)) : "[No Content]");

                if (emailContent != null && emailContent.contains(verificationCode)) {
                    Matcher matcher = PHONE_DOMAIN_PATTERN.matcher(fromAddress);
                    if (matcher.find()) {
                        String fromPhone = matcher.group(1);
                        String fromDomain = matcher.group(2);
                        log.debug("Extracted phone: {}, domain: {}", fromPhone, fromDomain);

                        boolean isAllowedDomain = false;
                        for (String domain : ALLOWED_DOMAINS) {
                            if (domain.equals(fromDomain)) {
                                isAllowedDomain = true;
                                break;
                            }
                        }

                        if (isAllowedDomain) {
                            log.info("Verification email found from allowed domain. Phone: {}", fromPhone);
                            return fromPhone;
                        } else {
                            log.warn("Email from disallowed domain: {}", fromDomain);
                        }
                    } else {
                        log.warn("Failed to extract phone number/domain from email 'From' header: {}", fromAddress);
                    }
                }
            }
            throw new Exception("NO_VALID_EMAIL_FOUND: No matching email found with valid sender and code.");

        } catch (AuthenticationFailedException e) {
            log.error("IMAP authentication failed: {}", e.getMessage());
            throw new Exception("IMAP_AUTH_FAILED: Check IMAP username/password.");
        } catch (MessagingException e) {
            log.error("IMAP messaging error: {}", e.getMessage());
            throw new Exception("IMAP_MESSAGING_ERROR: " + e.getMessage());
        } finally {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }

    private String getTextFromMessage(Message message) throws Exception {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result = (String) bodyPart.getContent();
                    break;
                }
            }
        }
        return result;
    }
}