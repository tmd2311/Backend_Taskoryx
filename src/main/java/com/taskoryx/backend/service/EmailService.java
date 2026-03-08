package com.taskoryx.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    /**
     * Gửi email chào mừng kèm mật khẩu tạm thời khi admin tạo user mới.
     * Chạy async để không block luồng chính.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String temporaryPassword) {
        try {
            Context ctx = new Context();
            ctx.setVariable("fullName", fullName);
            ctx.setVariable("email", toEmail);
            ctx.setVariable("temporaryPassword", temporaryPassword);
            ctx.setVariable("loginUrl", frontendUrl + "/login");
            ctx.setVariable("supportEmail", fromEmail);
            ctx.setVariable("year", Year.now().getValue());

            String htmlContent = templateEngine.process("email/welcome-user", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Taskoryx");
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với Taskoryx – Thông tin tài khoản");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
        }
    }
}
