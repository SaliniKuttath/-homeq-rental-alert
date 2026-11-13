package com.homeq.alert.notify;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends alerts by email over SMTP (e.g. Gmail with an app password). */
@Component
public class EmailNotifier implements AlertChannel {
    private final JavaMailSender mailSender;
    private final String from;
    private final String to;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String from,
                         @Value("${alert.email-to:}") String to){
        this.mailSender=mailSender;this.from=from;this.to=to;
    }

    @Override public String name(){return "email";}

    @Override public void send(String subject,String body){
        if(from.isBlank()||to.isBlank())
            throw new IllegalStateException("Email alerts need MAIL_USERNAME, MAIL_PASSWORD and ALERT_EMAIL_TO in the environment.");
        SimpleMailMessage msg=new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to.split("\\s*,\\s*"));
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
