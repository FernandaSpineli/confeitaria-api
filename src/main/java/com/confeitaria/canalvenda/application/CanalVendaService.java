package com.confeitaria.canalvenda.application;

import com.confeitaria.canalvenda.api.CanalVendaDtos.CanalVendaRequest;
import com.confeitaria.canalvenda.api.CanalVendaDtos.CanalVendaResponse;
import com.confeitaria.canalvenda.persistence.CanalVenda;
import com.confeitaria.canalvenda.persistence.CanalVendaRepository;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanalVendaService {

    private final CanalVendaRepository canais;

    public CanalVendaService(CanalVendaRepository canais) {
        this.canais = canais;
    }

    @Transactional
    public CanalVendaResponse criar(UUID ownerId, CanalVendaRequest r) {
        return CanalVendaResponse.of(canais.save(
                new CanalVenda(ownerId, r.nome(), r.taxaPercentual(), !Boolean.FALSE.equals(r.ativo()))));
    }

    @Transactional(readOnly = true)
    public CanalVendaResponse buscar(UUID ownerId, UUID id) {
        return CanalVendaResponse.of(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<CanalVendaResponse> listar(UUID ownerId, Pageable pageable) {
        return canais.findByOwnerId(ownerId, pageable).map(CanalVendaResponse::of);
    }

    @Transactional
    public CanalVendaResponse atualizar(UUID ownerId, UUID id, CanalVendaRequest r) {
        CanalVenda c = obter(ownerId, id);
        c.atualizar(r.nome(), r.taxaPercentual(), !Boolean.FALSE.equals(r.ativo()));
        return CanalVendaResponse.of(c);
    }

    @Transactional
    public void remover(UUID ownerId, UUID id) {
        canais.delete(obter(ownerId, id));
    }

    public CanalVenda obter(UUID ownerId, UUID id) {
        return canais.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Canal de venda"));
    }
}
