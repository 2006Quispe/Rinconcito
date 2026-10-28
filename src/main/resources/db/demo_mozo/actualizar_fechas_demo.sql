-- Solo para la base de demostracion. Usar si tomas capturas otro dia.
USE rinconcito_demo_mozo;
SET SESSION time_zone = '-05:00';
SET @ahora = NOW(), @hoy = CURDATE();
SET @inicio = GREATEST(TIME('00:00:00'), SUBTIME(CURTIME(), '00:15:00')),
    @fin = LEAST(TIME('23:59:59'), ADDTIME(CURTIME(), '01:45:00'));

UPDATE reservas
SET fecha_reserva = @hoy, hora_inicio = @inicio, hora_fin = @fin
WHERE codigo_reserva LIKE 'RES-DEMO-%'
  AND estado IN ('CONFIRMADA', 'CLIENTE_LLEGO', 'EN_ATENCION');

UPDATE eventos
SET fecha = @hoy, hora_inicio = @inicio, hora_fin = @fin
WHERE id_evento = 1 AND titulo = 'Almuerzo familiar';

-- No cambia estados, pagos ni los pedidos que terminaste durante la prueba.
