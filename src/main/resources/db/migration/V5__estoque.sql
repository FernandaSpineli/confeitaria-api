CREATE TABLE producao (
    id              UUID PRIMARY KEY,
    owner_id        UUID           NOT NULL REFERENCES usuario (id),
    receita_id      UUID           NOT NULL REFERENCES receita (id),
    lotes           INTEGER        NOT NULL CHECK (lotes > 0),
    custo_materiais NUMERIC(15, 2) NOT NULL,
    usuario_id      UUID           NOT NULL REFERENCES usuario (id),
    criado_em       TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_producao_owner ON producao (owner_id, criado_em DESC);

CREATE TABLE movimentacao_estoque (
    id             UUID PRIMARY KEY,
    owner_id       UUID            NOT NULL REFERENCES usuario (id),
    tipo           VARCHAR(10)     NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE')),
    motivo         VARCHAR(12)     NOT NULL
        CHECK (motivo IN ('COMPRA', 'PRODUCAO', 'PERDA', 'VENCIMENTO', 'INVENTARIO', 'OUTRO')),
    insumo_id      UUID REFERENCES insumo (id),
    embalagem_id   UUID REFERENCES embalagem (id),
    quantidade     NUMERIC(19, 6)  NOT NULL CHECK (quantidade <> 0),
    saldo_apos     NUMERIC(19, 6)  NOT NULL,
    custo_unitario NUMERIC(20, 10) NOT NULL,
    valor_total    NUMERIC(15, 2),
    observacao     VARCHAR(255),
    producao_id    UUID REFERENCES producao (id),
    usuario_id     UUID            NOT NULL REFERENCES usuario (id),
    criado_em      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT ck_mov_um_item CHECK ((insumo_id IS NULL) <> (embalagem_id IS NULL)),
    CONSTRAINT ck_mov_sinal CHECK (
        (tipo = 'ENTRADA' AND quantidade > 0) OR (tipo = 'SAIDA' AND quantidade < 0) OR tipo = 'AJUSTE')
);
CREATE INDEX idx_mov_owner_data ON movimentacao_estoque (owner_id, criado_em DESC);
CREATE INDEX idx_mov_insumo ON movimentacao_estoque (insumo_id) WHERE insumo_id IS NOT NULL;
CREATE INDEX idx_mov_embalagem ON movimentacao_estoque (embalagem_id) WHERE embalagem_id IS NOT NULL;
CREATE INDEX idx_mov_producao ON movimentacao_estoque (producao_id) WHERE producao_id IS NOT NULL;

-- Histórico imutável: nem a aplicação nem um SQL manual podem alterar ou apagar movimentações.
CREATE FUNCTION bloquear_alteracao_movimentacao() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'movimentacao_estoque é imutável: % não permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movimentacao_imutavel
    BEFORE UPDATE OR DELETE ON movimentacao_estoque
    FOR EACH ROW EXECUTE FUNCTION bloquear_alteracao_movimentacao();
