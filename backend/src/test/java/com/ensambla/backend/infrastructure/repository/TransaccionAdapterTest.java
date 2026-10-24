package com.ensambla.backend.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransaccionAdapterTest {

    private final PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);

    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);

    private final TransaccionAdapter transaccionAdapter = new TransaccionAdapter(platformTransactionManager);

    @Test
    @DisplayName("Ejecutar con resultado confirma la transacción y devuelve lo que produjo el bloque")
    void ejecutarShouldCommitAndReturnResultWhenWorkSucceeds() {
        givenTransaccionAbierta();

        String resultado = transaccionAdapter.ejecutar(() -> "listo");

        assertThat(resultado).isEqualTo("listo");
        verify(platformTransactionManager).commit(transactionStatus);
    }

    @Test
    @DisplayName("Ejecutar sin resultado corre el bloque y confirma la transacción")
    void ejecutarShouldCommitWhenWorkWithoutResultSucceeds() {
        givenTransaccionAbierta();
        AtomicBoolean corrio = new AtomicBoolean();

        transaccionAdapter.ejecutar(() -> corrio.set(true));

        assertThat(corrio).isTrue();
        verify(platformTransactionManager).commit(transactionStatus);
    }

    @Test
    @DisplayName("Si el bloque falla, la transacción se revierte y el error se propaga")
    void ejecutarShouldRollbackWhenWorkFails() {
        givenTransaccionAbierta();

        assertThatThrownBy(() -> transaccionAdapter.ejecutar((Runnable) () -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class).hasMessage("falla");

        verify(platformTransactionManager).rollback(transactionStatus);
        verify(platformTransactionManager, never()).commit(transactionStatus);
    }

    // --- arrange ---
    private void givenTransaccionAbierta() {
        when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
    }
}
