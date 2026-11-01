package com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerImpactoAvisosTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final ObtenerImpactoAvisos obtenerImpactoAvisos =
            new ObtenerImpactoAvisos(negocioRepository, avisoRepository);

    @Test
    @DisplayName("Cuenta los avisos aplicados y descartados y suma el cambio de ingreso de los medidos")
    void executeShouldSummarizeTheAvisosOfTheBusiness() {
        Aviso mejoro = givenAplicado("30.00");
        Aviso empeoro = givenAplicado("15.00");
        Aviso sinMedir = givenAplicado(null);
        Aviso descartado = givenAviso();
        descartado.descartar(AHORA);
        Aviso pendiente = givenAviso();
        givenAvisos(empeoro, sinMedir, descartado, pendiente, mejoro);

        ImpactoAvisos impacto = obtenerImpactoAvisos.execute(USUARIO_ID);

        assertThat(impacto.aplicados()).isEqualTo(3);
        assertThat(impacto.descartados()).isEqualTo(1);
        assertThat(impacto.ingresoSemanalDelta()).isEqualByComparingTo("35.00");
        assertThat(impacto.medidos()).extracting(ImpactoDeAviso::avisoId)
                .containsExactly(mejoro.getId(), empeoro.getId());
        assertThat(impacto.medidos().get(0).titulo()).isEqualTo("Mermelada cara");
        assertThat(impacto.medidos().get(0).tipo()).isEqualTo(TipoAviso.ESTANCADO);
        assertThat(impacto.medidos().get(0).aplicadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Sin avisos el resumen es cero")
    void executeShouldReturnZeroWithoutAvisos() {
        givenAvisos();

        ImpactoAvisos impacto = obtenerImpactoAvisos.execute(USUARIO_ID);

        assertThat(impacto.aplicados()).isZero();
        assertThat(impacto.descartados()).isZero();
        assertThat(impacto.ingresoSemanalDelta()).isEqualByComparingTo("0");
        assertThat(impacto.medidos()).isEmpty();
    }

    private void givenAvisos(Aviso... avisos) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of(avisos));
    }

    /** Un aviso aplicado; con {@code ingresoPorDiaDespues} ya medido, contra 20 por día antes del aviso. */
    private static Aviso givenAplicado(String ingresoPorDiaDespues) {
        Aviso aviso = givenAviso();
        aviso.aplicar(AHORA);
        if (ingresoPorDiaDespues != null) {
            aviso.registrarImpacto(new ImpactoAviso(14, BigDecimal.ONE, BigDecimal.ONE,
                    new BigDecimal("20.00"), new BigDecimal(ingresoPorDiaDespues)), AHORA);
        }
        return aviso;
    }

    private static Aviso givenAviso() {
        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.ESTANCADO,
                new ResultadoRegla(AvisoTestData.PROYECTO_ID, ResultadoRegla.entidadDeRegistro(RegistroId.nuevo()),
                        (short) 2, "Mermelada cara", "Detalle", Map.of("precio", 100), List.of(), List.of()), AHORA);
        aviso.setId(AvisoId.nuevo());
        return aviso;
    }
}
