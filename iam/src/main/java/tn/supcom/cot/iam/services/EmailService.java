package tn.supcom.cot.iam.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {

    @Inject
    private Logger logger;

    @Inject @ConfigProperty(name = "smtp.host")
    private String host;

    @Inject @ConfigProperty(name = "smtp.port")
    private int port;

    @Inject @ConfigProperty(name = "smtp.username")
    private String username;

    @Inject @ConfigProperty(name = "smtp.password")
    private String password;

    @Inject @ConfigProperty(name = "smtp.starttls.enable")
    private boolean auth;

    @Inject @ConfigProperty(name = "smtp.sender")
    private String sender;

    public void sendActivationEmail(String to, String code) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", host);
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", String.valueOf(auth));

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Activate your Health Monitoring Account");

            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #667eea; text-align: center;">Welcome to Health Monitoring</h2>
                    <p>Thank you for registering. To ensure the security of your medical data, please verify your email address.</p>
                    <p style="text-align: center;">Your activation code is:</p>
                    <h1 style="background: #f0f4ff; color: #764ba2; padding: 15px; text-align: center; letter-spacing: 5px; border-radius: 5px;">%s</h1>
                    <p>This code expires in 10 minutes.</p>
                    <hr style="border: 0; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #999; text-align: center;">If you did not request this code, please ignore this email.</p>
                </div>
                """, code);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Activation email sent to " + to);

        } catch (MessagingException e) {
            logger.severe("Failed to send email: " + e.getMessage());
            // On log l'erreur mais on ne crash pas l'application entière
        }
    }
}