package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotorAvisosServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-19T11:00:00Z");

    @Mock
    private ContextoNegocioService contextoNegocioService;

    @Mock
    private AvisoRepository avisoRepository;

    @Mock
    private ReglaAviso regla;

    private MotorAvisosService motorAvisosService;

    private final ContextoNegocio contexto = new ContextoNegocio(NEGOCIO_ID, List.of());

    @BeforeEach
    void setUp() {
        motorAvisosService = new MotorAvisosService(List.of(regla), contextoNegocioService, avisoRepository, new TransaccionDirecta());
        when(contextoNegocioService.armar(NEGOCIO_ID)).thenReturn(contexto);
        when(regla.tipo()).thenReturn(TipoAviso.STOCK);
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
    void ejecutarShouldCreateAvisoWhenRuleDetectsSomethingNew() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        givenRegla(resultado);
        givenSinAvisos();

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso creado = whenGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getEntidad()).isEqualTo("registro:1");
        assertThat(creado.getHuella()).isEqualTo(resultado.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("No duplica un aviso pendiente que la regla vuelve a detectar con los mismos datos")
    void ejecutarShouldNotDuplicatePendingAviso() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso pendiente = givenAvisoDe(resultado, EstadoAviso.PENDIENTE);
        givenRegla(resultado);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, pendiente.getHuella()))
                .thenReturn(Optional.of(pendiente));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        verify(avisoRepository).save(any(Aviso.class));
    }

    @Test
    @DisplayName("No vuelve a crear un aviso descartado mientras sus datos de origen no cambien")
    void ejecutarShouldNotRecreateDiscardedAvisoWithSameHuella() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso descartado = givenAvisoDe(resultado, EstadoAviso.DESCARTADO);
        givenRegla(resultado);
        givenSinPendientes();
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, descartado.getHuella()))
                .thenReturn(Optional.of(descartado));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("No vuelve a crear un aviso aplicado con los mismos datos")
    void ejecutarShouldNotRecreateAppliedAviso() {
        ResultadoRegla resultado = givenResultado("registro:1", 2);
        Aviso aplicado = givenAvisoDe(resultado, EstadoAviso.APLICADO);
        givenRegla(resultado);
        givenSinPendientes();
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, aplicado.getHuella()))
                .thenReturn(Optional.of(aplicado));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        verify(avisoRepository, never()).save(any(Aviso.class));
    }

    @Test
    @DisplayName("Un aviso descartado reaparece cuando cambian los datos que lo originaron")
    void ejecutarShouldCreateNewAvisoWhenDiscardedOneHasDifferentDatos() {
        ResultadoRegla antes = givenResultado("registro:1", 2, 4);
        givenAvisoDe(antes, EstadoAviso.DESCARTADO);
        ResultadoRegla ahora = givenResultado("registro:1", 3, 0);
        givenRegla(ahora);
        givenSinAvisos();

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso creado = whenGuardado();
        assertThat(creado.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(creado.getHuella()).isEqualTo(ahora.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("Si cambian los datos de un aviso pendiente, lo actualiza en lugar de crear otro")
    void ejecutarShouldUpdatePendingAvisoOfSameEntidadWhenDatosChange() {
        Aviso pendiente = givenAvisoDe(givenResultado("registro:1", 2, 4), EstadoAviso.PENDIENTE);
        ResultadoRegla ahora = givenResultado("registro:1", 3, 0);
        givenRegla(ahora);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, ahora.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.empty());

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getPrioridad()).isEqualTo((short) 3);
        assertThat(guardado.getHuella()).isEqualTo(ahora.huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("Reactiva un aviso vencido cuando la regla vuelve a detectarlo y vence el pendiente de la entidad")
    void ejecutarShouldReactivateExpiredAvisoAndExpireOtherPendingOfSameEntidad() {
        ResultadoRegla original = givenResultado("registro:1", 2, 4);
        Aviso vencido = givenAvisoDe(original, EstadoAviso.VENCIDO);
        Aviso otroPendiente = givenAvisoDe(givenResultado("registro:1", 2, 3), EstadoAviso.PENDIENTE);
        givenRegla(original);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(otroPendiente));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, original.huellaPara(TipoAviso.STOCK)))
                .thenReturn(Optional.of(vencido));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(avisoRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Aviso::getId, Aviso::getEstado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(otroPendiente.getId(), EstadoAviso.VENCIDO),
                        org.assertj.core.groups.Tuple.tuple(vencido.getId(), EstadoAviso.PENDIENTE));
    }

    @Test
    @DisplayName("Vence el aviso pendiente de una entidad que la regla ya no detecta")
    void ejecutarShouldExpirePendingAvisoWhenRuleNoLongerDetectsIt() {
        Aviso pendiente = givenAvisoDe(givenResultado("registro:1", 2), EstadoAviso.PENDIENTE);
        givenRegla();
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(new ArrayList<>(List.of(pendiente)));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(pendiente.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    @Test
    @DisplayName("Vence el pendiente de una entidad cuyo aviso ya fue descartado con los datos actuales")
    void ejecutarShouldExpireStalePendingWhenDiscardedAvisoMatchesCurrentDatos() {
        ResultadoRegla actual = givenResultado("registro:1", 2, 4);
        Aviso descartado = givenAvisoDe(actual, EstadoAviso.DESCARTADO);
        Aviso viejo = givenAvisoDe(givenResultado("registro:1", 2, 9), EstadoAviso.PENDIENTE);
        givenRegla(actual);
        when(avisoRepository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(viejo));
        when(avisoRepository.findByNegocioIdAndHuella(NEGOCIO_ID, descartado.getHuella()))
                .thenReturn(Optional.of(descartado));

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        Aviso guardado = whenGuardado();
        assertThat(guardado.getId()).isEqualTo(viejo.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
    }

    @Test
    @DisplayName("Ejecuta todas las reglas registradas")
    void ejecutarShouldRunEveryRegisteredRule() {
        ReglaAviso otra = org.mockito.Mockito.mock(ReglaAviso.class);
        when(otra.tipo()).thenReturn(TipoAviso.PRECIO_ZONA);
        when(otra.evaluar(contexto)).thenReturn(List.of());
        motorAvisosService = new MotorAvisosService(List.of(regla, otra), contextoNegocioService, avisoRepository, new TransaccionDirecta());
        givenRegla();
        givenSinAvisos();

        motorAvisosService.ejecutar(NEGOCIO_ID, AHORA);

        verify(otra).evaluar(contexto);
        verify(avisoRepository).findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.PRECIO_ZONA,
                EstadoAviso.PENDIENTE);
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
