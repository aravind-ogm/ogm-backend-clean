package com.ogm.market.service;

import com.ogm.market.model.ContactForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String[] RECEIVER_EMAILS = {
            "hemanth.ogm@gmail.com",
            "aravind.ogm@gmail.com"
    };

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactEmail(ContactForm form) throws MessagingException {

        MimeMessage msg = mailSender.createMimeMessage();

        // multipart = true (required for inline images)
        MimeMessageHelper helper =
                new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());

        // From (SMTP user)
        helper.setFrom(mailSenderUsername());

        // To (internal team)
        helper.setTo(RECEIVER_EMAILS);

        // Reply-To (user email if provided)
        if (form.getEmail() != null && !form.getEmail().isBlank()) {
            helper.setReplyTo(form.getEmail());
        }

        helper.setSubject("New Contact Request from " + safe(form.getName()));

        // Content
        String text = buildPlainText(form);
        String html = buildHtml(form);

        helper.setText(text, html);

        // ✅ INLINE LOGO (CID)
        ClassPathResource logo = new ClassPathResource("static/logo.png");
        helper.addInline("ogmLogo", logo, "image/png");

        // Optional headers
        msg.addHeader("X-Priority", "1");
        msg.addHeader("X-Mailer", "OGM-Market-Mailer");

        mailSender.send(msg);
    }

    /* ----------------------------------------------------
       PLAIN TEXT (Fallback)
    ---------------------------------------------------- */
    private String buildPlainText(ContactForm f) {
        return "New contact submission\n\n"
                + "Name: " + safe(f.getName()) + "\n"
                + "Email: " + safe(f.getEmail()) + "\n"
                + "Mobile: " + safe(f.getMobile()) + "\n\n"
                + "Message:\n"
                + safe(f.getMessage()) + "\n\n"
                + "—\nSent from OGM Market Contact Form";
    }

    /* ----------------------------------------------------
       HTML EMAIL
    ---------------------------------------------------- */
    private String buildHtml(ContactForm f) {

        String template = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>OGM Contact Request</title>
</head>

<body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">

  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:20px 0;">
    <tr>
      <td align="center">

        <table width="600" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 14px rgba(0,0,0,0.10);">

          <!-- HEADER -->
          <tr>
            <td style="background:#0A4D92;padding:22px;text-align:center;">
              <img
                src="cid:ogmLogo"
                width="90"
                alt="OGM Logo"
                style="display:block;margin:0 auto 10px;"
              />
              <h1 style="color:#ffffff;margin:12px 0 0 0;font-size:22px;font-weight:600;">
                New Contact Request
              </h1>
            </td>
          </tr>

          <!-- BODY -->
          <tr>
            <td style="padding:28px 32px;color:#333;font-size:15px;line-height:1.6;">

              <p>
                You have received a new contact request from
                <strong>{{NAME}}</strong>.
              </p>

              <table width="100%%" style="margin-top:18px;">
                <tr>
                  <td style="color:#0A4D92;font-weight:600;width:120px;">Name:</td>
                  <td>{{NAME}}</td>
                </tr>
                <tr>
                  <td style="color:#0A4D92;font-weight:600;">Email:</td>
                  <td>{{EMAIL}}</td>
                </tr>
                <tr>
                  <td style="color:#0A4D92;font-weight:600;">Mobile:</td>
                  <td>{{MOBILE}}</td>
                </tr>
              </table>

              <hr style="border:none;border-top:1px solid #e0e0e0;margin:22px 0;" />

              <p style="white-space:pre-wrap;">
                {{MESSAGE}}
              </p>

            </td>
          </tr>

          <!-- FOOTER -->
          <tr>
            <td style="background:#f8f8f8;text-align:center;padding:18px;color:#777;font-size:12px;">
              Sent via <strong>OGM Market</strong> Contact Form
            </td>
          </tr>

        </table>

      </td>
    </tr>
  </table>

</body>
</html>
""";

        return template
                .replace("{{NAME}}", escapeHtml(f.getName()))
                .replace("{{EMAIL}}", escapeHtml(
                        (f.getEmail() == null || f.getEmail().isBlank())
                                ? "Not provided"
                                : f.getEmail()
                ))
                .replace("{{MOBILE}}", escapeHtml(f.getMobile()))
                .replace("{{MESSAGE}}", escapeHtml(f.getMessage()));
    }

    /* ----------------------------------------------------
       HELPERS
    ---------------------------------------------------- */

    private String mailSenderUsername() {
        try {
            Object impl = mailSender;
            var m = impl.getClass().getMethod("getUsername");
            Object username = m.invoke(impl);
            return username != null ? username.toString() : "no-reply@ogm.com";
        } catch (Exception ex) {
            return "no-reply@ogm.com";
        }
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
