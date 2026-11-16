package com.confeitaria.receita.application;

import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.embalagem.persistence.EmbalagemRepository;
import com.confeitaria.equipamento.persistence.Equipamento;
import com.confeitaria.equipamento.persistence.EquipamentoRepository;
import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.insumo.persistence.InsumoRepository;
import com.confeitaria.receita.api.ReceitaDtos.ItemEmbalagemResponse;
import com.confeitaria.receita.api.ReceitaDtos.ItemEquipamentoResponse;
import com.confeitaria.receita.api.ReceitaDtos.ItemInsumoRequest;
import com.confeitaria.receita.api.ReceitaDtos.ItemInsumoResponse;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaRequest;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaResponse;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaResumoResponse;
import com.confeitaria.receita.persistence.Receita;
import com.confeitaria.receita.persistence.ReceitaEmbalagem;
import com.confeitaria.receita.persistence.ReceitaEquipamento;
import com.confeitaria.receita.persistence.ReceitaInsumo;
import com.confeitaria.receita.persistence.ReceitaRepository;
import com.confeitaria.shared.domain.BaseQuantidade;
import com.confeitaria.shared.domain.UnidadeMedida;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import com.confeitaria.shared.error.RegraNegocioException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceitaService {

    private final ReceitaRepository receitas;
    private final InsumoRepository insumos;
    private final EmbalagemRepository embalagens;
    private final EquipamentoRepository equipamentos;
    private final Clock clock;

    public ReceitaService(ReceitaRepository receitas, InsumoRepository insumos, EmbalagemRepository embalagens,
            EquipamentoRepository equipamentos, Clock clock) {
        this.receitas = receitas;
        this.insumos = insumos;
        this.embalagens = embalagens;
        this.equipamentos = equipamentos;
        this.clock = clock;
    }

    @Transactional
    public ReceitaResponse criar(UUID ownerId, ReceitaRequest r) {
        Receita receita = new Receita(ownerId, clock.instant());
        preencher(receita, ownerId, r);
        return toResponse(receitas.save(receita));
    }

    @Transactional
    public ReceitaResponse atualizar(UUID ownerId, UUID id, ReceitaRequest r) {
        Receita receita = obter(ownerId, id);
        preencher(receita, ownerId, r);
        return toResponse(receita);
    }

    @Transactional(readOnly = true)
    public ReceitaResponse buscar(UUID ownerId, UUID id) {
        return toResponse(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<ReceitaResumoResponse> listar(UUID ownerId, String nome, Pageable pageable) {
        return receitas.findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(ownerId, nome == null ? "" : nome, pageable)
                .map(rc -> new ReceitaResumoResponse(rc.getId(), rc.getNome(), rc.getRendimento(), rc.getMargemLucro(),
                        rc.isAtivo()));
    }

    @Transactional
    public void desativar(UUID ownerId, UUID id) {
        obter(ownerId, id).desativar();
    }

    private Receita obter(UUID ownerId, UUID id) {
        return receitas.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new RecursoNaoEncontradoException("Receita"));
    }

    /** Todo item referenciado precisa pertencer à mesma usuária; caso contrário, 404. */
    private void preencher(Receita receita, UUID ownerId, ReceitaRequest r) {
        receita.atualizarDados(r.nome(), r.rendimento(), r.tempoPreparoMin(),
                r.minutosGas() == null ? 0 : r.minutosGas(), r.margemLucro(), r.vendasEstimadasMes());

        List<ReceitaInsumo> itensInsumo = r.insumosOuVazio().stream().map(i -> itemInsumo(ownerId, i)).toList();
        List<ReceitaEmbalagem> itensEmbalagem = r.embalagensOuVazio().stream().map(e -> {
            Embalagem embalagem = embalagens.findByIdAndOwnerId(e.embalagemId(), ownerId)
                    .filter(Embalagem::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Embalagem " + e.embalagemId()));
            return new ReceitaEmbalagem(embalagem, e.quantidade(), baseOuPadrao(e.base()));
        }).toList();
        List<ReceitaEquipamento> itensEquipamento = r.equipamentosOuVazio().stream().map(e -> {
            Equipamento equipamento = equipamentos.findByIdAndOwnerId(e.equipamentoId(), ownerId)
                    .filter(Equipamento::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento " + e.equipamentoId()));
            return new ReceitaEquipamento(equipamento, e.minutosUso());
        }).toList();

        receita.substituirItens(itensInsumo, itensEmbalagem, itensEquipamento);
    }

    private ReceitaInsumo itemInsumo(UUID ownerId, ItemInsumoRequest i) {
        Insumo insumo = insumos.findByIdAndOwnerId(i.insumoId(), ownerId)
                .filter(Insumo::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Insumo " + i.insumoId()));
        UnidadeMedida unidade = i.unidade() == null ? UnidadeMedida.valueOf(insumo.unidadeBase().name()) : i.unidade();
        if (!unidade.compativelCom(insumo.unidadeBase())) {
            throw new RegraNegocioException("Unidade " + unidade + " incompatível com o insumo '" + insumo.getNome()
                    + "' (unidade base " + insumo.unidadeBase() + ").");
        }
        return new ReceitaInsumo(insumo, unidade.paraBase(i.quantidade()), baseOuPadrao(i.base()));
    }

    private static BaseQuantidade baseOuPadrao(BaseQuantidade base) {
        return base == null ? BaseQuantidade.POR_LOTE : base;
    }

    private static ReceitaResponse toResponse(Receita r) {
        int rend = r.getRendimento();
        return new ReceitaResponse(r.getId(), r.getNome(), rend, r.getTempoPreparoMin(), r.getMinutosGas(),
                r.getMargemLucro(), r.getVendasEstimadasMes(), r.isAtivo(),
                r.getInsumos().stream().map(i -> new ItemInsumoResponse(i.getInsumo().getId(), i.getInsumo().getNome(),
                        i.getQuantidade(), i.getInsumo().unidadeBase(), i.getBase(),
                        i.getBase().quantidadePorLote(i.getQuantidade(), rend))).toList(),
                r.getEmbalagens().stream().map(e -> new ItemEmbalagemResponse(e.getEmbalagem().getId(),
                        e.getEmbalagem().getNome(), e.getQuantidade(), e.getBase(),
                        e.getBase().quantidadePorLote(e.getQuantidade(), rend))).toList(),
                r.getEquipamentos().stream().map(e -> new ItemEquipamentoResponse(e.getEquipamento().getId(),
                        e.getEquipamento().getNome(), e.getEquipamento().getPotenciaWatts(), e.getMinutosUso())).toList());
    }
}
