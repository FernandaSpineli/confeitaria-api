-- custo_unitario com 10 casas: custos por grama/ml são pequenos (ex.: R$ 0,0056093750/g).
CREATE TABLE insumo (
    id                     UUID PRIMARY KEY,
    owner_id               UUID           NOT NULL REFERENCES usuario (id),
    nome                   VARCHAR(120)   NOT NULL,
    unidade_base           VARCHAR(5)     NOT NULL CHECK (unidade_base IN ('G', 'ML', 'UN')),
    custo_unitario         NUMERIC(20, 10) NOT NULL CHECK (custo_unitario >= 0),
    ultima_qtd_embalagem   NUMERIC(19, 6) NOT NULL,
    ultimo_preco_embalagem NUMERIC(15, 2) NOT NULL,
    saldo                  NUMERIC(19, 6) NOT NULL DEFAULT 0,
    estoque_minimo         NUMERIC(19, 6) NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
    ativo                  BOOLEAN        NOT NULL DEFAULT TRUE,
    version                BIGINT         NOT NULL DEFAULT 0,
    criado_em              TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_insumo_owner ON insumo (owner_id, nome);

CREATE TABLE embalagem (
    id             UUID PRIMARY KEY,
    owner_id       UUID            NOT NULL REFERENCES usuario (id),
    nome           VARCHAR(120)    NOT NULL,
    custo_unitario NUMERIC(20, 10) NOT NULL CHECK (custo_unitario >= 0),
    saldo          NUMERIC(19, 6)  NOT NULL DEFAULT 0,
    estoque_minimo NUMERIC(19, 6)  NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
    ativo          BOOLEAN         NOT NULL DEFAULT TRUE,
    version        BIGINT          NOT NULL DEFAULT 0,
    criado_em      TIMESTAMPTZ     NOT NULL
);
CREATE INDEX idx_embalagem_owner ON embalagem (owner_id, nome);

CREATE TABLE equipamento (
    id             UUID PRIMARY KEY,
    owner_id       UUID          NOT NULL REFERENCES usuario (id),
    nome           VARCHAR(120)  NOT NULL,
    potencia_watts NUMERIC(8, 2) NOT NULL CHECK (potencia_watts > 0),
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_equipamento_owner ON equipamento (owner_id);

CREATE TABLE custo_fixo (
    id           UUID PRIMARY KEY,
    owner_id     UUID           NOT NULL REFERENCES usuario (id),
    descricao    VARCHAR(120)   NOT NULL,
    valor_mensal NUMERIC(15, 2) NOT NULL CHECK (valor_mensal >= 0),
    ativo        BOOLEAN        NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_custo_fixo_owner ON custo_fixo (owner_id);

CREATE TABLE canal_venda (
    id              UUID PRIMARY KEY,
    owner_id        UUID          NOT NULL REFERENCES usuario (id),
    nome            VARCHAR(120)  NOT NULL,
    taxa_percentual NUMERIC(5, 2) NOT NULL CHECK (taxa_percentual >= 0 AND taxa_percentual < 100),
    ativo           BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_canal_venda_owner ON canal_venda (owner_id);
