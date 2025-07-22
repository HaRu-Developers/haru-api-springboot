package com.haru.api.infra.mail;

public interface EmailSender {
    void send(
            String to,
            String subject,
            String content
    );
}
