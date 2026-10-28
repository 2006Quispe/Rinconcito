package com.rinconcito.config;

import com.rinconcito.service.correo.OutlookOAuthTokenService;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
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
        return new com.rinconcito.service.correo.OutlookGraphMailSender(tokens, clienteMicrosoft());
    }

    private RestClient clienteMicrosoft() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var solicitudes = new JdkClientHttpRequestFactory(http);
        solicitudes.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(solicitudes).build();
    }
}
