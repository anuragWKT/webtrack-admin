package com.webknot.webtrak_admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/projects", "/projects/bulk").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/projects").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/projects/{code}").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/projects/user/{userId}").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                    .requestMatchers(HttpMethod.GET, "/projects/managed/{userId}").hasAnyRole("ADMIN", "HR", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/projects/{code}/allocations").hasAnyRole("ADMIN", "HR", "MANAGER")

                    .requestMatchers(HttpMethod.POST, "/allocations").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.PUT, "/allocations/{id}").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.DELETE, "/allocations/{id}").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/allocations").hasAnyRole("ADMIN", "HR", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/allocations/expiring").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/allocations/summary/project").hasAnyRole("ADMIN", "HR", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/allocations/summary/user").hasAnyRole("ADMIN", "HR")

                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
