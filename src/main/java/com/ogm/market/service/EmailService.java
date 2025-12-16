package com.ogm.market.service;

import com.ogm.market.model.ContactForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;


import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    // change to the email that should receive contact submissions
    private final String RECEIVER_EMAIL = "aravindareddy.lingareddigari@gmail.com";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactEmail(ContactForm form) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();

        // true = multipart (for inline images + alternative text)
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());

        // Set basic headers
        helper.setFrom(mailSenderUsername());            // sender (the configured SMTP user)
        helper.setTo(RECEIVER_EMAIL);                    // recipient
//        helper.setReplyTo(form.getEmail());              // reply goes to the submitter
        if (form.getEmail() != null && !form.getEmail().isBlank()) {
            helper.setReplyTo(form.getEmail());
        }

        helper.setSubject("New Contact Request from " + safe(form.getName()));

        // Plain text fallback
        String text = buildPlainText(form);

        // HTML body with inline image reference 'logo'
        String html = buildHtml(form);

        helper.setText(text, html);

        // Inline logo (uses the generated file path you already have)
        File logoFile = new File("src/main/resources/static/logo.png\n");
        if (logoFile.exists()) {
            ClassPathResource logo = new ClassPathResource("static/logo.png");
//            helper.addInline("logo", logo, "image/png");
        }

        // Optional: set priority header (1 = highest)
        msg.addHeader("X-Priority", "1");
        msg.addHeader("X-Mailer", "OGM-Market-Mailer");

        mailSender.send(msg);
    }

    private String buildPlainText(ContactForm f) {
        return "New contact submission\n\n"
                + "Name: " + safe(f.getName()) + "\n"
                + "Email: " + safe(f.getEmail()) + "\n"
                + "Mobile: " + safe(f.getMobile()) + "\n"
                + "Message:\n" + safe(f.getMessage()) + "\n\n"
                + "—\nThis message was sent from the OGM Market contact form.";
    }


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

        <!-- Card -->
        <table width="600" cellpadding="0" cellspacing="0" 
          style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 14px rgba(0,0,0,0.10);">

          <!-- Header -->
          <tr>
            <td style="background:#0A4D92;padding:22px;text-align:center;">
                <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAABAAAAAQACAIAAADwf7zUAADelmNhQlgAAN6WanV...XRhbFNvdXJjZVR5cGV4Rmh0dHA6Ly9jdi5pcHRjLm9yZy9uZXdzY29kZXMvZGln"\s
                                      width="60"\s
                                      height="60"\s
                                      alt="OGM Logo"\s
                                      style="display:block;margin:0 auto;" />
                                                                                                               <h1 style="color:#ffffff;margin:12px 0 0 0;font-size:22px;font-weight:600;">
                  New Contact Request
                </h1>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:28px 32px;color:#333;font-size:15px;line-height:1.6;">

              <p style="margin:0 0 14px 0;">
                You have received a new contact request from <strong>{{NAME}}</strong>.
              </p>

              <table width="100%%" style="margin-top:18px;">
                <tr>
                  <td style="color:#0A4D92;font-weight:600;width:120px;">Name:</td>
                  <td>{{NAME}}</td>
                </tr>

                <tr>
                  <td style="color:#0A4D92;font-weight:600;width:120px;">Email:</td>
                  <td><a href="mailto:{{EMAIL}}" style="color:#0A4D92;text-decoration:none;">{{EMAIL}}</a></td>
                </tr>

                <tr>
                  <td style="color:#0A4D92;font-weight:600;width:120px;">Mobile:</td>
                  <td>{{MOBILE}}</td>
                </tr>
              </table>

              <hr style="border:none;border-top:1px solid #e0e0e0;margin:22px 0;" />

              <p style="white-space:pre-wrap;margin-bottom:20px;">
                {{MESSAGE}}
              </p>

            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background:#f8f8f8;text-align:center;padding:18px 0;color:#777;font-size:12px;">
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
//                .replace("{{EMAIL}}", escapeHtml(f.getEmail()))
                .replace("{{EMAIL}}", escapeHtml(
                        (f.getEmail() == null || f.getEmail().isBlank()) ? "Not provided" : f.getEmail()
                ))

                .replace("{{MOBILE}}", escapeHtml(f.getMobile()))
                .replace("{{MESSAGE}}", escapeHtml(f.getMessage()));
    }




    // Helper: get mail sender username from mailSender impl (if available)
    private String mailSenderUsername() {
        try {
            // JavaMailSenderImpl has getUsername(); reflection used to avoid tying to impl type explicitly
            Object impl = mailSender;
            var m = impl.getClass().getMethod("getUsername");
            Object username = m.invoke(impl);
            return username != null ? username.toString() : "no-reply@example.com";
        } catch (Exception ex) {
            return "no-reply@example.com";
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // Minimal HTML escaping for safety
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
