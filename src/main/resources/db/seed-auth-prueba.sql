-- Seed opcional de autenticación (NO mostrar credenciales en la UI).
-- Ejecutar manualmente en MySQL (rinconcito_db) si hace falta probar redirecciones.
-- Contraseña para todos: Prueba123!
-- Hash BCrypt generado con Spring Security crypto 7.1.1:
-- $2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm

INSERT INTO roles (nombre) VALUES ('CLIENTE')
  ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
INSERT INTO roles (nombre) VALUES ('ADMINISTRADOR')
  ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
INSERT INTO roles (nombre) VALUES ('MESERO')
  ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
INSERT INTO roles (nombre) VALUES ('CAJA')
  ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO usuarios (id_rol, nombres, apellidos, dni, telefono, correo, contrasena, tipo_registro, estado, fecha_registro)
SELECT r.id_rol, 'Admin', 'Prueba', '10000001', '999000001', 'admin@rinconcito.pe',
       '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm',
       'CUENTA', 'ACTIVO', NOW()
FROM roles r WHERE r.nombre = 'ADMINISTRADOR'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.correo = 'admin@rinconcito.pe');

INSERT INTO usuarios (id_rol, nombres, apellidos, dni, telefono, correo, contrasena, tipo_registro, estado, fecha_registro)
SELECT r.id_rol, 'Mesero', 'Prueba', '10000002', '999000002', 'mesero@rinconcito.pe',
       '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm',
       'CUENTA', 'ACTIVO', NOW()
FROM roles r WHERE r.nombre = 'MESERO'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.correo = 'mesero@rinconcito.pe');

INSERT INTO usuarios (id_rol, nombres, apellidos, dni, telefono, correo, contrasena, tipo_registro, estado, fecha_registro)
SELECT r.id_rol, 'Caja', 'Prueba', '10000003', '999000003', 'caja@rinconcito.pe',
       '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm',
       'CUENTA', 'ACTIVO', NOW()
FROM roles r WHERE r.nombre = 'CAJA'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.correo = 'caja@rinconcito.pe');

INSERT INTO usuarios (id_rol, nombres, apellidos, dni, telefono, correo, contrasena, tipo_registro, estado, fecha_registro)
SELECT r.id_rol, 'Cliente', 'Prueba', '10000004', '999000004', 'cliente@rinconcito.pe',
       '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm',
       'CUENTA', 'ACTIVO', NOW()
FROM roles r WHERE r.nombre = 'CLIENTE'
AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.correo = 'cliente@rinconcito.pe');
