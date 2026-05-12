package es.prw.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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

    public void sendActivationEmail(String to, String username, String activationUrl) {
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setSubject("Activa tu cuenta de Libria");
        message.setText(
                "Hola " + username + ",\n\n"
                        + "Para activar tu cuenta de Libria, abre este enlace:\n"
                        + activationUrl + "\n\n"
                        + "Si no has creado esta cuenta, puedes ignorar este correo.");

        mailSender.send(message);
    }

    public void sendContactEmail(String to, String senderName, String senderEmail, String body) {
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setReplyTo(senderEmail);
        message.setSubject("Nuevo mensaje de contacto de Libria");
        message.setText(
                "Nombre: " + senderName + "\n"
                        + "Email: " + senderEmail + "\n\n"
                        + body);

        mailSender.send(message);
    }

    private void validateMailConfiguration() {
        if (isBlank(host) || isBlank(from) || isBlank(password)) {
            throw new IllegalStateException("No se pudo enviar el email de activacion. El correo de Libria no esta configurado.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
