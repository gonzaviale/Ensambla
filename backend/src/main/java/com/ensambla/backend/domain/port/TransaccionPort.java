package com.ensambla.backend.domain.port;

import java.util.function.Supplier;

/**
 * Unidad de trabajo atómica: lo que corre dentro de {@code ejecutar} se confirma junto o se revierte junto. Si ya hay
 * una transacción abierta, el bloque se suma a ella. Reemplaza a {@code @Transactional} para que el dominio no dependa
 * de Spring.
 */
public interface TransaccionPort {

    <T> T ejecutar(Supplier<T> trabajo);

    void ejecutar(Runnable trabajo);
}
