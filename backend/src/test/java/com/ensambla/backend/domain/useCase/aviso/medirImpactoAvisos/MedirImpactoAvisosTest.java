package com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MedirImpactoAvisosTest {

    private static final NegocioId NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    private static final RegistroId PRODUCTO = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId OTRO_PRODUCTO = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime APLICADO_HACE_MAS_DE_UNA_VENTANA = OffsetDateTime.parse("2026-10-01T15:00:00Z");
    private static final OffsetDateTime APLICADO_HACE_POCO = OffsetDateTime.parse("2026-10-10T15:00:00Z");
    private static final int VENTANA = 14;

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);

    private MedirImpactoAvisos medirImpactoAvisos;

    @BeforeEach
    void setUp() {
        medirImpactoAvisos = new MedirImpactoAvisos(negocioRepository, proyectoRepository, registroRepository,
                avisoRepository, new MetricasVentas(30, new BigDecimal("0.5")), new TransaccionDirecta(),
                Clock.fixed(INSTANTE, ZoneOffset.UTC), VENTANA);
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID));
    }

    @Test
    @DisplayName("Mide el aviso aplicado y guarda el impacto de las ventas de su producto")
    void executeShouldMeasureAndSaveTheImpactOfAnAppliedAviso() {
        Aviso aviso = givenAvisoAplicado(PRODUCTO, APLICADO_HACE_MAS_DE_UNA_VENTANA);
        givenAvisosAplicados(aviso);
        givenVentas(
                AvisoTestData.venta(PRODUCTO.toString(), 2, 100, "2026-09-20", null),
                AvisoTestData.venta(PRODUCTO.toString(), 7, 100, "2026-10-02", null),
                AvisoTestData.venta(OTRO_PRODUCTO.toString(), 50, 100, "2026-10-02", null));

        medirImpactoAvisos.execute();

        Aviso guardado = whenGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.APLICADO);
        assertThat(guardado.impacto()).isPresent();
        assertThat(guardado.impacto().orElseThrow().unidadesPorDiaDespues()).isEqualByComparingTo("0.50");
        assertThat(guardado.impacto().orElseThrow().deltaIngresoSemanal()).isEqualByComparingTo("249.97");
    }

    @Test
    @DisplayName("Las filas de ejemplo de la plantilla no cuentan como ventas")
    void executeShouldIgnoreExampleRows() {
        givenAvisosAplicados(givenAvisoAplicado(PRODUCTO, APLICADO_HACE_MAS_DE_UNA_VENTANA));
        Registro ejemplo = AvisoTestData.venta(PRODUCTO.toString(), 70, 100, "2026-10-02", null);
        ejemplo.setEsEjemplo(true);
        givenVentas(ejemplo);

        medirImpactoAvisos.execute();

        assertThat(whenGuardado().impacto().orElseThrow().deltaIngresoSemanal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Una tabla que no es de ventas, o un campo sin clave, no cambian la medición")
    void executeShouldOnlyReadVentasTablesAndCamposWithClave() {
        givenAvisosAplicados(givenAvisoAplicado(PRODUCTO, APLICADO_HACE_MAS_DE_UNA_VENTANA));
        List<Campo> campos = new ArrayList<>(AvisoTestData.bloqueVentas().getCampos());
        campos.add(AvisoTestData.campo(java.util.UUID.randomUUID(), null));
        Bloque ventas = Bloque.builder().id(AvisoTestData.VENTAS_ID).negocioId(NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.VENTAS).campos(campos).build();
        givenProyecto(AvisoTestData.bloqueInventario(), ventas);
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.VENTAS_ID, NEGOCIO_ID))
                .thenReturn(List.of(AvisoTestData.venta(PRODUCTO.toString(), 7, 100, "2026-10-02", null)));

        medirImpactoAvisos.execute();

        assertThat(whenGuardado().impacto().orElseThrow().unidadesPorDiaDespues()).isEqualByComparingTo("0.50");
        verify(registroRepository, never()).findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un proyecto que ya no existe no aporta ventas")
    void executeShouldSkipProjectsThatNoLongerExist() {
        givenAvisosAplicados(givenAvisoAplicado(PRODUCTO, APLICADO_HACE_MAS_DE_UNA_VENTANA));
        Proyecto resumen = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(resumen));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());

        medirImpactoAvisos.execute();

        assertThat(whenGuardado().impacto().orElseThrow().deltaIngresoSemanal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("No mide un aviso cuya ventana posterior todavía no terminó")
    void executeShouldWaitWhenTheWindowHasNotEnded() {
        givenAvisosAplicados(givenAvisoAplicado(PRODUCTO, APLICADO_HACE_POCO));
        givenVentas();

        medirImpactoAvisos.execute();

        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("Sin avisos medibles no carga las ventas del negocio")
    void executeShouldNotLoadSalesWhenNothingIsMeasurable() {
        Aviso yaMedido = givenAvisoAplicado(PRODUCTO, APLICADO_HACE_MAS_DE_UNA_VENTANA);
        yaMedido.setTipo(TipoAviso.COMBO);
        givenAvisosAplicados(yaMedido);

        medirImpactoAvisos.execute();

        verifyNoInteractions(proyectoRepository, registroRepository);
        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("Un negocio con error no frena la medición de los demás")
    void executeShouldKeepGoingWhenABusinessFails() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.APLICADO))
                .thenThrow(new IllegalStateException("falló"));
        when(avisoRepository.findByNegocioIdAndEstado(OTRO_NEGOCIO_ID, EstadoAviso.APLICADO)).thenReturn(List.of());

        assertThatCode(() -> medirImpactoAvisos.execute()).doesNotThrowAnyException();

        verify(avisoRepository).findByNegocioIdAndEstado(OTRO_NEGOCIO_ID, EstadoAviso.APLICADO);
    }

    private void givenAvisosAplicados(Aviso... avisos) {
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.APLICADO)).thenReturn(List.of(avisos));
    }

    private void givenVentas(Registro... filas) {
        givenProyecto(AvisoTestData.bloqueVentas());
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.VENTAS_ID, NEGOCIO_ID))
                .thenReturn(List.of(filas));
    }

    private void givenProyecto(Bloque... bloques) {
        Proyecto resumen = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        Proyecto completo = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID)
                .bloques(List.of(bloques)).build();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(resumen));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(completo));
    }

    private Aviso whenGuardado() {
        ArgumentCaptor<Aviso> guardado = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository, times(1)).save(guardado.capture());
        return guardado.getValue();
    }

    private static Aviso givenAvisoAplicado(RegistroId producto, OffsetDateTime aplicadoEn) {
        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.ESTANCADO,
                new ResultadoRegla(AvisoTestData.PROYECTO_ID, ResultadoRegla.entidadDeRegistro(producto), (short) 2,
                        "Mermelada cara", "Detalle", Map.of("precio", 100), List.of(), List.of()), aplicadoEn);
        aviso.aplicar(aplicadoEn);
        return aviso;
    }
}
