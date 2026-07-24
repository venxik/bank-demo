package com.ocbc.bankdemo.notification;

import com.ocbc.bankdemo.entity.Account;

/**
 * STRATEGY PATTERN + SOLID tie-in:
 *
 * ISP (Interface Segregation) — one method, nothing a channel wouldn't need.
 * OCP (Open/Closed) — adding a new channel (push, webhook, ...) means adding a new
 *   class that implements this interface. AccountService (the caller) never changes.
 * DIP (Dependency Inversion) — AccountService depends on this abstraction, not on
 *   ConsoleNotificationService or SmsNotificationService directly. Spring injects
 *   whichever concrete beans exist at runtime.
 * LSP (Liskov Substitution) — every implementation is interchangeable here: Spring
 *   collects all beans of this type into a List<NotificationService> and calls each
 *   one the same way, with no implementation needing special-case handling.
 *
 * Go analogy: this is exactly implicit interface satisfaction — any type with a
 * matching method shape "is a" NotificationService, no explicit `implements`
 * relationship declared anywhere but here.
 */
public interface NotificationService {
    void notify(Account account, String message);
}
