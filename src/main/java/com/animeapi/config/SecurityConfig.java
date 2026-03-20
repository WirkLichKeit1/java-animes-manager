package com.animeapi.config;

import com.animeapi.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas de auth
                .requestMatchers("/api/auth/**").permitAll()

                // Endpoint de erro do Spring — deve ser público para evitar
                // "Unable to handle the Spring Security Exception because the
                // response is already committed" nos logs
                .requestMatchers("/error").permitAll()

                // Health check
                .requestMatchers("/actuator/health").permitAll()

                // Leitura pública de animes e episódios
                .requestMatchers(HttpMethod.GET, "/api/animes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/episodes/**").permitAll()

                // Imagens públicas (capas, banners, thumbnails) — armazenamento local
                .requestMatchers("/images/**").permitAll()

                // Streaming de vídeo exige autenticação
                .requestMatchers("/api/videos/stream/**").authenticated()

                // Apenas ADMIN
                .requestMatchers(HttpMethod.POST, "/api/animes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/animes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/animes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/episodes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/episodes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/episodes/**").hasRole("ADMIN")
                .requestMatchers("/api/videos/upload/**").hasRole("ADMIN")

                // Qualquer outra rota exige login
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "https://*.vercel.app",
            "https://*.replit.dev",
            "https://*.picard.replit.dev"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}