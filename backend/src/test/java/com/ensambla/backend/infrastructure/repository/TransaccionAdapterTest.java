package com.ensambla.backend.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransaccionAdapterTest {

    private final PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
    private final TransaccionAdapter transaccionAdapter = new TransaccionAdapter(platformTransactionManager);

    private String resultado;
    private final AtomicBoolean corrio = new AtomicBoolean();
    private Throwable fallo;

    @Test
    @DisplayName("Ejecutar con resultado confirma la transacción y devuelve lo que produjo el bloque")
    void ejecutarConResultadoConfirmaLaTransaccionYDevuelveLoQueProdujoElBloque() {
        dadoUnaTransaccionAbierta();

        cuandoSeEjecutaUnTrabajoConResultado();

        entoncesSeDevuelveElResultadoYSeConfirma();
    }

    @Test
    @DisplayName("Ejecutar sin resultado corre el bloque y confirma la transacción")
    void ejecutarSinResultadoCorreElBloqueYConfirmaLaTransaccion() {
        dadoUnaTransaccionAbierta();

        cuandoSeEjecutaUnTrabajoSinResultado();

        entoncesCorrioElBloqueYSeConfirma();
    }

    @Test
    @DisplayName("Si el bloque falla, la transacción se revierte y el error se propaga")
    void siElBloqueFallaLaTransaccionSeRevierteYElErrorSePropaga() {
        dadoUnaTransaccionAbierta();

        cuandoSeEjecutaUnTrabajoQueFalla();

        entoncesSeRevierteSinConfirmarYElErrorSePropaga();
    }

    // --- dado ---

    private void dadoUnaTransaccionAbierta() {
        when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
    }

    // --- cuando ---

    private void cuandoSeEjecutaUnTrabajoConResultado() {
        resultado = transaccionAdapter.ejecutar(() -> "listo");
    }

    private void cuandoSeEjecutaUnTrabajoSinResultado() {
        transaccionAdapter.ejecutar(() -> corrio.set(true));
    }

    private void cuandoSeEjecutaUnTrabajoQueFalla() {
        fallo = catchThrowable(() -> transaccionAdapter.ejecutar((Runnable) () -> {
            throw new IllegalStateException("falla");
        }));
    }

    // --- entonces ---

    private void entoncesSeDevuelveElResultadoYSeConfirma() {
        assertThat(resultado).isEqualTo("listo");
        verify(platformTransactionManager).commit(transactionStatus);
    }

    private void entoncesCorrioElBloqueYSeConfirma() {
        assertThat(corrio).isTrue();
        verify(platformTransactionManager).commit(transactionStatus);
    }

    private void entoncesSeRevierteSinConfirmarYElErrorSePropaga() {
        assertThat(fallo).isInstanceOf(IllegalStateException.class).hasMessage("falla");
        verify(platformTransactionManager).rollback(transactionStatus);
        verify(platformTransactionManager, never()).commit(transactionStatus);
    }
}
