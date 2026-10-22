package com.rinconcito.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LogbackConfiguracionTest {

    @TempDir
    Path directorio;

    @Test
    void escribeEnArchivoConLaConfiguracionDeLaAplicacion() throws Exception {
        Path archivo = directorio.resolve("rinconcito.log");

        // Solo arrancamos un contexto vacio: esta comprobacion no necesita MySQL.
        try (ConfigurableApplicationContext contexto =
                     new SpringApplicationBuilder(ContextoDePrueba.class)
                             .web(WebApplicationType.NONE)
                             .run("--logging.file.name=" + archivo,
                                     "--spring.main.banner-mode=off")) {
            Logger log = LoggerFactory.getLogger("com.rinconcito.prueba.Logging");
            log.info("Comprobacion de escritura del log");

            assertTrue(Files.readString(archivo).contains("Comprobacion de escritura del log"));
            assertTrue(log.isInfoEnabled());
            assertFalse(log.isDebugEnabled());

            LoggerContext logback = (LoggerContext) LoggerFactory.getILoggerFactory();
            var appender = assertInstanceOf(RollingFileAppender.class,
                    logback.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE"));
            var politica = assertInstanceOf(TimeBasedRollingPolicy.class, appender.getRollingPolicy());
            assertEquals(14, politica.getMaxHistory());
        } finally {
            // En Windows hay que cerrar el archivo antes de limpiar el directorio temporal.
            LoggerContext logback = (LoggerContext) LoggerFactory.getILoggerFactory();
            var root = logback.getLogger(Logger.ROOT_LOGGER_NAME);
            var archivoLog = root.getAppender("FILE");
            if (archivoLog != null) {
                archivoLog.stop();
                root.detachAppender(archivoLog);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ContextoDePrueba {
    }
}
