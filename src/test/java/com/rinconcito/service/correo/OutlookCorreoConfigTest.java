package com.rinconcito.service.correo;

import com.rinconcito.config.OutlookCorreoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OutlookCorreoConfigTest {
    @Test void utilizaGraphSinContrasenaSmtp() throws Exception {
        var tokens=mock(OutlookOAuthTokenService.class);
        var sender=new OutlookCorreoConfig().outlookMailSender(
                tokens,"remitente@hotmail.com","remitente@hotmail.com");
        assertInstanceOf(OutlookGraphMailSender.class,sender);
        assertNull(((JavaMailSenderImpl)sender).getPassword());
    }

    @Test void noPermiteUnRemitenteDistintoDeLaCuentaAutorizada() {
        assertThrows(IllegalStateException.class,()->new OutlookCorreoConfig().outlookMailSender(
                mock(OutlookOAuthTokenService.class),"remitente@hotmail.com","otra@hotmail.com"));
    }
}
