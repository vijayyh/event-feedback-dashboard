package com.eventfeedback.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Two roles:
 * <ul>
 *   <li><b>Attendee</b> – anonymous. Browses events, reads feedback, submits feedback.</li>
 *   <li><b>Organiser</b> – signs in (user {@code organiser}, password from configuration, never committed).
 *       Creates events and changes their status.</li>
 * </ul>
 * The browser UI uses fetch() with a session cookie, so CSRF protection uses the
 * {@code XSRF-TOKEN} cookie / {@code X-XSRF-TOKEN} header pair.
 */
@Configuration
public class SecurityConfig {

    static final String ORGANISER = "ORGANISER";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Load the CSRF token on every request so the XSRF-TOKEN cookie is always present for the JS client
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        RequestMatcher apiRequests = request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/events").hasRole(ORGANISER)
                .requestMatchers(HttpMethod.PATCH, "/api/events/*/status").hasRole(ORGANISER)
                .requestMatchers("/organiser.html").hasRole(ORGANISER)
                // organiser dashboard: indicators, drill-down, feedback search and follow-up workflow
                .requestMatchers("/api/dashboard/**", "/dashboard.html").hasRole(ORGANISER)
                .requestMatchers(HttpMethod.GET, "/api/feedback").hasRole(ORGANISER)
                .requestMatchers(HttpMethod.PATCH, "/api/feedback/*/status").hasRole(ORGANISER)
                .anyRequest().permitAll())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler))
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
                .failureHandler((request, response, exception) ->
                        writeError(response, HttpStatus.UNAUTHORIZED, "Wrong username or password"))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
            .exceptionHandling(ex -> ex
                // API calls get a plain 401; pages (organiser.html) are redirected to the sign-in page
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests)
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login.html"), AnyRequestMatcher.INSTANCE));
        return http.build();
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }
}
