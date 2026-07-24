package com.ocbc.bankdemo.antipattern;

import com.ocbc.bankdemo.dto.AccountResponse;
import com.ocbc.bankdemo.exception.AccountNotFoundException;
import com.ocbc.bankdemo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contrast case for {@link com.ocbc.bankdemo.service.AccountService}, which uses
 * constructor injection. Not wired into any controller — exists only to point at
 * in an interview when asked "what's wrong with field injection?":
 *
 * 1. {@code accountRepository} can't be {@code final} — nothing stops another
 *    method in this class (or a subclass) from reassigning it after construction.
 * 2. {@code new BrokenAccountLookupService()} compiles and returns a live object
 *    with a null repository — the NPE only surfaces on first use, not at
 *    construction. Constructor injection makes that state unrepresentable.
 * 3. Unit testing without a Spring context means reflection
 *    (`ReflectionTestUtils.setField`) to poke the private field, instead of just
 *    calling `new AccountService(mockRepo, ...)`.
 * 4. A circular dependency between two field-injected beans boots successfully
 *    and fails later at first use; the same cycle with constructor injection
 *    fails fast at startup with a clear "not eligible for auto-proxying" /
 *    circular reference error.
 */
@Service
public class BrokenAccountLookupService {

    @Autowired
    private AccountRepository accountRepository;

    public AccountResponse lookup(Long id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
