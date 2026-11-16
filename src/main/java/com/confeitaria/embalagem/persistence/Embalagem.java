package com.confeitaria.embalagem.persistence;

import com.confeitaria.shared.domain.UnidadeBase;
import com.confeitaria.shared.persistence.ItemEstoque;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Caixa, fita, etiqueta, forminha... Sempre contada em unidades. */
@Entity
@Table(name = "embalagem")
public class Embalagem extends ItemEstoque {

    protected Embalagem() {
    }

    public Embalagem(UUID ownerId, String nome, BigDecimal custoUnitario, BigDecimal estoqueMinimo, Instant criadoEm) {
        super(ownerId, nome, custoUnitario, estoqueMinimo, criadoEm);
    }

    public void atualizarCadastro(String nome, BigDecimal custoUnitario, BigDecimal estoqueMinimo) {
        atualizarCadastroBase(nome, custoUnitario, estoqueMinimo);
    }

    @Override
    public UnidadeBase unidadeBase() {
        return UnidadeBase.UN;
    }
}
