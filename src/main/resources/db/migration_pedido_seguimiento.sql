ALTER TABLE pedidos
    ADD COLUMN avance_entrega VARCHAR(24) NULL,
    ADD COLUMN token_seguimiento VARCHAR(36) NULL,
    ADD UNIQUE KEY uq_pedidos_token_seguimiento (token_seguimiento);
