package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvisoUseCasesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000009");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Clock CLOCK = Clock.fixed(INSTANTE, ZoneOffset.UTC);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final RegistroDatosService registroDatosService = mock(RegistroDatosService.class);
    private final CampoService campoService = mock(CampoService.class);
    private final AccesoContenidoService accesoContenidoService = mock(AccesoContenidoService.class, invocation -> {
        if (invocation.getMethod().getName().equals("seleccionar")) {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves, 0, null, claves.size(), 0, AHORA.plusMonths(1));
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    });
    private final AvisoService avisoService = new AvisoService(avisoRepository);
    private final ListarAvisos listarAvisos =
            new ListarAvisos(negocioService, avisoRepository, accesoContenidoService, CLOCK);
    private final ContarAvisos contarAvisos = new ContarAvisos(negocioService, avisoRepository);
    private final AplicarAviso aplicarAviso = new AplicarAviso(negocioService, avisoService, accesoContenidoService,
            registroDatosService, campoService, avisoRepository, new TransaccionDirecta(), CLOCK);
    private final DescartarAviso descartarAviso = new DescartarAviso(negocioService, avisoService,
            accesoContenidoService, avisoRepository, new TransaccionDirecta(), CLOCK);

    @BeforeEach
    void givenNegocioDelUsuario() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- listar ---

    @Test
    @DisplayName("Listar pone primero los pendientes, del más urgente al menos urgente y del más nuevo al más viejo")
    void listarShouldPutPendingFirstThenByPrioridadAndDate() {
        Aviso descartado = givenAviso("descartado", 3, EstadoAviso.DESCARTADO, 5);
        Aviso pocoUrgente = givenAviso("poco urgente", 1, EstadoAviso.PENDIENTE, 5);
        Aviso viejoUrgente = givenAviso("viejo urgente", 3, EstadoAviso.PENDIENTE, 1);
        Aviso nuevoUrgente = givenAviso("nuevo urgente", 3, EstadoAviso.PENDIENTE, 4);
        when(avisoRepository.findByNegocioId(NEGOCIO_ID))
                .thenReturn(List.of(descartado, pocoUrgente, viejoUrgente, nuevoUrgente));

        List<AvisoItem> result = listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, null));

        assertThat(result).extracting(AvisoItem::titulo)
                .containsExactly("nuevo urgente", "viejo urgente", "poco urgente", "descartado");
    }

    @Test
    @DisplayName("Listar con estado devuelve sólo los avisos de ese estado del negocio del usuario")
    void listarShouldFilterByEstado() {
        Aviso pendiente = givenAviso("pendiente", 2, EstadoAviso.PENDIENTE, 1);
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(pendiente));

        List<AvisoItem> result = listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.PENDIENTE));

        assertThat(result).extracting(AvisoItem::id).containsExactly(pendiente.getId());
        verify(avisoRepository, never()).findByNegocioId(any());
    }

    // --- contar ---

    @Test
    @DisplayName("Contar devuelve los pendientes y cuántos son urgentes del negocio del usuario")
    void contarShouldCountPendingAndUrgent() {
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(
                givenAviso("a", 3, EstadoAviso.PENDIENTE, 1),
                givenAviso("b", 2, EstadoAviso.PENDIENTE, 1),
                givenAviso("c", 1, EstadoAviso.PENDIENTE, 1)));

        ContadorAvisos result = contarAvisos.execute(USUARIO_ID);

        assertThat(result).isEqualTo(new ContadorAvisos(3, 2));
    }

    // --- aplicar ---

    @Test
    @DisplayName("Aplicar busca el aviso en el negocio del usuario y lo guarda aplicado con la hora del reloj")
    void aplicarShouldApplyAvisoOfUsersNegocio() {
        Aviso aviso = givenGuardado(givenAvisoConAcciones());

        AvisoItem result = aplicar();

        assertThat(result.id()).isEqualTo(AVISO_ID);
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.APLICADO);
        assertThat(aviso.getResueltoEn()).isEqualTo(AHORA);
        verify(avisoRepository).save(aviso);
    }

    @Test
    @DisplayName("Aplicar un aviso de otro negocio responde 'no existe' y no aplica nada")
    void aplicarShouldNotApplyForeignAviso() {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(this::aplicar)
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("El aviso no existe");
        verify(avisoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aplicar un aviso que ya fue resuelto no ejecuta su acción ni lo guarda")
    void aplicarShouldThrowWithoutRunningActionWhenAvisoWasResolved() {
        Aviso aviso = givenGuardado(givenAvisoConAcciones(editarPrecio(Map.of("price", 4200))));
        aviso.setEstado(EstadoAviso.DESCARTADO);

        assertThatThrownBy(this::aplicar).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(registroDatosService);
        verify(avisoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar registro cambia los valores del registro usando la clave del campo antes de guardar el aviso")
    void aplicarShouldEditRegistroByCampoClave() {
        Aviso aviso = givenGuardado(givenAvisoConAcciones(editarPrecio(Map.of("price", 4200))));
        Registro registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 2, 5);
        List<Campo> campos = List.of(AvisoTestData.campo(CAMPO_PRECIO_ID.valor(), "price"),
                AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID.valor(), "name"));
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(registro);
        when(campoService.listarDeBloque(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID)).thenReturn(campos);

        aplicar();

        InOrder orden = inOrder(registroDatosService, avisoRepository);
        orden.verify(registroDatosService).editar(registro, campos, Map.of(CAMPO_PRECIO_ID.toString(), 4200), AHORA);
        orden.verify(avisoRepository).save(aviso);
    }

    @Test
    @DisplayName("Las acciones que sólo abren pantallas no tocan ningún dato")
    void aplicarShouldDoNothingForNavigationActions() {
        givenGuardado(givenAvisoConAcciones(Map.of("tipo", "REGISTRAR", "bloqueId", "x"), "texto suelto"));

        aplicar();

        verifyNoInteractions(registroDatosService, campoService);
    }

    @Test
    @DisplayName("Rechaza una acción de edición con un registro que no es un id")
    void aplicarShouldThrowWhenRegistroIdIsInvalid() {
        givenGuardado(givenAvisoConAcciones(Map.of("tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id",
                "valores", Map.of("price", 1))));

        assertThatThrownBy(this::aplicar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La acción del aviso no indica qué registro cambiar");
    }

    @Test
    @DisplayName("Rechaza una acción de edición sin valores")
    void aplicarShouldThrowWhenValoresAreMissing() {
        givenRegistroYCampos();
        givenGuardado(givenAvisoConAcciones(Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString())));

        assertThatThrownBy(this::aplicar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La acción del aviso no indica qué valores cambiar");
    }

    @Test
    @DisplayName("Rechaza una acción de edición con valores vacíos")
    void aplicarShouldThrowWhenValoresAreEmpty() {
        givenRegistroYCampos();
        givenGuardado(givenAvisoConAcciones(editarPrecio(Map.of())));

        assertThatThrownBy(this::aplicar).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una acción cuyos campos ya no existen en la tabla")
    void aplicarShouldThrowWhenCamposNoLongerExist() {
        givenRegistroYCampos();
        givenGuardado(givenAvisoConAcciones(editarPrecio(Map.of("costo-que-no-existe", 1))));

        assertThatThrownBy(this::aplicar)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La tabla ya no tiene los campos que el aviso quería cambiar");
        verify(registroDatosService, never()).editar(any(), any(), any(), any());
    }

    // --- descartar ---

    @Test
    @DisplayName("Descartar busca el aviso en el negocio del usuario y lo guarda descartado con la hora del reloj")
    void descartarShouldDiscardAvisoOfUsersNegocio() {
        Aviso aviso = givenGuardado(givenAvisoConAcciones());

        AvisoItem result = descartarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));

        assertThat(result.id()).isEqualTo(AVISO_ID);
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.DESCARTADO);
        assertThat(aviso.getResueltoEn()).isEqualTo(AHORA);
        verify(avisoRepository).save(aviso);
    }

    // --- arrange ---
    private Aviso givenGuardado(Aviso aviso) {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.of(aviso));
        return aviso;
    }

    private void givenRegistroYCampos() {
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 2, 5));
        when(campoService.listarDeBloque(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(AvisoTestData.campo(CAMPO_PRECIO_ID.valor(), "price")));
    }

    private static Map<String, Object> editarPrecio(Map<String, Object> valores) {
        return Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString(), "valores", valores);
    }

    private static Aviso givenAvisoConAcciones(Object... acciones) {
        return Aviso.builder().id(AVISO_ID).negocioId(NEGOCIO_ID).estado(EstadoAviso.PENDIENTE)
                .prioridad((short) 2).acciones(List.of(acciones)).build();
    }

    private static Aviso givenAviso(String titulo, int prioridad, EstadoAviso estado, int hora) {
        return Aviso.builder()
                .id(AvisoId.nuevo()).negocioId(NEGOCIO_ID).titulo(titulo).prioridad((short) prioridad)
                .estado(estado).creadoEn(AHORA.withHour(hora)).acciones(List.of())
                .build();
    }

    // --- act ---
    private AvisoItem aplicar() {
        return aplicarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }
}
