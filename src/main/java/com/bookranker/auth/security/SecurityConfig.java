package com.bookranker.auth.security;

import com.bookranker.auth.repository.TeacherRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final boolean localProfileActive;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, Environment environment) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.localProfileActive = environment.acceptsProfiles(Profiles.of("local"));
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  .requestMatchers("/api/teachers/register", "/api/teachers/login")
                  .permitAll()
                  .requestMatchers("/api/teachers/me/**")
                  .authenticated()
                  .requestMatchers(HttpMethod.POST, "/api/classes/join")
                  .permitAll()
                  .requestMatchers("/api/students/**")
                  .permitAll()
                  .requestMatchers("/api/classes/**")
                  .authenticated();

              if (localProfileActive) {
                auth.requestMatchers("/h2-console/**").permitAll();
              } else {
                auth.requestMatchers("/h2-console/**").denyAll();
              }

              auth.anyRequest().permitAll();
            })
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    if (localProfileActive) {
      http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
    }

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(TeacherRepository teacherRepository) {
    return username ->
        teacherRepository
            .findByEmail(username)
            .map(
                teacher ->
                    User.withUsername(teacher.getEmail())
                        .password(teacher.getPasswordHash())
                        .roles("TEACHER")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Teacher not found"));
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value(
              "${bookranker.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}")
          String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(splitCsv(allowedOrigins));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> splitCsv(String value) {
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();
  }
}
