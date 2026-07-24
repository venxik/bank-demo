package com.ocbc.bankdemo.notification;

import com.ocbc.bankdemo.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Second Strategy, same interface. This is the OCP proof: this class did not
 * exist when AccountService was written, and adding it required zero edits to
 * AccountService — Spring's component scan just picks it up and it joins the
 * List<NotificationService> AccountService already depends on.
 */
@Component
public class SmsNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    @Override
    public void notify(Account account, String message) {
        log.info("[sms-channel] would SMS owner of {}: {}", account.getAccountNumber(), message);
    }
}
