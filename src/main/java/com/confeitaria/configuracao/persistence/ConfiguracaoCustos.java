package com.confeitaria.configuracao.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Parâmetros de custo de cada usuária (1:1 com a usuária). */
@Entity
@Table(name = "configuracao_custos")
public class ConfiguracaoCustos {

    @Id
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "salario_desejado_mes", nullable = false)
    private BigDecimal salarioDesejadoMes;

    @Column(name = "horas_trabalhadas_mes", nullable = false)
    private BigDecimal horasTrabalhadasMes;

    @Enumerated(EnumType.STRING)
    @Column(name = "bandeira_vigente", nullable = false)
    private Bandeira bandeiraVigente;

    @Column(name = "gas_preco_botijao")
    private BigDecimal gasPrecoBotijao;

    @Column(name = "gas_duracao_horas")
    private BigDecimal gasDuracaoHoras;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_rateio", nullable = false)
    private CriterioRateio criterioRateio;

    @Column(name = "permitir_saldo_negativo", nullable = false)
    private boolean permitirSaldoNegativo;

    protected ConfiguracaoCustos() {
    }

    public static ConfiguracaoCustos padrao(UUID ownerId) {
        ConfiguracaoCustos c = new ConfiguracaoCustos();
        c.ownerId = ownerId;
        c.salarioDesejadoMes = BigDecimal.ZERO;
        c.horasTrabalhadasMes = BigDecimal.valueOf(160);
        c.bandeiraVigente = Bandeira.VERDE;
        c.criterioRateio = CriterioRateio.HORAS;
        c.permitirSaldoNegativo = false;
        return c;
    }

    public void atualizar(BigDecimal salario, BigDecimal horas, Bandeira bandeira, BigDecimal gasPreco,
            BigDecimal gasDuracao, CriterioRateio criterio, boolean permitirNegativo) {
        this.salarioDesejadoMes = salario;
        this.horasTrabalhadasMes = horas;
        this.bandeiraVigente = bandeira;
        this.gasPrecoBotijao = gasPreco;
        this.gasDuracaoHoras = gasDuracao;
        this.criterioRateio = criterio;
        this.permitirSaldoNegativo = permitirNegativo;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public BigDecimal getSalarioDesejadoMes() {
        return salarioDesejadoMes;
    }

    public BigDecimal getHorasTrabalhadasMes() {
        return horasTrabalhadasMes;
    }

    public Bandeira getBandeiraVigente() {
        return bandeiraVigente;
    }

    public BigDecimal getGasPrecoBotijao() {
        return gasPrecoBotijao;
    }

    public BigDecimal getGasDuracaoHoras() {
        return gasDuracaoHoras;
    }

    public CriterioRateio getCriterioRateio() {
        return criterioRateio;
    }

    public boolean isPermitirSaldoNegativo() {
        return permitirSaldoNegativo;
    }
}
