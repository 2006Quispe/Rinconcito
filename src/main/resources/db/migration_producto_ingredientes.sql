-- Selecciona primero tu base de Rinconcito en MySQL Workbench.
-- Conserva todos los productos; los ingredientes existentes quedan vacíos.
SET @columna_ingredientes_existe = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'productos'
      AND column_name = 'ingredientes'
);
SET @sql_ingredientes = IF(
    @columna_ingredientes_existe = 0,
    'ALTER TABLE productos ADD COLUMN ingredientes VARCHAR(1000) NULL AFTER descripcion',
    'SELECT ''La columna ingredientes ya existe'' AS resultado'
);
PREPARE migracion_ingredientes FROM @sql_ingredientes;
EXECUTE migracion_ingredientes;
DEALLOCATE PREPARE migracion_ingredientes;
