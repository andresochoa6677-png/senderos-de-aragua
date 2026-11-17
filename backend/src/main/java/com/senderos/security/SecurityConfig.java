package com.senderos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Registro único de sesiones de Spring Security (control de concurrencia).
     * Se declara como bean y se INYECTA en el filter chain: nunca debe llamarse
     * al método directamente dentro de la configuración, porque eso crearía una
     * segunda instancia y desincronizaría la expiración de sesiones.
     */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder enc) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(enc);
        provider.setUserDetailsService(uds);
        return new ProviderManager(provider);
    }

    /**
     * "Mantener sesión activa": token persistente por dispositivo. En la BD
     * solo vive el hash; la cookie (REMEMBERME, Secure/HttpOnly) rota en
     * cada uso y permite re-autenticar cuando la sesión HTTP expira.
     */
    @Bean
    PersistentTokenBasedRememberMeServices rememberMeServices(javax.sql.DataSource dataSource,
                                                         UserDetailsService userDetailsService) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        PersistentTokenBasedRememberMeServices servicios =
            new PersistentTokenBasedRememberMeServices("SenderosRememberMe-v1", userDetailsService, repo);
        servicios.setCookieName("REMEMBERME");
        servicios.setUseSecureCookie(true);
        // El control de cuándo emitir la cookie lo hace AuthController (flag
        // "recordar" del JSON); sin esto loginSuccess exigiría un parámetro
        // HTTP remember-me que el login JSON nunca envía.
        servicios.setAlwaysRemember(true);
        return servicios;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    SessionRegistry sessionRegistry,
                                    PersistentTokenBasedRememberMeServices rememberMeServices,
                                    @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOrigins)))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfRequestHandler))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; font-src 'self'; connect-src 'self'; "
                    + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/onboarding/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verificar-correo", "/api/auth/reenviar-verificacion").permitAll()
                .requestMatchers("/api/config").permitAll()
                .requestMatchers("/api/password/solicitud", "/api/password/restablecer").permitAll()
                .requestMatchers("/api/auth/csrf").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            // SPA: sin sesion valida debe llegar 401 (no 403) para redirigir a login.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (req, res, authEx) -> res.sendError(401, "No autenticado")))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .rememberMe(remember -> remember
                .rememberMeServices(rememberMeServices))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                .addLogoutHandler(rememberMeServices)
                .deleteCookies("JSESSIONID", "REMEMBERME"))
            // Unica fuente de verdad de sesiones: maximo 1 por usuario con
            // registry inyectado. La proteccion de fijacion de sesion se aplica
            // en el login manual cambiando el session id real.
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Requested-With", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
