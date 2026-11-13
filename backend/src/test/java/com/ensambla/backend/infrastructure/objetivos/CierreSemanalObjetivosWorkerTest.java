package com.ensambla.backend.infrastructure.objetivos;

import com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos.CerrarSemanasObjetivos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CierreSemanalObjetivosWorkerTest {

    private final CerrarSemanasObjetivos cerrarSemanasObjetivos = mock(CerrarSemanasObjetivos.class);

    private final CierreSemanalObjetivosWorker worker = new CierreSemanalObjetivosWorker(cerrarSemanasObjetivos);

    @Test
    @DisplayName("El domingo corre el cierre semanal")
    void cerrarShouldRunTheWeeklyClose() {
        worker.cerrar();

        verify(cerrarSemanasObjetivos).execute();
    }

    @Test
    @DisplayName("Al arrancar corre el cierre para recuperar las semanas pendientes")
    void recuperarShouldRunTheWeeklyClose() {
        worker.recuperar();

        verify(cerrarSemanasObjetivos).execute();
    }
}
