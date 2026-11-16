package com.confeitaria.estoque.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.confeitaria.estoque.domain.CalculadoraCapacidade.Capacidade;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculadoraCapacidadeTest {

    private final CalculadoraCapacidade calculadora = new CalculadoraCapacidade();

    private static Necessidade n(String nome, String porLote, String saldo) {
        return new Necessidade(TipoItem.INSUMO, UUID.nameUUIDFromBytes(nome.getBytes()), nome, "g",
                new BigDecimal(porLote), new BigDecimal(saldo));
    }

    @Test
    void limitadoPeloItemMaisEscassoEArredondadoParaBaixo() {
        Capacidade c = calculadora.calcular(List.of(n("Farinha", "300", "2000"), n("Chocolate", "200", "650"),
                n("Açúcar", "250", "5000")));
        assertThat(c.lotesPossiveis()).isEqualTo(3);
        assertThat(c.limitante().nome()).isEqualTo("Chocolate");
    }

    @Test
    void saldoMenorQueUmLoteDaZero() {
        assertThat(calculadora.calcular(List.of(n("Ovos", "4", "3"))).lotesPossiveis()).isZero();
    }

    @Test
    void saldoZeroOuNegativoDaZero() {
        assertThat(calculadora.calcular(List.of(n("A", "1", "0"))).lotesPossiveis()).isZero();
        assertThat(calculadora.calcular(List.of(n("A", "1", "-5"))).lotesPossiveis()).isZero();
    }

    @Test
    void itensRepetidosSaoSomadosAntesDeDividir() {
        Capacidade c = calculadora.calcular(List.of(n("Farinha", "300", "1000"), n("Farinha", "200", "1000")));
        assertThat(c.lotesPossiveis()).isEqualTo(2);
    }

    @Test
    void receitaSemItensNaoTemCapacidadeCalculavel() {
        assertThatThrownBy(() -> calculadora.calcular(List.of())).isInstanceOf(EstoqueException.class);
    }
}
