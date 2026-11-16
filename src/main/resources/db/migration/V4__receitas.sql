CREATE TABLE receita (
    id                   UUID PRIMARY KEY,
    owner_id             UUID          NOT NULL REFERENCES usuario (id),
    nome                 VARCHAR(120)  NOT NULL,
    rendimento           INTEGER       NOT NULL CHECK (rendimento > 0),
    tempo_preparo_min    INTEGER       NOT NULL CHECK (tempo_preparo_min >= 0),
    minutos_gas          INTEGER       NOT NULL DEFAULT 0 CHECK (minutos_gas >= 0),
    margem_lucro         NUMERIC(5, 2) NOT NULL CHECK (margem_lucro >= 0 AND margem_lucro < 100),
    vendas_estimadas_mes INTEGER CHECK (vendas_estimadas_mes >= 0),
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    version              BIGINT        NOT NULL DEFAULT 0,
    criado_em            TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_receita_owner ON receita (owner_id, nome);

-- base: POR_LOTE (quantidade para a receita inteira) ou POR_UNIDADE (multiplicada pelo rendimento).
CREATE TABLE receita_insumo (
    id         UUID PRIMARY KEY,
    receita_id UUID           NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    insumo_id  UUID           NOT NULL REFERENCES insumo (id),
    quantidade NUMERIC(19, 6) NOT NULL CHECK (quantidade > 0),
    base       VARCHAR(12)    NOT NULL CHECK (base IN ('POR_LOTE', 'POR_UNIDADE'))
);
CREATE INDEX idx_receita_insumo_receita ON receita_insumo (receita_id);
CREATE INDEX idx_receita_insumo_insumo ON receita_insumo (insumo_id);

CREATE TABLE receita_embalagem (
    id           UUID PRIMARY KEY,
    receita_id   UUID           NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    embalagem_id UUID           NOT NULL REFERENCES embalagem (id),
    quantidade   NUMERIC(19, 6) NOT NULL CHECK (quantidade > 0),
    base         VARCHAR(12)    NOT NULL CHECK (base IN ('POR_LOTE', 'POR_UNIDADE'))
);
CREATE INDEX idx_receita_embalagem_receita ON receita_embalagem (receita_id);
CREATE INDEX idx_receita_embalagem_embalagem ON receita_embalagem (embalagem_id);

CREATE TABLE receita_equipamento (
    id             UUID PRIMARY KEY,
    receita_id     UUID    NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    equipamento_id UUID    NOT NULL REFERENCES equipamento (id),
    minutos_uso    INTEGER NOT NULL CHECK (minutos_uso > 0)
);
CREATE INDEX idx_receita_equipamento_receita ON receita_equipamento (receita_id);
CREATE INDEX idx_receita_equipamento_equipamento ON receita_equipamento (equipamento_id);
