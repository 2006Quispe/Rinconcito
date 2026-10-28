package com.rinconcito.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        authenticationProvider.setPasswordEncoder(
                passwordEncoder
        );

        return new ProviderManager(
                authenticationProvider
        );
    }

    @Bean
    public AuthenticationSuccessHandler
    clienteSuccessHandler() {

        return new RoleBasedAuthenticationSuccessHandler(
                false
        );
    }

    @Bean
    public AuthenticationSuccessHandler
    personalSuccessHandler() {

        return new RoleBasedAuthenticationSuccessHandler(
                true
        );
    }

    /*
     * Cadena exclusiva para el inicio de sesión del personal del restaurante.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain personalLoginFilterChain(
            HttpSecurity http,

            @Qualifier("personalSuccessHandler")
            AuthenticationSuccessHandler
                    personalSuccessHandler
    ) throws Exception {

        http
                .securityMatcher(
                        "/personal/login",
                        "/personal/login/**"
                )

                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                )

                .csrf(Customizer.withDefaults())

                .formLogin(form -> form
                        .loginPage("/personal/login")
                        .loginProcessingUrl(
                                "/personal/login"
                        )
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .successHandler(
                                personalSuccessHandler
                        )
                        .failureUrl(
                                "/personal/login?error"
                        )
                        .permitAll()
                );

        return http.build();
    }

    /*
     * Cadena principal del sistema.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,

            @Qualifier("clienteSuccessHandler")
            AuthenticationSuccessHandler
                    clienteSuccessHandler
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        /*Recursos y páginas públicas.*/
                		.requestMatchers(
                		        "/",
                		        "/nosotros",
                		        "/carta",
                		        "/eventos",
                		        "/reservas",
                		        "/reservas/**",
                		        "/pedidos",
                		        "/pedidos/**",
                		        "/carrito",
                		        "/carrito/**",
                		        "/galeria/**",
                		        "/registro",
                		        "/login",
                		        "/css/**",
                		        "/js/**",
                		        "/img/**",
                		        "/webjars/**",
                		        "/configuracion/qr/**",
                		        "/error"
                		).permitAll()

                        /*El administrador únicamente entra a las rutas de administración.*/
                        .requestMatchers("/admin/**")
                        .hasRole("ADMINISTRADOR")

                        /*
                         * Rutas exclusivas del mesero.
                         */
                        .requestMatchers("/mesero/**")
                        .hasRole("MESERO")

                        /*
                         * Rutas exclusivas de Caja.
                         * El administrador ya no será enviado
                         * al panel de Caja.
                         */
                        .requestMatchers("/caja/**")
                        .hasRole("CAJA")

                        /*
                         * Cuenta del cliente.
                         */
                        .requestMatchers("/mi-cuenta/**")
                        .hasRole("CLIENTE")

                        .anyRequest()
                        .permitAll()
                )

                .csrf(Customizer.withDefaults())

                /*
                 * Inicio de sesión de clientes.
                 */
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .successHandler(
                                clienteSuccessHandler
                        )
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint(
                                (request,
                                 response,
                                 exception) -> {

                                    String ruta =
                                            obtenerRutaRelativa(
                                                    request.getRequestURI(),
                                                    request.getContextPath()
                                            );

                                    if (esRutaPersonal(ruta)) {
                                        response.sendRedirect(
                                                request.getContextPath()
                                                        + "/personal/login"
                                        );
                                    } else {
                                        response.sendRedirect(
                                                request.getContextPath()
                                                        + "/login"
                                        );
                                    }
                                }
                        )

                        .accessDeniedHandler(
                                (request,
                                 response,
                                 exception) -> {

                                    String ruta =
                                            obtenerRutaRelativa(
                                                    request.getRequestURI(),
                                                    request.getContextPath()
                                            );

                                    if (esRutaPersonal(ruta)) {
                                        response.sendRedirect(
                                                request.getContextPath()
                                                        + "/personal/login"
                                                        + "?sinPermiso"
                                        );
                                    } else {
                                        response.sendRedirect(
                                                request.getContextPath()
                                                        + "/login"
                                                        + "?sinPermiso"
                                        );
                                    }
                                }
                        )
                );

        return http.build();
    }

    private static String obtenerRutaRelativa(
            String ruta,
            String contexto
    ) {
        if (
                contexto != null
                        && !contexto.isBlank()
                        && ruta.startsWith(contexto)
        ) {
            return ruta.substring(
                    contexto.length()
            );
        }

        return ruta;
    }

    private static boolean esRutaPersonal(
            String ruta
    ) {
        return ruta.startsWith("/admin")
                || ruta.startsWith("/mesero")
                || ruta.startsWith("/caja");
    }
}
