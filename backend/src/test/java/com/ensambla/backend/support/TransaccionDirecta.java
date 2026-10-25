package com.ensambla.backend.support;

import com.ensambla.backend.domain.port.TransaccionPort;

import java.util.function.Supplier;

/** Unidad de trabajo para tests unitarios: corre el bloque en el acto, sin base de datos. */
public class TransaccionDirecta implements TransaccionPort {

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return trabajo.get();
    }

    @Override
    public void ejecutar(Runnable trabajo) {
        trabajo.run();
    }
}
