package com.confeitaria.precificacao.domain;

import static com.confeitaria.shared.domain.Decimais.dividir;
import static com.confeitaria.shared.domain.Decimais.fracaoParaPercentual;
import static com.confeitaria.shared.domain.Decimais.minutosParaHoras;
import static com.confeitaria.shared.domain.Decimais.monetario;

import com.confeitaria.precificacao.domain.MargemInviavelException.CanalInviavel;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.CanalTaxa;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.ItemCusto;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.RateioCustosFixos;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.Tamanho;
import com.confeitaria.precificacao.domain.ParametrosPrecificacao.UsoEquipamento;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.ComposicaoCusto;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.Componente;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.LinhaCusto;
import com.confeitaria.precificacao.domain.ResultadoPrecificacao.PrecoCalculado;
import com.confeitaria.shared.domain.Decimais;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculadora de preço de venda. Java puro: sem Spring, sem JPA.
 *
 * <pre>
 * custo do lote  = insumos + embalagens + mão de obra + energia + gás + custos fixos rateados
 * custo unitário = custo do lote / rendimento
 * preço          = custo unitário / (1 − margem − taxa do canal)      (margem sobre o preço)
 * </pre>
 *
 * Com um tamanho (ex.: meia receita que rende 1 torta pequena), o custo da receita é multiplicado pela
 * fração usada, a embalagem do tamanho entra por peça e o custo unitário é dividido pelas peças do tamanho.
 *
 * Todo o cálculo usa escala 10 e HALF_EVEN; só a saída é arredondada para 2 casas.
 */
public class CalculadoraPreco {

    public ResultadoPrecificacao calcular(ParametrosPrecificacao p) {
        validarEntradas(p);
        validarMargem(p);

        Tamanho tamanho = p.tamanho();
        BigDecimal fator = tamanho == null ? BigDecimal.ONE : tamanho.fracaoReceita();
        int pecas = tamanho == null ? p.rendimento() : tamanho.rendimento();

        List<LinhaCusto> linhas = new ArrayList<>();
        BigDecimal insumos = somarItens(p.insumos(), Componente.INSUMO, p.rendimento(), fator, linhas);
        BigDecimal embalagens = somarItens(p.embalagens(), Componente.EMBALAGEM, p.rendimento(), fator, linhas)
                .add(embalagemDoTamanho(tamanho, linhas));
        BigDecimal maoDeObra = maoDeObra(p, fator, linhas);
        BigDecimal energia = energia(p, fator, linhas);
        BigDecimal gas = gas(p, fator, linhas);
        BigDecimal fixos = custosFixos(p, fator, pecas, linhas);

        BigDecimal custoLote = insumos.add(embalagens).add(maoDeObra).add(energia).add(gas).add(fixos);
        BigDecimal custoUnitario = dividir(custoLote, BigDecimal.valueOf(pecas));

        ComposicaoCusto composicao = new ComposicaoCusto(monetario(insumos), monetario(embalagens),
                monetario(maoDeObra), monetario(energia), monetario(gas), monetario(fixos), monetario(custoLote));

        PrecoCalculado base = preco(custoUnitario, p.margemLucro(), new CanalTaxa(null, "Sem taxa", BigDecimal.ZERO));
        List<PrecoCalculado> porCanal = p.canais().stream()
                .map(c -> preco(custoUnitario, p.margemLucro(), c))
                .toList();

        return new ResultadoPrecificacao(composicao, List.copyOf(linhas), pecas, monetario(custoUnitario),
                fracaoParaPercentual(p.margemLucro()), base, porCanal);
    }

