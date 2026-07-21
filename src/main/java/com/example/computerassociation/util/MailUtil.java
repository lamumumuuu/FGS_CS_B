package com.example.computerassociation.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class MailUtil {

    private static TemplateEngine templateEngine;
    private static JavaMailSender mailSender;
    private static String mailFrom;

    @Autowired
    public void setTemplateEngine(TemplateEngine templateEngine) {
        MailUtil.templateEngine = templateEngine;
    }

    @Autowired
    public void setMailSender(JavaMailSender mailSender) {
        MailUtil.mailSender = mailSender;
    }

    @Autowired
    public void setMailFrom(@Value("${spring.mail.from}") String mailFrom) {
        MailUtil.mailFrom = mailFrom;
    }

    public static String renderTemplate(String templateName, Context context) {
        return templateEngine.process(templateName, context);
    }

    public static void sendVerificationEmail(String to, String subject, String verificationCode) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(mailFrom);

        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        context.setVariable("expireTime", 5);
        String emailContent = renderTemplate("verification-code-email.html", context);

        helper.setText(emailContent, true);
        mailSender.send(mimeMessage);
    }
}
