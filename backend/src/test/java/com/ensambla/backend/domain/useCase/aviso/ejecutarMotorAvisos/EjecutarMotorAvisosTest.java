package com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecutarMotorAvisosTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-19T11:00:00Z");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private ReglaAviso regla;

    private EjecutarMotorAvisos ejecutarMotorAvisos;

    private final ContextoNegocio contexto = new ContextoNegocio(NEGOCIO_ID, List.of());

    @BeforeEach
    void setUp() {
        ejecutarMotorAvisos = motor(List.of(regla));
        lenient().when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID));
        lenient().when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        lenient().when(regla.tipo()).thenReturn(TipoAviso.STOCK);
        lenient().when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocacion -> {
            Aviso aviso = invocacion.getArgument(0);
            if (aviso.getId() == null) {
                aviso.setId(UUID.randomUUID());
            }
            return aviso;
        });
    }

    @Test
    @DisplayName("Crea el aviso cuando la regla detecta algo nuevo")
    void executeShouldCreateAvisoWhenRuleDetectsSomethingNew() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        givenRegla(resultado);
        givenSinAvisos();

        ejecutarMotorAvisos.execute();

        Aviso creado = whenGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getEntidad()).isEqualTo("registro:1");
        assertThat(creado.getHuella()).isEqualTo(resultado.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("No duplica un aviso pendiente que la regla vuelve a detectar con los mismos datos")
    void executeShouldNotDuplicatePendingAviso() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso pendiente = givenAvisoDe(resultado, EstadoAviso.PENDIENTE);
        givenRegla(resultado);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, pendiente.getHuella()))
                .thenReturn(Optional.of(pendiente));

        ejecutarMotorAvisos.execute();

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        verify(avisoRepository).save(any(Aviso.class));
    }

    @Test
    @DisplayName("No vuelve a crear un aviso descartado mientras sus datos de origen no cambien")
    void executeShouldNotRecreateDiscardedAvisoWithSameHuella() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso descartado = givenAvisoDe(resultado, EstadoAviso.DESCARTADO);
        givenRegla(resultado);
        givenSinPendientes();
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, descartado.getHuella()))
                .thenReturn(Optional.of(descartado));

        ejecutarMotorAvisos.execute();

        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("No vuelve a crear un aviso aplicado con los mismos datos")
    void executeShouldNotRecreateAppliedAviso() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso aplicado = givenAvisoDe(resultado, EstadoAviso.APLICADO);
        givenRegla(resultado);
        givenSinPendientes();
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, aplicado.getHuella()))
                .thenReturn(Optional.of(aplicado));

        ejecutarMotorAvisos.execute();

        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("Un aviso descartado reaparece cuando cambian los datos que lo originaron")
    void executeShouldCreateNewAvisoWhenDiscardedOneHasDifferentDatos() {
        ResultadoRegla antes = givenResultado("registro:1", 2, 4);
        givenAvisoDe(antes, EstadoAviso.DESCARTADO);
        ResultadoRegla ahora = givenResultado("registro:1", 3, 0);
        givenRegla(ahora);
        givenSinAvisos();

        ejecutarMotorAvisos.execute();

        Aviso creado = whenGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getHuella()).isEqualTo(ahora.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("Si cambian los datos de un aviso pendiente, lo actualiza en lugar de crear otro")
    void executeShouldUpdatePendingAvisoOfSameEntidadWhenDatosChange() {
        Aviso pendiente = givenAvisoDe(givenResultado("registro:1", 2, 4), EstadoAviso.PENDIENTE);
        ResultadoRegla ahora = givenResultado("registro:1", 3, 0);
        givenRegla(ahora);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, ahora.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.empty());

        ejecutarMotorAvisos.execute();

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getPrioridad()).isEqualTo((short) 3);
        assertThat(guardado.getHuella()).isEqualTo(ahora.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("Reactiva un aviso vencido cuando la regla vuelve a detectarlo y vence el pendiente de la entidad")
    void executeShouldReactivateExpiredAvisoAndExpireOtherPendingOfSameEntidad() {
        ResultadoRegla original = givenResultado("registro:1", 2, 4);
        Aviso vencido = givenAvisoDe(original, EstadoAviso.VENCIDO);
        Aviso otroPendiente = givenAvisoDe(givenResultado("registro:1", 2, 3), EstadoAviso.PENDIENTE);
        givenRegla(original);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(otroPendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, original.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.of(vencido));

        ejecutarMotorAvisos.execute();

        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Aviso::getId, Aviso::getEstado)
                .containsExactly(
                        tuple(otroPendiente.getId(), EstadoAviso.VENCIDO),
                        tuple(vencido.getId(), EstadoAviso.PENDIENTE));
    }

    @Test
    @DisplayName("Vence el aviso pendiente de una entidad que la regla ya no detecta")
    void executeShouldExpirePendingAvisoWhenRuleNoLongerDetectsIt() {
        Aviso pendiente = givenAvisoDe(givenResultado("registro:1", 2), EstadoAviso.PENDIENTE);
        givenRegla();
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(new ArrayList<>(List.of(pendiente)));

        ejecutarMotorAvisos.execute();

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    @Test
    @DisplayName("Vence el pendiente de una entidad cuyo aviso ya fue descartado con los datos actuales")
    void executeShouldExpireStalePendingWhenDiscardedAvisoMatchesCurrentDatos() {
        ResultadoRegla actual = givenResultado("registro:1", 2, 4);
        Aviso descartado = givenAvisoDe(actual, EstadoAviso.DESCARTADO);
        Aviso viejo = givenAvisoDe(givenResultado("registro:1", 2, 9), EstadoAviso.PENDIENTE);
        givenRegla(actual);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(viejo));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, descartado.getHuella()))
                .thenReturn(Optional.of(descartado));

        ejecutarMotorAvisos.execute();

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(viejo.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    @Test
    @DisplayName("Ejecuta todas las reglas registradas")
    void executeShouldRunEveryRegisteredRule() {
        ReglaAviso otra = mock(ReglaAviso.class);
        when(otra.tipo()).thenReturn(TipoAviso.PRECIO_ZONA);
        when(otra.evaluar(contexto)).thenReturn(List.of());
        ejecutarMotorAvisos = motor(List.of(regla, otra));
        givenRegla();
        givenSinAvisos();

        ejecutarMotorAvisos.execute();

        verify(otra).evaluar(contexto);
        verify(avisoRepository).findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.PRECIO_ZONA,
                EstadoAviso.PENDIENTE);
    }

    @Test
    @DisplayName("Corre las reglas sobre cada negocio con el instante del reloj")
    void executeShouldRunRulesForEveryNegocio() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(proyectoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenReturn(List.of());
        givenSinPendientes();
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(OTRO_NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of());
        when(regla.evaluar(any())).thenReturn(List.of());

        ejecutarMotorAvisos.execute();

        verify(regla).evaluar(contexto);
        verify(regla).evaluar(new ContextoNegocio(OTRO_NEGOCIO_ID, List.of()));
    }

    @Test
    @DisplayName("Un negocio con error no frena a los demás")
    void executeShouldContinueWhenOneNegocioFails() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID, OTRO_NEGOCIO_ID));
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenThrow(new IllegalStateException("Base caída"));
        when(proyectoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenReturn(List.of());
        when(regla.evaluar(any())).thenReturn(List.of());
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(OTRO_NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of());

        assertThatCode(ejecutarMotorAvisos::execute).doesNotThrowAnyException();

        verify(regla).evaluar(new ContextoNegocio(OTRO_NEGOCIO_ID, List.of()));
    }

    @Test
    @DisplayName("Arma las tablas de inventario con sus campos por clave y sus registros")
    void executeShouldBuildInventariosWithCamposAndRegistros() {
        Registro registro = AvisoTestData.registro(UUID.randomUUID(), "Yerba", 2, 5);
        givenProyecto(AvisoTestData.bloqueInventario());
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(registro));

        ContextoNegocio contextoArmado = thenContextoEvaluado();

        assertThat(contextoArmado.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(contextoArmado.inventarios()).singleElement().satisfies(inventario -> {
            assertThat(inventario.bloque().getId()).isEqualTo(AvisoTestData.INVENTARIO_ID);
            assertThat(inventario.camposPorClave()).containsOnlyKeys("name", "quantity", "minimum");
            assertThat(inventario.registros()).containsExactly(registro);
            assertThat(inventario.entradasId()).isNull();
        });
    }

    @Test
    @DisplayName("Ubica la tabla de entradas del proyecto y deja afuera las tablas que no son de inventario")
    void executeShouldFindEntradasAndIgnoreOtherBlocks() {
        Bloque entradas = Bloque.builder().id(AvisoTestData.ENTRADAS_ID).negocioId(NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.ENTRADAS).campos(List.of()).build();
        givenProyecto(AvisoTestData.bloqueInventario(), entradas);
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of());

        ContextoNegocio contextoArmado = thenContextoEvaluado();

        assertThat(contextoArmado.inventarios()).extracting(BloqueInventario::entradasId)
                .containsExactly(AvisoTestData.ENTRADAS_ID);
    }

    @Test
    @DisplayName("Ignora los campos sin clave semántica")
    void executeShouldIgnoreCamposWithoutClave() {
        Bloque bloque = AvisoTestData.bloqueInventario();
        bloque.setCampos(List.of(AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID, "name"),
                Campo.builder().id(UUID.randomUUID()).clave(null).build()));
        givenProyecto(bloque);
        when(registroRepository.findAllByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of());

        ContextoNegocio contextoArmado = thenContextoEvaluado();

        assertThat(contextoArmado.inventarios().getFirst().camposPorClave()).containsOnlyKeys("name");
    }

    @Test
    @DisplayName("Salta el proyecto que desapareció entre la lista y la carga")
    void executeShouldSkipProyectoThatCannotBeLoaded() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build()));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());

        assertThat(thenContextoEvaluado().inventarios()).isEmpty();
    }

    // --- arrange ---
    private EjecutarMotorAvisos motor(List<ReglaAviso> reglas) {
        return new EjecutarMotorAvisos(negocioRepository, reglas, proyectoRepository, registroRepository,
                avisoRepository, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    private void givenProyecto(Bloque... bloques) {
        Proyecto resumen = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        Proyecto completo = Proyecto.builder().id(AvisoTestData.PROYECTO_ID).negocioId(NEGOCIO_ID)
                .bloques(List.of(bloques)).build();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(resumen));
        when(proyectoRepository.findByIdAndNegocioId(AvisoTestData.PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(completo));
    }

    private ContextoNegocio thenContextoEvaluado() {
        when(regla.evaluar(any())).thenReturn(List.of());
        givenSinPendientes();
        ejecutarMotorAvisos.execute();
        ArgumentCaptor<ContextoNegocio> captor = ArgumentCaptor.forClass(ContextoNegocio.class);
        verify(regla).evaluar(captor.capture());
        return captor.getValue();
    }


    private void givenRegla(ResultadoRegla... resultados) {
        when(regla.evaluar(contexto)).thenReturn(List.of(resultados));
    }

    private void givenSinPendientes() {
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of());
    }

    private void givenSinAvisos() {
        givenSinPendientes();
        lenientHuellaVacia();
    }

    private void lenientHuellaVacia() {
        lenient().when(avisoRepository.findByNegocioIdAndHuella(any(), any())).thenReturn(Optional.empty());
    }

    private Aviso whenGuardado() {
        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static ResultadoRegla givenResultado(String entidad, int prioridad) {
        return givenResultado(entidad, prioridad, 4);
    }

    private static ResultadoRegla givenResultado(String entidad, int prioridad, int cantidad) {
        return new ResultadoRegla(null, entidad, (short) prioridad, "titulo", "detalle",
                Map.of("cantidad", cantidad), List.of(), List.of());
    }

    private static Aviso givenAvisoDe(ResultadoRegla resultado, EstadoAviso estado) {
        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, resultado, ANTES);
        aviso.setId(UUID.randomUUID());
        aviso.setEstado(estado);
        if (estado == EstadoAviso.DESCARTADO || estado == EstadoAviso.APLICADO) {
            aviso.setResueltoEn(ANTES);
        }
        return aviso;
    }
}
