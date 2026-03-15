package com.ogm.market.service;

import com.ogm.market.model.ContactForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.receiver-email:aravindareddy.lingareddigari@gmail.com}")
    private String contactReceiverEmail;

    /* ── OTP email ───────────────────────────────────────────── */

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("OGM Login OTP");
        msg.setText("Your OTP is " + otp + ". Valid for 5 minutes. Do not share it with anyone.");
        mailSender.send(msg);
        log.debug("OTP email dispatched to: {}", to);
    }

    /* ── Contact form email ──────────────────────────────────── */

    public void sendContactEmail(ContactForm form) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());

        helper.setFrom(senderAddress());
        helper.setTo(contactReceiverEmail);

        String replyTo = form.getEmail();
        if (replyTo != null && !replyTo.isBlank()) {
            helper.setReplyTo(replyTo);
        }

        helper.setSubject("New Contact Request from " + safe(form.getName()));
        helper.setText(buildPlainText(form), buildHtml(form));

        // Inline logo – only attach if the resource actually exists on the classpath
        ClassPathResource logo = new ClassPathResource("static/logo.png");
        if (logo.exists()) {
            helper.addInline("ogmLogo", logo, "image/png");
        }

        msg.addHeader("X-Priority", "1");
        msg.addHeader("X-Mailer", "OGM-Market-Mailer");

        mailSender.send(msg);
        log.info("Contact email sent from: {}", safe(form.getEmail()));
    }

    /* ── Private helpers ─────────────────────────────────────── */

    private String senderAddress() {
        if (mailSender instanceof JavaMailSenderImpl impl) {
            String username = impl.getUsername();
            return username != null ? username : "no-reply@oneglobalmarketplace.com";
        }
        return "no-reply@oneglobalmarketplace.com";
    }

    private String buildPlainText(ContactForm f) {
        return "New contact submission\n\n"
                + "Name:    " + safe(f.getName()) + "\n"
                + "Email:   " + safe(f.getEmail()) + "\n"
                + "Mobile:  " + safe(f.getMobile()) + "\n"
                + "Message:\n" + safe(f.getMessage()) + "\n\n"
                + "—\nSent via the OGM Market contact form.";
    }

    private String buildHtml(ContactForm f) {
        String name    = escapeHtml(safe(f.getName()));
        String email   = escapeHtml(f.getEmail() == null || f.getEmail().isBlank() ? "Not provided" : f.getEmail());
        String mobile  = escapeHtml(safe(f.getMobile()));
        String message = escapeHtml(safe(f.getMessage()));

        return """
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8" /><title>OGM Contact Request</title></head>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:20px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0"
        style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 14px rgba(0,0,0,0.10);">
        <tr>
          <td style="background:#0A4D92;padding:22px;text-align:center;">
            <img src="cid:ogmLogo" width="60" height="60" alt="OGM Logo"
                 style="display:block;margin:0 auto;" />
            <h1 style="color:#ffffff;margin:12px 0 0 0;font-size:22px;font-weight:600;">
              New Contact Request
            </h1>
          </td>
        </tr>
        <tr>
          <td style="padding:28px 32px;color:#333;font-size:15px;line-height:1.6;">
            <p style="margin:0 0 14px 0;">
              You have received a new contact request from <strong>%s</strong>.
            </p>
            <table width="100%%" style="margin-top:18px;">
              <tr>
                <td style="color:#0A4D92;font-weight:600;width:120px;">Name:</td>
                <td>%s</td>
              </tr>
              <tr>
                <td style="color:#0A4D92;font-weight:600;">Email:</td>
                <td><a href="mailto:%s" style="color:#0A4D92;text-decoration:none;">%s</a></td>
              </tr>
              <tr>
                <td style="color:#0A4D92;font-weight:600;">Mobile:</td>
                <td>%s</td>
              </tr>
            </table>
            <hr style="border:none;border-top:1px solid #e0e0e0;margin:22px 0;" />
            <p style="white-space:pre-wrap;margin-bottom:20px;">%s</p>
          </td>
        </tr>
        <tr>
          <td style="background:#f8f8f8;text-align:center;padding:18px 0;color:#777;font-size:12px;">
            Sent via <strong>OGM Market</strong> Contact Form
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
""".formatted(name, name, email, email, mobile, message);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}