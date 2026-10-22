# Logging con SLF4J y Logback

Se aprovecha Logback, ya incluido por Spring Boot, mediante la interfaz SLF4J. No se agrega una dependencia nueva.

## Cambios

- `NotaVentaPdfController`: reemplaza las impresiones de consola y `printStackTrace` por logs. Captura excepciones de la operacion, en lugar de `Throwable`, que tambien ocultaba errores graves de la JVM.
- `AdminReporteController`: registra exportaciones PDF/Excel y errores que antes se devolvian sin contexto tecnico.
- `CajaPagoPedidoController` y `CajaPagoReservaController`: registran aprobaciones, rechazos, cobros presenciales y problemas de lectura de evidencias. Los resultados se registran despues de que el servicio retorna correctamente.
- `EvidenciaStorageService`: conserva en el log el error de entrada/salida al guardar una captura, manteniendo el mensaje sencillo que recibe el cliente.
- `application.properties`: habilita consola y archivo `logs/rinconcito.log`, rotacion diaria y por tamano de 10 MB, retencion de 14 periodos diarios y limite de 200 MB para archivos historicos. La limpieza tambien se ejecuta al iniciar. El archivo activo se cuenta aparte del limite de historicos.
- `.gitignore`: excluye los archivos generados de logging.

## Por que es util

Permite investigar un fallo con fecha, clase, nivel y excepcion, incluso despues de reiniciar la aplicacion. Los niveles distinguen operaciones completadas (`INFO`), acciones que no pudieron realizarse (`WARN`), fallos tecnicos (`ERROR`) y detalles de diagnostico (`DEBUG`).

Los mensajes de pagos usan identificadores internos; no incluyen nombres, DNI, contrasenas, numeros de operacion, capturas ni motivos escritos por clientes o personal. Las excepciones tecnicas pueden contener rutas y otros datos del entorno, por lo que los logs deben permanecer en el servidor con acceso restringido. Son registros de diagnostico, no sustituyen la informacion transaccional de pagos en MySQL.

## Como usarlo

El nivel normal es `INFO`. Para investigar un problema puede establecerse `APP_LOG_LEVEL=DEBUG` antes de iniciar la aplicacion. `APP_LOG_FILE` permite cambiar el destino del archivo. No hace falta editar Java para cambiar esas opciones.

Los archivos se crean al arrancar la aplicacion; el directorio de destino debe ser escribible. Las reglas de negocio, permisos y respuestas HTTP se mantienen.

## Validacion

Se aprobaron 16 pruebas: nueve de seguridad, seis de reportes y una de configuracion de logging. Esta ultima arranca un contexto sin MySQL, escribe un mensaje en un archivo temporal y comprueba el nivel predeterminado y la retencion configurada. No simula el paso de 14 dias ni llena el archivo para provocar una rotacion.

```shell
./mvnw -Dtest=LogbackConfiguracionTest,AdminReporteServiceTest,UsuarioServiceTest test
```

Referencia: [Logging en Spring Boot](https://docs.spring.io/spring-boot/reference/features/logging.html).
