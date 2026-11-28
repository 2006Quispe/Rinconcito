-- Ejecutar en la base de datos que usa Rinconcito.
ALTER TABLE usuarios
    MODIFY COLUMN correo VARCHAR(250) NULL;

ALTER TABLE configuracion_restaurante
    MODIFY COLUMN correo VARCHAR(250) NULL;
