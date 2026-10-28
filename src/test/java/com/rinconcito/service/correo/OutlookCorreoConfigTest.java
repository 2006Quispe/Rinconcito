package com.rinconcito.service.correo;

import com.rinconcito.config.OutlookCorreoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OutlookCorreoConfigTest {
    @Test void utilizaOAuthConTlsYNoUnaContrasenaNormal() throws Exception {
        var tokens=mock(OutlookOAuthTokenService.class);
        var sender=(JavaMailSenderImpl)new OutlookCorreoConfig().outlookMailSender(
                tokens,"remitente@hotmail.com","remitente@hotmail.com");
        assertEquals("smtp-mail.outlook.com",sender.getHost());
        assertEquals(587,sender.getPort());
        assertNull(sender.getPassword());
        assertEquals("XOAUTH2",sender.getJavaMailProperties().getProperty("mail.smtp.auth.mechanisms"));
        assertEquals("true",sender.getJavaMailProperties().getProperty("mail.smtp.starttls.required"));
        assertEquals("true",sender.getJavaMailProperties().getProperty("mail.smtp.ssl.checkserveridentity"));
        assertEquals("false",sender.getJavaMailProperties().getProperty("mail.debug"));
    }

    @Test void noPermiteUnRemitenteDistintoDeLaCuentaAutorizada() {
        assertThrows(IllegalStateException.class,()->new OutlookCorreoConfig().outlookMailSender(
                mock(OutlookOAuthTokenService.class),"remitente@hotmail.com","otra@hotmail.com"));
    }
}
