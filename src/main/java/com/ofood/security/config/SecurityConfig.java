package com.ofood.security.config;

import com.ofood.security.handler.AuthEntryPoint;
import com.ofood.security.handler.RestAccessDeniedHandler;
import com.ofood.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthConverter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          AuthEntryPoint authEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthConverter) {
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // CSRF strategy: enable CSRF with Cookie repository, but ignore CSRF for stateless JWT auth and public auth endpoints
        RequestMatcher hasAuthorizationHeader = request -> request.getHeader("Authorization") != null;
        RequestMatcher publicAuthEndpoints = request -> {
            if (!HttpMethod.POST.matches(request.getMethod())) {
                return false;
            }
            String uri = request.getRequestURI();
            return "/api/v1/auth/register".equals(uri)
                    || "/api/v1/auth/login".equals(uri)
                    || "/api/v1/auth/refresh".equals(uri);
        };

        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(hasAuthorizationHeader, publicAuthEndpoints)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/change-password").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/cities", "/api/v1/cities/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/pincodes", "/api/v1/pincodes/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/serviceability").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                .requestMatchers(HttpMethod.GET, "/.well-known/openid-configuration").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .authenticationProvider(authenticationProvider())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .headers(headers -> headers.frameOptions().sameOrigin())
            ;

        return http.build();
    }
}
