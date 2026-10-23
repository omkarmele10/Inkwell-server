package com.omkarmele.inkwell.config;

import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.security.JwtAuthFilter;
import com.omkarmele.inkwell.security.JwtService;
import com.omkarmele.inkwell.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Cookie auth => CSRF protection is mandatory. The SPA reads XSRF-TOKEN and sends X-XSRF-TOKEN.
            .csrf(csrfConfigurer -> csrfConfigurer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            // The authenticated context lives only for one request; identity comes from the JWT cookie.
            .securityContext(securityContextConfigurer -> securityContextConfigurer
                    .securityContextRepository(new RequestAttributeSecurityContextRepository()))
            .authorizeHttpRequests(authorizationConfigurer -> authorizationConfigurer
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/auth/register", "/api/auth/login",
                                 "/api/auth/forgot-password", "/api/auth/reset-password", "/error", "/actuator/health",
                                 "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/users/suggestions",
                                 "/api/articles/mine", "/api/articles/following").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/hashtags/**",
                                 "/api/articles/**", "/api/users/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2LoginConfigurer -> oauth2LoginConfigurer
                .successHandler(oauth2SuccessHandler)
                .failureHandler((request, response, exception) ->
                        response.sendRedirect(appProperties.frontendUrl() + "/login?error=oauth")))
            .exceptionHandling(exceptionHandlingConfigurer ->
                    exceptionHandlingConfigurer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
