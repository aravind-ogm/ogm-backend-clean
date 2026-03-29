package com.ogm.market.broker;

import com.ogm.market.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerNotificationService {

    private final EmailService    emailService;   // existing service in com.ogm.market.auth
    private final SmsService      smsService;
    private final WhatsAppService whatsAppService;

    @Async
    public void sendAllRegistrationNotifications(Broker broker) {
        log.info("[Notification] Firing all notifications for broker={}", broker.getId());

        // 1. Welcome email to broker
        emailService.sendBrokerWelcomeEmail(broker);

        // 2. Admin notification email
        emailService.sendBrokerAdminNotification(broker);

        // 3. SMS (logs in dev if MSG91 not configured)
        smsService.sendWelcomeSms(broker);

        // 4. WhatsApp (logs in dev if Meta API not configured)
        whatsAppService.sendWelcomeWhatsApp(broker);

        log.info("[Notification] All notifications dispatched for broker={}", broker.getId());
    }
}