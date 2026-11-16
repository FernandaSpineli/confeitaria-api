package com.confeitaria.custofixo.application;

import com.confeitaria.custofixo.api.CustoFixoDtos.CustoFixoRequest;
import com.confeitaria.custofixo.api.CustoFixoDtos.CustoFixoResponse;
import com.confeitaria.custofixo.persistence.CustoFixo;
import com.confeitaria.custofixo.persistence.CustoFixoRepository;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustoFixoService {

    private final CustoFixoRepository custos;

    public CustoFixoService(CustoFixoRepository custos) {
        this.custos = custos;
    }

    @Transactional
    public CustoFixoResponse criar(UUID ownerId, CustoFixoRequest r) {
        return CustoFixoResponse.of(custos.save(
                new CustoFixo(ownerId, r.descricao(), r.valorMensal(), !Boolean.FALSE.equals(r.ativo()))));
    }

    @Transactional(readOnly = true)
    public CustoFixoResponse buscar(UUID ownerId, UUID id) {
        return CustoFixoResponse.of(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<CustoFixoResponse> listar(UUID ownerId, Pageable pageable) {
        return custos.findByOwnerId(ownerId, pageable).map(CustoFixoResponse::of);
    }

    @Transactional
    public CustoFixoResponse atualizar(UUID ownerId, UUID id, CustoFixoRequest r) {
        CustoFixo c = obter(ownerId, id);
        c.atualizar(r.descricao(), r.valorMensal(), !Boolean.FALSE.equals(r.ativo()));
        return CustoFixoResponse.of(c);
    }

    @Transactional
    public void remover(UUID ownerId, UUID id) {
        custos.delete(obter(ownerId, id));
    }

    private CustoFixo obter(UUID ownerId, UUID id) {
        return custos.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new RecursoNaoEncontradoException("Custo fixo"));
    }
}
