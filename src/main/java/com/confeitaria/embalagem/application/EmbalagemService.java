package com.confeitaria.embalagem.application;

import com.confeitaria.embalagem.api.EmbalagemDtos.EmbalagemRequest;
import com.confeitaria.embalagem.api.EmbalagemDtos.EmbalagemResponse;
import com.confeitaria.embalagem.persistence.Embalagem;
import com.confeitaria.embalagem.persistence.EmbalagemRepository;
import com.confeitaria.shared.error.ConflitoException;
import com.confeitaria.shared.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbalagemService {

    private final EmbalagemRepository embalagens;
    private final Clock clock;

    public EmbalagemService(EmbalagemRepository embalagens, Clock clock) {
        this.embalagens = embalagens;
        this.clock = clock;
    }

    @Transactional
    public EmbalagemResponse criar(UUID ownerId, EmbalagemRequest r) {
        return EmbalagemResponse.of(embalagens.save(
                new Embalagem(ownerId, r.nome(), r.custoUnitario(), r.estoqueMinimo(), clock.instant())));
    }

    @Transactional(readOnly = true)
    public EmbalagemResponse buscar(UUID ownerId, UUID id) {
        return EmbalagemResponse.of(obter(ownerId, id));
    }

    @Transactional(readOnly = true)
    public Page<EmbalagemResponse> listar(UUID ownerId, String nome, Pageable pageable) {
        return embalagens.findByOwnerIdAndAtivoTrueAndNomeContainingIgnoreCase(ownerId, nome == null ? "" : nome,
                pageable).map(EmbalagemResponse::of);
    }

    @Transactional
    public EmbalagemResponse atualizar(UUID ownerId, UUID id, EmbalagemRequest r) {
        Embalagem embalagem = obter(ownerId, id);
        embalagem.atualizarCadastro(r.nome(), r.custoUnitario(), r.estoqueMinimo());
        return EmbalagemResponse.of(embalagem);
    }

    @Transactional
    public void desativar(UUID ownerId, UUID id) {
        Embalagem embalagem = obter(ownerId, id);
        if (embalagens.usadaEmReceitaAtiva(id)) {
            throw new ConflitoException("Embalagem usada em receita ativa. Remova-a das receitas antes de excluir.");
        }
        embalagem.desativar();
    }

    private Embalagem obter(UUID ownerId, UUID id) {
        return embalagens.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Embalagem"));
    }
}
