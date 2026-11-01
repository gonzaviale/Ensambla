package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.port.TransaccionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Implementa la unidad de trabajo del dominio con la transacción de Spring (propagación REQUIRED). */
@Component
public class TransaccionAdapter implements TransaccionPort {

    private final TransactionTemplate transactionTemplate;

    public TransaccionAdapter(PlatformTransactionManager platformTransactionManager) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return transactionTemplate.execute(status -> trabajo.get());
    }

    @Override
    public void ejecutar(Runnable trabajo) {
        transactionTemplate.executeWithoutResult(status -> trabajo.run());
    }
}
