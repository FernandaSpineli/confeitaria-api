package com.confeitaria.equipamento.application;

import com.confeitaria.equipamento.api.EquipamentoDtos.EquipamentoRequest;
import com.confeitaria.equipamento.api.EquipamentoDtos.EquipamentoResponse;
import com.confeitaria.equipamento.persistence.Equipamento;
import com.confeitaria.equipamento.persistence.EquipamentoRepository;
import com.confeitaria.shared.error.ConflitoException;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentos;

    public EquipamentoService(EquipamentoRepository equipamentos) {
        this.equipamentos = equipamentos;
    }

    @Transactional
    public EquipamentoResponse criar(UUID ownerId, EquipamentoRequest r) {
        return EquipamentoResponse.of(equipamentos.save(new Equipamento(ownerId, r.nome(), r.potenciaWatts())));
    }

    @Transactional(readOnly = true)
    public EquipamentoResponse buscar(UUID ownerId, UUID id) {
        return EquipamentoResponse.of(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<EquipamentoResponse> listar(UUID ownerId, Pageable pageable) {
        return equipamentos.findByOwnerIdAndAtivoTrue(ownerId, pageable).map(EquipamentoResponse::of);
    }

    @Transactional
    public EquipamentoResponse atualizar(UUID ownerId, UUID id, EquipamentoRequest r) {
        Equipamento e = obter(ownerId, id);
        e.atualizar(r.nome(), r.potenciaWatts());
        return EquipamentoResponse.of(e);
    }

    @Transactional
    public void desativar(UUID ownerId, UUID id) {
        Equipamento e = obter(ownerId, id);
        if (equipamentos.usadoEmReceitaAtiva(id)) {
            throw new ConflitoException("Equipamento usado em receita ativa. Remova-o das receitas antes de excluir.");
        }
        e.desativar();
    }

    private Equipamento obter(UUID ownerId, UUID id) {
        return equipamentos.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento"));
    }
}
