package com.rinconcito.service.correo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OutlookOAuthTokenServiceTest {
    @TempDir Path directorio;

    Path autorizacion() throws Exception {
        Path archivo=directorio.resolve("outlook.properties");
        Files.writeString(archivo,"client_id=cliente-de-prueba\nusername=remitente@hotmail.com\nrefresh_token=refresh-prueba\n");
        return archivo;
    }

    @Test void renuevaUnaVezYReutilizaElTokenHastaQueCaduque() throws Exception {
        var builder=RestClient.builder(); var servidor=MockRestServiceServer.bindTo(builder).build();
        var archivo=autorizacion();
        servidor.expect(requestTo(OutlookOAuthTokenService.TOKEN_ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"access-prueba\",\"expires_in\":3600,\"refresh_token\":\"refresh-nuevo\"}",MediaType.APPLICATION_JSON));
        var tokens=new OutlookOAuthTokenService(archivo,"remitente@hotmail.com",builder.build());
        assertEquals("access-prueba",tokens.obtenerToken());
        assertEquals("access-prueba",tokens.obtenerToken());
        assertTrue(Files.readString(archivo).contains("refresh-nuevo"));
        assertFalse(Files.readString(archivo).contains("access-prueba"));
        servidor.verify();
    }

    @Test void unaAutenticacionRechazadaPermiteRenovarOtraVez() throws Exception {
        var builder=RestClient.builder(); var servidor=MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(OutlookOAuthTokenService.TOKEN_ENDPOINT))
                .andRespond(withSuccess("{\"access_token\":\"primero\",\"expires_in\":3600}",MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(OutlookOAuthTokenService.TOKEN_ENDPOINT))
                .andRespond(withSuccess("{\"access_token\":\"segundo\",\"expires_in\":3600}",MediaType.APPLICATION_JSON));
        var tokens=new OutlookOAuthTokenService(autorizacion(),"remitente@hotmail.com",builder.build());
        assertEquals("primero",tokens.obtenerToken()); tokens.invalidar();
        assertEquals("segundo",tokens.obtenerToken()); servidor.verify();
    }

    @Test void unErrorDeMicrosoftNoExponeSuRespuestaNiLaCredencial() throws Exception {
        var builder=RestClient.builder(); var servidor=MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(OutlookOAuthTokenService.TOKEN_ENDPOINT))
                .andRespond(withBadRequest().body("respuesta-sensible-que-no-se-debe-propagar"));
        var tokens=new OutlookOAuthTokenService(autorizacion(),"remitente@hotmail.com",builder.build());
        var error=assertThrows(IllegalStateException.class,tokens::obtenerToken);
        assertFalse(error.getMessage().contains("sensible")); assertNull(error.getCause()); servidor.verify();
    }

    @Test void noAceptaUnArchivoDeOtraCuentaNiUnaAutorizacionInexistente() throws Exception {
        assertThrows(IllegalStateException.class,()->new OutlookOAuthTokenService(
                autorizacion(),"otra@hotmail.com",RestClient.create()));
        assertThrows(IllegalStateException.class,()->new OutlookOAuthTokenService(
                directorio.resolve("no-existe"),"remitente@hotmail.com",RestClient.create()));
    }

    @Test void noAceptaRespuestasIncompletas() throws Exception {
        var builder=RestClient.builder(); var servidor=MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(OutlookOAuthTokenService.TOKEN_ENDPOINT))
                .andRespond(withSuccess("{\"expires_in\":3600}",MediaType.APPLICATION_JSON));
        var tokens=new OutlookOAuthTokenService(autorizacion(),"remitente@hotmail.com",builder.build());
        assertThrows(IllegalStateException.class,tokens::obtenerToken); servidor.verify();
    }
}
