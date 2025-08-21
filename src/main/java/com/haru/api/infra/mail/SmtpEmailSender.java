package com.haru.api.infra.mail;

import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.infra.mail.handler.MailHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void send(
            String to,
            String title,
            String content
    ) {
        // 1. 이메일 형식 검증
        if (!isValidEmail(to)) {
            log.warn("잘못된 이메일 형식 - {}", to);
            throw new MailHandler(ErrorStatus.MAIL_INVALID_FORMAT);
        }

        // 2. 메일 전송 시도
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true); // HTML 지원위해 true 로 설정
            helper.setFrom(from); // 보내는 계정

            javaMailSender.send(message);
            log.info("이메일 전송 완료 - 수신자: {}", to);

        } catch (MessagingException e) {
            log.error("이메일 전송 실패 - 수신자: {}", to, e);
            throw new MailHandler(ErrorStatus.MAIL_SEND_FAIL);
        }
    }

    // RFC 형식 검증
    private boolean isValidEmail(String email) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate(); // 형식이 틀리면 AddressException 발생
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}

