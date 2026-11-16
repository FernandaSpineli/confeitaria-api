CREATE TABLE configuracao_custos (
    owner_id                UUID PRIMARY KEY REFERENCES usuario (id),
    salario_desejado_mes    NUMERIC(15, 2) NOT NULL CHECK (salario_desejado_mes >= 0),
    horas_trabalhadas_mes   NUMERIC(6, 2)  NOT NULL CHECK (horas_trabalhadas_mes > 0),
    bandeira_vigente        VARCHAR(20)    NOT NULL,
    gas_preco_botijao       NUMERIC(15, 2) CHECK (gas_preco_botijao > 0),
    gas_duracao_horas       NUMERIC(8, 2)  CHECK (gas_duracao_horas > 0),
    criterio_rateio         VARCHAR(10)    NOT NULL CHECK (criterio_rateio IN ('HORAS', 'VENDAS')),
    permitir_saldo_negativo BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_gas_completo CHECK ((gas_preco_botijao IS NULL) = (gas_duracao_horas IS NULL))
);

-- Histórico: a tarifa vigente é a de maior vigencia_inicio <= hoje.
CREATE TABLE tarifa_energia (
    id              UUID PRIMARY KEY,
    owner_id        UUID          NOT NULL REFERENCES usuario (id),
    tarifa_kwh      NUMERIC(9, 6) NOT NULL CHECK (tarifa_kwh > 0),
    vigencia_inicio DATE          NOT NULL,
    CONSTRAINT uk_tarifa_vigencia UNIQUE (owner_id, vigencia_inicio)
);

CREATE TABLE adicional_bandeira (
    id              UUID PRIMARY KEY,
    owner_id        UUID          NOT NULL REFERENCES usuario (id),
    bandeira        VARCHAR(20)   NOT NULL CHECK (bandeira IN ('VERDE', 'AMARELA', 'VERMELHA_1', 'VERMELHA_2')),
    adicional_kwh   NUMERIC(9, 6) NOT NULL CHECK (adicional_kwh >= 0),
    vigencia_inicio DATE          NOT NULL,
    CONSTRAINT uk_adicional_vigencia UNIQUE (owner_id, bandeira, vigencia_inicio)
);
