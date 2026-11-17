-- Tamanhos de uma receita: cada um usa uma fração da receita e rende N peças, com embalagem própria por peça.
-- Ex.: torta holandesa grande (1 receita, 1 peça), pequena (0,5 receita, 1 peça), pote (1 receita, 8 peças).
-- Sem UNIQUE (receita_id, nome): os tamanhos são substituídos a cada edição e o Hibernate insere antes de
-- remover; a unicidade do nome é validada na aplicação.
CREATE TABLE receita_tamanho (
    id                  UUID PRIMARY KEY,
    receita_id          UUID           NOT NULL REFERENCES receita (id) ON DELETE CASCADE,
    nome                VARCHAR(60)    NOT NULL,
    fracao_receita      NUMERIC(9, 4)  NOT NULL CHECK (fracao_receita > 0 AND fracao_receita <= 100),
    rendimento          INTEGER        NOT NULL CHECK (rendimento > 0),
    embalagem_id        UUID REFERENCES embalagem (id),
    embalagens_por_peca NUMERIC(19, 6) CHECK (embalagens_por_peca > 0),
    posicao             INTEGER        NOT NULL,
    CONSTRAINT ck_tamanho_embalagem CHECK ((embalagem_id IS NULL) = (embalagens_por_peca IS NULL))
);
CREATE INDEX idx_receita_tamanho_receita ON receita_tamanho (receita_id);
CREATE INDEX idx_receita_tamanho_embalagem ON receita_tamanho (embalagem_id) WHERE embalagem_id IS NOT NULL;

-- A produção guarda o nome do tamanho e as peças produzidas no momento do registro: o histórico não muda
-- se a receita for editada depois.
ALTER TABLE producao ADD COLUMN tamanho VARCHAR(60);
ALTER TABLE producao ADD COLUMN unidades_produzidas INTEGER;
UPDATE producao p SET unidades_produzidas = p.lotes * r.rendimento FROM receita r WHERE r.id = p.receita_id;
ALTER TABLE producao ALTER COLUMN unidades_produzidas SET NOT NULL;
ALTER TABLE producao ADD CONSTRAINT ck_producao_unidades CHECK (unidades_produzidas > 0);
