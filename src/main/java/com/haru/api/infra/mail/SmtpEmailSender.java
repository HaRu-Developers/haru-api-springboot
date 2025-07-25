package com.haru.api.infra.mail;

import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.infra.mail.handler.MailHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(
            String to,
            String title,
            String content
    ) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true); // HTML 지원위해 true 로 설정

            javaMailSender.send(message);
            log.info("이메일 전송 완료 - 수신자: {}", to);

        } catch (MessagingException e) {
            log.error("이메일 전송 실패 - 수신자: {}", to, e);
            throw new MailHandler(ErrorStatus.MAIL_SEND_FAIL);
        }
    }
}

