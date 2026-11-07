package com.cse3230.university.controller.rest;

import com.cse3230.university.dto.AccountResponse;
import com.cse3230.university.exception.NotFoundException;
import com.cse3230.university.repository.UserAccountRepository;
import com.cse3230.university.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UserAccountRepository accounts;
    public AuthRestController(UserAccountRepository accounts) { this.accounts = accounts; }

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return accounts.findById(principal.getId()).map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("Account no longer exists"));
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    public record CsrfResponse(String headerName, String parameterName, String token) { }
}
