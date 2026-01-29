package com.davydcr.document.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de envio de emails para alertas de segurança.
 * 
 * Responsabilidades:
 * - Enviar emails de alerta de brute force
 * - Enviar notificações de atividades suspeitas
 * - Enviar relatórios de segurança periódicos
 * - Suportar templates HTML personalizados
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${spring.mail.from:noreply@document-ai.local}")
    private String mailFrom;
    
    @Value("${app.email.admin-address:admin@document-ai.local}")
    private String adminEmail;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    public EmailService(JavaMailSender mailSender, EmailTemplateService emailTemplateService) {
        this.mailSender = mailSender;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Envia alerta de brute force para admin
     */
    public void sendBruteForceAlert(String email, String ipAddress, int attemptCount) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping brute force alert");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userEmail", email);
            variables.put("ipAddress", ipAddress);
            variables.put("attemptCount", attemptCount);
            variables.put("timestamp", formatDateTime(LocalDateTime.now()));
            
            String subject = "🚨 Alerta de Brute Force Detectado";
            String htmlContent = emailTemplateService.renderTemplate("brute-force-alert", variables);
            
            sendHtmlEmail(adminEmail, subject, htmlContent);
            logger.info("Brute force alert sent for email: {} from IP: {}", email, ipAddress);
        } catch (Exception e) {
            logger.error("Erro ao enviar alerta de brute force: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia alerta de atividade suspeita para admin
     */
    public void sendSuspiciousActivityAlert(String email, String eventType, String ipAddress, String details) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping suspicious activity alert");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userEmail", email);
            variables.put("eventType", eventType);
            variables.put("ipAddress", ipAddress);
            variables.put("details", details);
            variables.put("timestamp", formatDateTime(LocalDateTime.now()));
            
            String subject = "⚠️ Atividade Suspeita Detectada: " + eventType;
            String htmlContent = emailTemplateService.renderTemplate("suspicious-activity-alert", variables);
            
            sendHtmlEmail(adminEmail, subject, htmlContent);
            logger.info("Suspicious activity alert sent for user: {}", email);
        } catch (Exception e) {
            logger.error("Erro ao enviar alerta de atividade suspeita: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia alerta de múltiplas tentativas de IP em tempos curtos
     */
    public void sendAnomalousIpAlert(String ipAddress, int eventCount, int failedLoginCount) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping anomalous IP alert");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ipAddress", ipAddress);
            variables.put("eventCount", eventCount);
            variables.put("failedLoginCount", failedLoginCount);
            variables.put("timestamp", formatDateTime(LocalDateTime.now()));
            
            String subject = "🔴 Atividade Anômala de IP: " + ipAddress;
            String htmlContent = emailTemplateService.renderTemplate("anomalous-ip-alert", variables);
            
            sendHtmlEmail(adminEmail, subject, htmlContent);
            logger.info("Anomalous IP alert sent for IP: {}", ipAddress);
        } catch (Exception e) {
            logger.error("Erro ao enviar alerta de IP anômalo: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia relatório de segurança diário para admin
     */
    public void sendSecurityReport(Map<String, Object> reportData) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping security report");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>(reportData);
            variables.put("generatedAt", formatDateTime(LocalDateTime.now()));
            
            String subject = "📊 Relatório de Segurança Diário";
            String htmlContent = emailTemplateService.renderTemplate("daily-security-report", variables);
            
            sendHtmlEmail(adminEmail, subject, htmlContent);
            logger.info("Daily security report sent");
        } catch (Exception e) {
            logger.error("Erro ao enviar relatório de segurança: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia email para usuário notificando acesso de um novo local
     */
    public void sendNewLocationNotification(String userEmail, String ipAddress, String location, LocalDateTime timestamp) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping new location notification");
            return;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ipAddress", ipAddress);
            variables.put("location", location);
            variables.put("timestamp", formatDateTime(timestamp));
            
            String subject = "🔐 Novo acesso detectado em seu conta";
            String htmlContent = emailTemplateService.renderTemplate("new-location-notification", variables);
            
            sendHtmlEmail(userEmail, subject, htmlContent);
            logger.info("New location notification sent to user: {}", userEmail);
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de novo local: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia email simples
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping simple email");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            logger.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Erro ao enviar email simples: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia email HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping HTML email");
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        logger.debug("HTML email sent to: {}", to);
    }

    /**
     * Envia email para múltiplos destinatários
     */
    public void sendBulkEmail(String[] recipients, String subject, String htmlContent) throws MessagingException {
        if (!emailEnabled) {
            logger.debug("Email disabled, skipping bulk email");
            return;
        }

        for (String recipient : recipients) {
            sendHtmlEmail(recipient, subject, htmlContent);
        }
        
        logger.info("Bulk email sent to {} recipients", recipients.length);
    }

    /**
     * Formata LocalDateTime para formato legível
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /**
     * Verifica se o serviço de email está habilitado
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }
}
