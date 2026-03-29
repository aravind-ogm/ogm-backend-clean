package com.ogm.market.auth;

import com.ogm.market.broker.Broker;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.receiver-email:aravindareddy.lingareddigari@gmail.com}")
    private String contactReceiverEmail;

    @Value("${notification.admin-email:aravindareddy.lingareddigari@gmail.com}")
    private String adminEmail;

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

        ClassPathResource logo = new ClassPathResource("static/logo.png");
        if (logo.exists()) {
            helper.addInline("ogmLogo", logo, "image/png");
        }

        msg.addHeader("X-Priority", "1");
        msg.addHeader("X-Mailer", "OGM-Market-Mailer");

        mailSender.send(msg);
        log.info("Contact email sent from: {}", safe(form.getEmail()));
    }

    /* ── Broker Welcome Email ────────────────────────────────── */

    @Async
    public void sendBrokerWelcomeEmail(Broker broker) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());

            helper.setFrom(senderAddress());
            helper.setTo(broker.getEmail());
            helper.setSubject("Welcome to One Global Marketplace — Registration Received!");
            helper.setText(buildBrokerWelcomeHtml(broker), true);

            mailSender.send(msg);
            log.info("[Email] Broker welcome email sent to {}", broker.getEmail());
        } catch (MessagingException e) {
            log.error("[Email] Failed to send broker welcome email to {}: {}", broker.getEmail(), e.getMessage());
        }
    }

    /* ── Admin Notification Email ────────────────────────────── */

    @Async
    public void sendBrokerAdminNotification(Broker broker) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());

            helper.setFrom(senderAddress());
            helper.setTo(adminEmail);
            helper.setSubject("🔔 New Broker Registered — " + broker.getFullName());
            helper.setText(buildAdminNotificationHtml(broker), true);

            mailSender.send(msg);
            log.info("[Email] Admin notification sent for broker {}", broker.getId());
        } catch (MessagingException e) {
            log.error("[Email] Failed to send admin notification: {}", e.getMessage());
        }
    }

    /* ── Email Templates ─────────────────────────────────────── */

    private String buildBrokerWelcomeHtml(Broker broker) {
        String shortId = broker.getId().toString().substring(0, 8).toUpperCase();
        String mobile  = broker.getMobile() != null ? broker.getMobile() : "your registered number";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:20px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                    style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#1B3A5C,#2d5986);padding:36px;text-align:center;">
                        <h1 style="color:#fff;margin:0;font-size:22px;">One Global Marketplace</h1>
                        <p style="color:#a8c4e0;margin:8px 0 0;font-size:14px;">Your Trusted Real Estate Partner</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px;">
                        <p style="font-size:18px;font-weight:600;color:#1B3A5C;margin:0 0 16px;">
                          Welcome, %s! 🎉
                        </p>
                        <p style="color:#4a5568;font-size:15px;line-height:1.7;margin:0 0 16px;">
                          Thank you for registering as a broker on <strong>One Global Marketplace</strong>.
                          Your application is currently under review.
                        </p>
                        <div style="background:#EEF4FF;border-radius:12px;padding:20px;border-left:4px solid #F47C20;margin:24px 0;">
                          <div style="font-size:12px;font-weight:700;color:#F47C20;text-transform:uppercase;letter-spacing:1px;">Your Broker ID</div>
                          <div style="font-size:22px;font-weight:700;color:#1B3A5C;font-family:monospace;margin-top:6px;">%s</div>
                        </div>
                        <p style="color:#4a5568;font-size:15px;margin:0 0 12px;">What happens next:</p>
                        <table cellpadding="0" cellspacing="0" width="100%%">
                          <tr>
                            <td width="36" valign="top">
                              <div style="width:28px;height:28px;border-radius:50%%;background:#F47C20;color:#fff;text-align:center;line-height:28px;font-weight:700;font-size:13px;">1</div>
                            </td>
                            <td style="color:#4a5568;font-size:14px;padding-top:4px;">Our team verifies your details within <strong>24 hours</strong></td>
                          </tr>
                          <tr><td colspan="2" style="height:10px;"></td></tr>
                          <tr>
                            <td width="36" valign="top">
                              <div style="width:28px;height:28px;border-radius:50%%;background:#F47C20;color:#fff;text-align:center;line-height:28px;font-weight:700;font-size:13px;">2</div>
                            </td>
                            <td style="color:#4a5568;font-size:14px;padding-top:4px;">We will call you on <strong>+91 %s</strong></td>
                          </tr>
                          <tr><td colspan="2" style="height:10px;"></td></tr>
                          <tr>
                            <td width="36" valign="top">
                              <div style="width:28px;height:28px;border-radius:50%%;background:#F47C20;color:#fff;text-align:center;line-height:28px;font-weight:700;font-size:13px;">3</div>
                            </td>
                            <td style="color:#4a5568;font-size:14px;padding-top:4px;">Once approved, start <strong>listing properties</strong> and receiving leads</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;text-align:center;padding:18px;color:#9aa3b0;font-size:12px;border-top:1px solid #e2e8f0;">
                        © 2025 One Global Marketplace
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(broker.getFullName(), shortId, mobile);
    }

    private String buildAdminNotificationHtml(Broker broker) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:20px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                    style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:#1B3A5C;padding:24px;text-align:center;">
                        <h2 style="color:#fff;margin:0;font-size:18px;">🔔 New Broker Registration</h2>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:28px 32px;">
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          %s
                        </table>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(buildAdminRows(broker));
    }

    private String buildAdminRows(Broker broker) {
        String[][] rows = {
                {"Full Name",        broker.getFullName()},
                {"Company",          broker.getCompanyName()},
                {"Mobile",           "+91 " + (broker.getMobile() != null ? broker.getMobile() : "-")},
                {"Email",            broker.getEmail()},
                {"Office Address",   broker.getOfficeAddress() != null ? broker.getOfficeAddress() : "-"},
                {"RERA Number",      broker.getReraNumber() != null ? broker.getReraNumber() : "Not provided"},
                {"Operating Areas",  broker.getOperatingAreas() != null ? String.join(", ", broker.getOperatingAreas()) : "-"},
                {"Property Types",   broker.getPropertyTypes() != null ? String.join(", ", broker.getPropertyTypes()) : "-"},
                {"Auth Provider",    broker.getAuthProvider().name()},
                {"Broker ID",        broker.getId().toString()},
                {"Status",           broker.getStatus().name()},
        };

        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append("""
                <tr style="border-bottom:1px solid #e2e8f0;">
                  <td style="padding:10px 12px;font-weight:600;color:#4a5568;width:40%%;">%s</td>
                  <td style="padding:10px 12px;color:#1B3A5C;">%s</td>
                </tr>
                """.formatted(escapeHtml(row[0]), escapeHtml(row[1])));
        }
        return sb.toString();
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

    /* ── WhatsApp Registration Confirmation Email ────────────── */

    @Async
    public void sendWhatsAppConfirmationEmail(Broker broker) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress());
            helper.setTo(broker.getEmail());
            helper.setSubject("Registration Received via WhatsApp — One Global Marketplace");
            helper.setText(buildWhatsAppConfirmationHtml(broker), true);
            mailSender.send(msg);
            log.info("[Email] WhatsApp confirmation sent to {}", broker.getEmail());
        } catch (MessagingException e) {
            log.error("[Email] WhatsApp confirmation failed for {}: {}", broker.getEmail(), e.getMessage());
        }
    }

    private String buildWhatsAppConfirmationHtml(Broker broker) {
        String shortId = broker.getId().toString().substring(0, 8).toUpperCase();
        String mobile  = broker.getMobile() != null && !broker.getMobile().isBlank()
                ? broker.getMobile() : "your registered number";
        return "<html><body style='font-family:Arial,sans-serif;background:#f4f6f8;padding:20px'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden'>"
                + "<div style='background:linear-gradient(135deg,#25A244,#1e8838);padding:32px;text-align:center'>"
                + "<h1 style='color:#fff;margin:0;font-size:20px'>Registration Request Received!</h1>"
                + "<p style='color:#a8f0bc;margin:8px 0 0;font-size:13px'>via WhatsApp &middot; One Global Marketplace</p>"
                + "</div>"
                + "<div style='padding:32px'>"
                + "<p style='font-size:17px;font-weight:600;color:#1B3A5C;margin:0 0 14px'>Hi " + broker.getFullName() + ",</p>"
                + "<p style='color:#4a5568;font-size:15px;line-height:1.7'>We have received your broker registration request sent via <strong>WhatsApp</strong>. Your details have been recorded and our team will review them shortly.</p>"
                + "<div style='background:#F0FFF4;border-radius:12px;padding:18px;border-left:4px solid #25A244;margin:20px 0'>"
                + "<div style='font-size:12px;font-weight:700;color:#25A244;text-transform:uppercase'>Reference ID</div>"
                + "<div style='font-size:22px;font-weight:700;color:#1B3A5C;font-family:monospace;margin-top:6px'>" + shortId + "</div>"
                + "</div>"
                + "<p style='color:#4a5568;font-size:14px'>Our team will call you on <strong>+91 " + mobile + "</strong> within 24 hours to verify your details.</p>"
                + "<p style='color:#4a5568;font-size:13px;margin-top:16px'>For faster processing, complete your full registration at "
                + "<a href='https://oneglobalmarketplace.com/broker/register' style='color:#F47C20'>oneglobalmarketplace.com/broker/register</a></p>"
                + "</div>"
                + "<div style='background:#f8fafc;text-align:center;padding:16px;color:#9aa3b0;font-size:12px'>"
                + "&copy; 2026 One Global Marketplace</div>"
                + "</div></body></html>";
    }
}