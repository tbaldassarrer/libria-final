package es.prw.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${spring.mail.password:}")
    private String password;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void logMailConfiguration() {
        logger.info("Configuracion SMTP: host={}, usuario={}, passwordConfigurada={}",
                clean(host),
                clean(from),
                !isBlank(clean(password)));
    }

    public void sendActivationEmail(String to, String username, String activationUrl) {
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(clean(from));
        message.setSubject("Activa tu cuenta de Libria");
        message.setText(
                "Hola " + username + ",\n\n"
                        + "Para activar tu cuenta de Libria, abre este enlace:\n"
                        + activationUrl + "\n\n"
                        + "Si no has creado esta cuenta, puedes ignorar este correo.");

        logger.info("Enviando email de activacion desde {} hacia {} usando SMTP {}", clean(from), to, clean(host));
        mailSender.send(message);
    }

    public void sendContactEmail(String to, String senderName, String senderEmail, String body) {
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(clean(from));
        message.setReplyTo(senderEmail);
        message.setSubject("Nuevo mensaje de contacto de Libria");
        message.setText(
                "Nombre: " + senderName + "\n"
                        + "Email: " + senderEmail + "\n\n"
                        + body);

        mailSender.send(message);
    }

    private void validateMailConfiguration() {
        host = clean(host);
        from = clean(from);
        password = clean(password);

        if (isBlank(host) || isBlank(from) || isBlank(password)) {
            throw new IllegalStateException("No se pudo enviar el email de activacion. El correo de Libria no esta configurado.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
