package com.confeitaria.insumo.application;

import com.confeitaria.insumo.api.InsumoDtos.InsumoRequest;
import com.confeitaria.insumo.api.InsumoDtos.InsumoResponse;
import com.confeitaria.insumo.persistence.Insumo;
import com.confeitaria.insumo.persistence.InsumoRepository;
import com.confeitaria.shared.error.ConflitoException;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import com.confeitaria.shared.error.RegraNegocioException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsumoService {

    private final InsumoRepository insumos;
    private final Clock clock;

    public InsumoService(InsumoRepository insumos, Clock clock) {
        this.insumos = insumos;
        this.clock = clock;
    }

    @Transactional
    public InsumoResponse criar(UUID ownerId, InsumoRequest r) {
        Insumo insumo = new Insumo(ownerId, r.nome(), r.unidade().base(), r.unidade().paraBase(r.quantidadeEmbalagem()),
                r.precoEmbalagem(), r.unidade().paraBase(r.estoqueMinimo()), clock.instant());
        return InsumoResponse.of(insumos.save(insumo));
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscar(UUID ownerId, UUID id) {
        return InsumoResponse.of(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<InsumoResponse> listar(UUID ownerId, String nome, Pageable pageable) {
        return insumos.findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(ownerId, nome == null ? "" : nome, pageable)
                .map(InsumoResponse::of);
    }

    /**
     * Correção manual de cadastro/custo. Como o preço das receitas é calculado sob demanda,
     * o novo custo se reflete imediatamente em todas as receitas que usam o insumo.
     */
    @Transactional
    public InsumoResponse atualizar(UUID ownerId, UUID id, InsumoRequest r) {
        Insumo insumo = obter(ownerId, id);
        if (!r.unidade().compativelCom(insumo.unidadeBase())) {
            throw new RegraNegocioException("Unidade " + r.unidade() + " incompatível com a unidade base "
                    + insumo.unidadeBase() + " do insumo. Para mudar a unidade, cadastre um novo insumo.");
        }
        insumo.atualizarCadastro(r.nome(), r.unidade().paraBase(r.quantidadeEmbalagem()), r.precoEmbalagem(),
                r.unidade().paraBase(r.estoqueMinimo()));
        return InsumoResponse.of(insumo);
    }

    @Transactional
    public void desativar(UUID ownerId, UUID id) {
        Insumo insumo = obter(ownerId, id);
        if (insumos.usadoEmReceitaAtiva(id)) {
            throw new ConflitoException("Insumo usado em receita ativa. Remova-o das receitas antes de excluir.");
        }
        insumo.desativar();
    }

    private Insumo obter(UUID ownerId, UUID id) {
        return insumos.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new RecursoNaoEncontradoException("Insumo"));
    }
}