    private static void validarEntradas(ParametrosPrecificacao p) {
        if (p.rendimento() <= 0) {
            throw new PrecificacaoException("O rendimento da receita deve ser maior que zero.");
        }
        if (p.tamanho() != null
                && (p.tamanho().fracaoReceita().signum() <= 0 || p.tamanho().rendimento() <= 0)) {
            throw new PrecificacaoException("O tamanho '" + p.tamanho().nome()
                    + "' precisa usar uma parte da receita e render pelo menos uma peça.");
        }
        if (p.margemLucro().signum() < 0) {
            throw new PrecificacaoException("A margem de lucro não pode ser negativa.");
        }
        if (p.maoDeObra().horasMes().signum() <= 0) {
            throw new PrecificacaoException("Configure as horas trabalhadas no mês (maior que zero).");
        }
        if (!p.equipamentos().isEmpty() && p.energia() == null) {
            throw new PrecificacaoException(
                    "A receita usa equipamentos elétricos, mas não há tarifa de energia (ou adicional da bandeira) vigente.");
        }
        if (p.minutosGas() > 0 && p.gas() == null) {
            throw new PrecificacaoException(
                    "A receita usa forno a gás, mas o preço e a duração do botijão não foram configurados.");
        }
        p.canais().forEach(c -> {
            if (c.taxa().signum() < 0) {
                throw new PrecificacaoException("A taxa do canal '" + c.nome() + "' não pode ser negativa.");
            }
        });
    }

    private static void validarMargem(ParametrosPrecificacao p) {
        List<CanalInviavel> invalidos = new ArrayList<>();
        if (p.margemLucro().compareTo(BigDecimal.ONE) >= 0) {
            invalidos.add(inviavel("Sem taxa", p.margemLucro(), BigDecimal.ZERO));
        }
        for (CanalTaxa canal : p.canais()) {
            if (p.margemLucro().add(canal.taxa()).compareTo(BigDecimal.ONE) >= 0) {
                invalidos.add(inviavel(canal.nome(), p.margemLucro(), canal.taxa()));
            }
        }
        if (!invalidos.isEmpty()) {
            throw new MargemInviavelException(invalidos);
        }
    }

    private static CanalInviavel inviavel(String canal, BigDecimal margem, BigDecimal taxa) {
        return new CanalInviavel(canal, fracaoParaPercentual(margem), fracaoParaPercentual(taxa),
                fracaoParaPercentual(margem.add(taxa)));
    }

