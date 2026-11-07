package com.cse3230.university.security;

import com.cse3230.university.repository.UserAccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revoke old sessions on the next request after deletion or a credential change. */
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserAccountRepository accounts;
    public AccountStatusFilter(UserAccountRepository accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            var current = accounts.findById(principal.getId());
            boolean valid = current.filter(a -> a.isEnabled()
                    && a.getEmail().equals(principal.getUsername())
                    && a.getPasswordHash().equals(principal.getPassword())
                    && principal.getAuthorities().stream().anyMatch(
                            authority -> authority.getAuthority().equals("ROLE_" + a.getRole().name()))).isPresent();
            if (!valid) {
                HttpSession session = request.getSession(false);
                if (session != null) { session.invalidate(); }
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
