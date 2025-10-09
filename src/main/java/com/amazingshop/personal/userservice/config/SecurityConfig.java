package com.amazingshop.personal.userservice.config;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.security.jwt.JwtFilter;
import com.amazingshop.personal.userservice.services.PeopleDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final PeopleDetailsService peopleDetailService;
    private final JwtFilter jwtFilter;

    @Autowired
    public SecurityConfig(PeopleDetailsService peopleDetailService, JwtFilter jwtFilter) {
        this.peopleDetailService = peopleDetailService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(configurationSource()))
                .userDetailsService(peopleDetailService)
                .oauth2Login(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (Публичные эндпоинты)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/users/health", "/api/v1/auth/health").permitAll()

                        // OAuth2 endpoints (публичные)
                        .requestMatchers("/api/v1/auth/oauth2/**").permitAll()

                        // Admin endpoints (Админские эндпоинты)
                        .requestMatchers("/api/v1/users/admin/**").hasRole(Role.ADMIN.toString())

                        // Custom endpoints (Пользовательские эндпоинты)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").hasAnyRole(Role.USER.toString(), Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").hasAnyRole(Role.USER.toString(), Role.ADMIN.toString())

                        // The remaining endpoints require authentication (Остальные эндпоинты требуют аутентификации)
                        .anyRequest().authenticated())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Adding the JWT filter (Добавляем JWT фильтр)
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource configurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}