    private static BigDecimal somarItens(List<ItemCusto> itens, Componente componente, int rendimento,
            BigDecimal fator, List<LinhaCusto> linhas) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCusto item : itens) {
            BigDecimal quantidade = item.base().quantidadePorLote(item.quantidade(), rendimento).multiply(fator);
            BigDecimal custo = quantidade.multiply(item.custoUnitario());
            linhas.add(new LinhaCusto(componente, item.nome(), Decimais.quantidade(quantidade), item.unidade(),
                    item.custoUnitario().setScale(6, Decimais.ARREDONDAMENTO), monetario(custo)));
            total = total.add(custo);
        }
        return total;
    }

    /** Embalagem própria do tamanho: quantidade por peça × peças. */
    private static BigDecimal embalagemDoTamanho(Tamanho tamanho, List<LinhaCusto> linhas) {
        if (tamanho == null || tamanho.embalagem() == null) {
            return BigDecimal.ZERO;
        }
        ItemCusto e = tamanho.embalagem();
        BigDecimal quantidade = e.quantidade().multiply(BigDecimal.valueOf(tamanho.rendimento()));
        BigDecimal custo = quantidade.multiply(e.custoUnitario());
        linhas.add(new LinhaCusto(Componente.EMBALAGEM, e.nome(), Decimais.quantidade(quantidade), e.unidade(),
                e.custoUnitario().setScale(6, Decimais.ARREDONDAMENTO), monetario(custo)));
        return custo;
    }

    private static BigDecimal maoDeObra(ParametrosPrecificacao p, BigDecimal fator, List<LinhaCusto> linhas) {
        BigDecimal valorHora = dividir(p.maoDeObra().salarioMensal(), p.maoDeObra().horasMes());
        BigDecimal horas = minutosParaHoras(p.tempoPreparoMin()).multiply(fator);
        BigDecimal custo = horas.multiply(valorHora);
        linhas.add(new LinhaCusto(Componente.MAO_DE_OBRA, "Tempo de preparo", Decimais.quantidade(horas), "h",
                monetario(valorHora), monetario(custo)));
        return custo;
    }

    /** (potência em kW × horas de uso) × (tarifa do kWh + adicional da bandeira). */
    private static BigDecimal energia(ParametrosPrecificacao p, BigDecimal fator, List<LinhaCusto> linhas) {
        if (p.equipamentos().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal precoKwh = p.energia().tarifaKwh().add(p.energia().adicionalBandeiraKwh());
        BigDecimal total = BigDecimal.ZERO;
        for (UsoEquipamento uso : p.equipamentos()) {
            BigDecimal kwh = dividir(uso.potenciaWatts(), Decimais.MIL).multiply(minutosParaHoras(uso.minutos()))
                    .multiply(fator);
            BigDecimal custo = kwh.multiply(precoKwh);
            linhas.add(new LinhaCusto(Componente.ENERGIA, uso.nome(), Decimais.quantidade(kwh), "kWh",
                    precoKwh.setScale(6, Decimais.ARREDONDAMENTO), monetario(custo)));
            total = total.add(custo);
        }
        return total;
    }

    private static BigDecimal gas(ParametrosPrecificacao p, BigDecimal fator, List<LinhaCusto> linhas) {
        if (p.minutosGas() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal custoHora = dividir(p.gas().precoBotijao(), p.gas().duracaoHoras());
        BigDecimal horas = minutosParaHoras(p.minutosGas()).multiply(fator);
        BigDecimal custo = horas.multiply(custoHora);
        linhas.add(new LinhaCusto(Componente.GAS, "Forno a gás", Decimais.quantidade(horas), "h",
                monetario(custoHora), monetario(custo)));
        return custo;
    }

    /** Por HORAS: horas de trabalho usadas (× fração). Por VENDAS: cada peça vendida leva uma parte. */
    private static BigDecimal custosFixos(ParametrosPrecificacao p, BigDecimal fator, int pecas,
            List<LinhaCusto> linhas) {
        RateioCustosFixos rateio = p.rateio();
        if (rateio.totalMensal().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal quantidade;
        BigDecimal custoPorUnidadeDeRateio;
        String unidade;
        if (rateio.criterio() == RateioCustosFixos.Criterio.HORAS) {
            if (rateio.horasMes().signum() <= 0) {
                throw new PrecificacaoException("Configure as horas trabalhadas no mês para ratear os custos fixos.");
            }
            quantidade = minutosParaHoras(p.tempoPreparoMin()).multiply(fator);
            custoPorUnidadeDeRateio = dividir(rateio.totalMensal(), rateio.horasMes());
            unidade = "h";
        } else {
            if (rateio.vendasEstimadasMes() <= 0) {
                throw new PrecificacaoException(
                        "Rateio por vendas exige informar as vendas estimadas no mês em pelo menos uma receita ativa.");
            }
            quantidade = BigDecimal.valueOf(pecas);
            custoPorUnidadeDeRateio = dividir(rateio.totalMensal(), BigDecimal.valueOf(rateio.vendasEstimadasMes()));
            unidade = "un";
        }
        BigDecimal custo = quantidade.multiply(custoPorUnidadeDeRateio);
        linhas.add(new LinhaCusto(Componente.CUSTOS_FIXOS, "Rateio por " + rateio.criterio().name().toLowerCase(),
                Decimais.quantidade(quantidade), unidade, custoPorUnidadeDeRateio.setScale(6, Decimais.ARREDONDAMENTO),
                monetario(custo)));
        return custo;
    }

    private static PrecoCalculado preco(BigDecimal custoUnitario, BigDecimal margem, CanalTaxa canal) {
        BigDecimal divisor = BigDecimal.ONE.subtract(margem).subtract(canal.taxa());
        BigDecimal preco = monetario(dividir(custoUnitario, divisor));
        BigDecimal valorTaxa = preco.multiply(canal.taxa());
        BigDecimal lucro = preco.subtract(custoUnitario).subtract(valorTaxa);
        return new PrecoCalculado(canal.id(), canal.nome(), fracaoParaPercentual(canal.taxa()), preco,
                monetario(valorTaxa), monetario(lucro));
    }
}
