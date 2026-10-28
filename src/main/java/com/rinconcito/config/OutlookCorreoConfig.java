package com.rinconcito.config;

import com.rinconcito.service.correo.OutlookOAuthTokenService;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix="app.correo", name={"habilitado", "oauth.habilitado"}, havingValue="true")
public class OutlookCorreoConfig {
    @Bean
    public OutlookOAuthTokenService outlookTokens(
            @Value("${app.correo.oauth.archivo:.local/outlook-oauth.properties}") String archivo,
            @Value("${spring.mail.username}") String cuenta) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var solicitudes = new JdkClientHttpRequestFactory(http);
        solicitudes.setReadTimeout(Duration.ofSeconds(15));
        return new OutlookOAuthTokenService(Path.of(archivo), cuenta,
                RestClient.builder().requestFactory(solicitudes).build());
    }

    @Bean
    public JavaMailSender outlookMailSender(OutlookOAuthTokenService tokens,
            @Value("${spring.mail.username}") String cuenta,
            @Value("${app.correo.remitente}") String remitente) throws Exception {
        if (!cuenta.equalsIgnoreCase(new InternetAddress(remitente, true).getAddress())) {
            throw new IllegalStateException("El remitente debe coincidir con la cuenta de Hotmail autorizada.");
        }
        var sender = new JavaMailSenderImpl() {
            @Override
            protected Transport connectTransport() throws MessagingException {
                String token = tokens.obtenerToken();
                Transport transporte = getTransport(getSession());
                try {
                    transporte.connect(getHost(), getPort(), getUsername(), token);
                    return transporte;
                } catch (MessagingException error) {
                    if (error instanceof AuthenticationFailedException) tokens.invalidar();
                    try { transporte.close(); } catch (Exception ignored) { }
                    throw error;
                }
            }
        };
        sender.setHost("smtp-mail.outlook.com");
        sender.setPort(587);
        sender.setUsername(cuenta);
        sender.setDefaultEncoding("UTF-8");
        var props = sender.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.auth.mechanisms", "XOAUTH2");
        props.setProperty("mail.smtp.auth.login.disable", "true");
        props.setProperty("mail.smtp.auth.plain.disable", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.starttls.required", "true");
        props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        props.setProperty("mail.smtp.connectiontimeout", "10000");
        props.setProperty("mail.smtp.timeout", "15000");
        props.setProperty("mail.smtp.writetimeout", "15000");
        props.setProperty("mail.debug", "false");
        return sender;
    }
}
