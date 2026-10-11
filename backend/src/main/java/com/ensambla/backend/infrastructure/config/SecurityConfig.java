package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.infrastructure.security.SecurityErrorHandler;
import com.ensambla.backend.infrastructure.security.SesionRequeridaFilter;
import com.ensambla.backend.infrastructure.security.UsuarioActivoFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", bcrypt));
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }

    @Bean
    public FilterRegistrationBean<UsuarioActivoFilter> usuarioActivoFilterRegistration(
            UsuarioActivoFilter usuarioActivoFilter) {
        FilterRegistrationBean<UsuarioActivoFilter> registration = new FilterRegistrationBean<>(usuarioActivoFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityErrorHandler securityErrorHandler, UsuarioActivoFilter usuarioActivoFilter)
            throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher rutasPublicas = new OrRequestMatcher(
                paths.matcher(HttpMethod.GET, "/api/plantillas"),
                paths.matcher(HttpMethod.GET, "/api/plantillas/**"),
                paths.matcher("/swagger-ui.html"),
                paths.matcher("/swagger-ui/**"),
                paths.matcher("/v3/api-docs/**"),
                paths.matcher(HttpMethod.GET, "/api/auth/csrf"),
                paths.matcher(HttpMethod.POST, "/api/auth/register"),
                paths.matcher(HttpMethod.POST, "/api/auth/login"),
                paths.matcher(HttpMethod.POST, "/api/auth/logout"));
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(rutasPublicas).permitAll()
                .anyRequest().authenticated())
                .csrf(Customizer.withDefaults())
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .addFilterBefore(usuarioActivoFilter, AnonymousAuthenticationFilter.class);
        http.addFilterBefore(new SesionRequeridaFilter(rutasPublicas, securityErrorHandler), CsrfFilter.class);
        return http.build();
    }
}
