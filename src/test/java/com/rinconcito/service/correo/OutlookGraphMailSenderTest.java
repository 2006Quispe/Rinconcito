package com.rinconcito.service.correo;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.RestClient;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Multipart;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OutlookGraphMailSenderTest {
    @Test void enviaMimeConPdfYTokenDelegado() throws Exception {
        var tokens=mock(OutlookOAuthTokenService.class);
        when(tokens.obtenerToken()).thenReturn("token-simulado");
        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        byte[] pdf="%PDF-1.4 documento de prueba".getBytes();
        server.expect(requestTo(OutlookGraphMailSender.SEND_ENDPOINT))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer token-simulado"))
                .andExpect(request -> {
                    try {
                        byte[] decoded=Base64.getDecoder().decode(((MockClientHttpRequest)request).getBodyAsBytes());
                        var message=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(decoded));
                        assertEquals("Nota de venta",message.getSubject());
                        assertEquals("cliente@example.test",message.getAllRecipients()[0].toString());
                        var parts=(Multipart)message.getContent();
                        assertEquals("Nota.pdf",parts.getBodyPart(1).getFileName());
                        assertArrayEquals(pdf,parts.getBodyPart(1).getInputStream().readAllBytes());
                    } catch (Exception error) {throw new AssertionError(error);}
                }).andRespond(withStatus(HttpStatus.ACCEPTED));
        var sender=new OutlookGraphMailSender(tokens,builder.build());
        var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,true,"UTF-8");
        helper.setFrom("remitente@hotmail.com");helper.setTo("cliente@example.test");
        helper.setSubject("Nota de venta");helper.setText("Pago aprobado.");
        helper.addAttachment("Nota.pdf",new ByteArrayResource(pdf),"application/pdf");
        sender.send(message);server.verify();
    }

    @Test void unTokenRechazadoSeInvalidaSinExponerRespuesta() throws Exception {
        var tokens=mock(OutlookOAuthTokenService.class);
        when(tokens.obtenerToken()).thenReturn("token-simulado");
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OutlookGraphMailSender.SEND_ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("respuesta-sensible"));
        var sender=new OutlookGraphMailSender(tokens,builder.build());
        var message=sender.createMimeMessage(); message.setText("Prueba");
        var error=assertThrows(MailSendException.class,()->sender.send(message));
        assertFalse(error.getMessage().contains("sensible"));assertNull(error.getCause());
        verify(tokens).invalidar();server.verify();
    }
}
