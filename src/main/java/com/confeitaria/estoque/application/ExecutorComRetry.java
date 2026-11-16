package com.confeitaria.estoque.application;

import com.confeitaria.shared.error.ConflitoException;
import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Executa a operação numa transação nova e, se outra requisição alterou o mesmo item no meio
 * do caminho (lock otimista via @Version), repete do zero relendo os saldos atualizados.
 * Esgotadas as tentativas, responde 409.
 */
@Component
public class ExecutorComRetry {

    static final int MAX_TENTATIVAS = 5;

    private static final Logger log = LoggerFactory.getLogger(ExecutorComRetry.class);

    private final TransactionTemplate transacao;

    public ExecutorComRetry(PlatformTransactionManager transactionManager) {
        this.transacao = new TransactionTemplate(transactionManager);
    }

    public <T> T executar(Supplier<T> operacao) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return transacao.execute(status -> operacao.get());
            } catch (OptimisticLockingFailureException | OptimisticLockException e) {
                if (tentativa >= MAX_TENTATIVAS) {
                    throw new ConflitoException(
                            "O estoque foi alterado por outra operação simultânea. Tente novamente.");
                }
                log.debug("Conflito de versão no estoque (tentativa {}), repetindo", tentativa);
                aguardar(tentativa);
            }
        }
    }

    private static void aguardar(int tentativa) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(5, 25L * tentativa));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflitoException("Operação interrompida.");
        }
    }
}
