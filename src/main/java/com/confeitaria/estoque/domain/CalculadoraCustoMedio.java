package com.confeitaria.estoque.domain;

import com.confeitaria.shared.domain.Decimais;
import java.math.BigDecimal;

/**
 * Custo médio ponderado após uma compra. Java puro.
 * <pre>
 * novo custo = (saldo × custo atual + valor pago) / (saldo + quantidade comprada)
 * </pre>
 * Saldo zero ou negativo não tem valor a ponderar: o novo custo passa a ser o da compra.
 */
public class CalculadoraCustoMedio {

    public BigDecimal calcular(BigDecimal saldoAtual, BigDecimal custoAtual, BigDecimal quantidadeComprada,
            BigDecimal valorPago) {
        if (quantidadeComprada.signum() <= 0) {
            throw new EstoqueException("A quantidade comprada deve ser maior que zero.");
        }
        if (valorPago.signum() < 0) {
            throw new EstoqueException("O valor pago não pode ser negativo.");
        }
        if (saldoAtual.signum() <= 0) {
            return Decimais.dividir(valorPago, quantidadeComprada);
        }
        BigDecimal valorEmEstoque = saldoAtual.multiply(custoAtual);
        return Decimais.dividir(valorEmEstoque.add(valorPago), saldoAtual.add(quantidadeComprada));
    }
}
