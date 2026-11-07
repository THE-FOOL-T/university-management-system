package com.cse3230.university.config;

import com.cse3230.university.repository.UserAccountRepository;
import com.cse3230.university.security.*;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityErrors errors,
                                                   UserAccountRepository accounts) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/login", "/css/**", "/error", "/api/auth/csrf", "/actuator/health").permitAll()
                .requestMatchers("/api/admins", "/api/admins/**", "/admins", "/admins/**").hasRole("ADMIN")
                .requestMatchers("/api/teachers", "/api/teachers/**", "/teachers", "/teachers/**")
                    .hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students", "/api/students/**", "/students", "/students/**")
                    .hasAnyRole("ADMIN", "STUDENT")
                .requestMatchers("/api/auth/me", "/", "/dashboard", "/access-denied").authenticated()
                .requestMatchers("/api/**").denyAll()
                .anyRequest().denyAll())
            .csrf(Customizer.withDefaults())
            .formLogin(form -> form.loginPage("/login").usernameParameter("email")
                .defaultSuccessUrl("/dashboard", true).failureUrl("/login?error").permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                        errors.unauthorized(request, response);
                    } else {
                        new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, exception);
                    }
                })
                .accessDeniedHandler(errors::denied))
            .addFilterBefore(new AccountStatusFilter(accounts), CsrfFilter.class);
        return http.build();
    }
}
