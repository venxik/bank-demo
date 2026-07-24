package com.ocbc.bankdemo.notification;

import com.ocbc.bankdemo.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * One concrete Strategy — stands in for a push/email channel. @Component means
 * Spring manages exactly one instance of this class for the whole app (the
 * Singleton pattern — you get it for free from the container's default bean
 * scope, no manual singleton boilerplate needed like you'd hand-write in
 * plain Java with a private constructor + static getInstance()).
 */
@Component
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);

    @Override
    public void notify(Account account, String message) {
        log.info("[console-channel] account {}: {}", account.getAccountNumber(), message);
    }
}
