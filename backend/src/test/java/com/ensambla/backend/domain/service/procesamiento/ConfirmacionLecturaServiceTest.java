package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.enums.EstadoLote;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmacionLecturaServiceTest {

    private static final UUID PROYECTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000021");
    private static final UUID BLOQUE_ID = UUID.fromString("00000000-0000-4000-8000-000000000022");
    private static final UUID MONTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000023");
    private static final UUID FECHA_ID = UUID.fromString("00000000-0000-4000-8000-000000000024");
    private static final UUID EXTRA_ID = UUID.fromString("00000000-0000-4000-8000-000000000025");
    private static final String NOMBRE_PROYECTO = "Ventas";
    private static final String NOMBRE_BLOQUE = "Hoja 1";

    @Mock
    private ProyectoService proyectoService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private LoteSubidaService loteSubidaService;

    @InjectMocks
    private ConfirmacionLecturaService confirmacionLecturaService;

    @Test
    @DisplayName("Crea proyecto, tabla, columnas y registros con su foto y confirma el lote al final")
    void confirmarShouldCreateEverythingAndConfirmLote() {
        Bloque bloque = givenProyectoYBloqueNuevos();
        givenCampoNuevoDe("Monto", campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        givenCampoNuevoDe("Fecha", campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false));
        givenCamposDelBloque(List.of(), List.of(
                campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false), campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false)));
        LoteSubida lote = loteListo();

        ConfirmacionGuardada result = confirmar(lote, confirmacionNueva(
                filaConPagina(1, 850, "2026-10-03"), filaSinPagina(100)));

        assertThat(result).isEqualTo(new ConfirmacionGuardada(PROYECTO_ID, BLOQUE_ID, 2));
        List<Registro> guardados = thenRegistrosGuardados();
        assertThat(guardados).hasSize(2);
        assertThat(guardados.get(0).getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardados.get(0).getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardados.get(0).getFotoOrigenId()).isEqualTo(FOTO_ID);
        assertThat(guardados.get(0).getValores())
                .containsEntry(MONTO_ID.toString(), 850)
                .containsEntry(FECHA_ID.toString(), "2026-10-03");
        assertThat(guardados.get(1).getFotoOrigenId()).isNull();
        assertThat(guardados.get(1).getValores()).containsOnly(Map.entry(MONTO_ID.toString(), 100));
        thenLoteConfirmedAfterSavingRegistros(lote, bloque);
    }

    @Test
    @DisplayName("Crea la tabla dentro de un proyecto existente sin crear otro proyecto")
    void confirmarShouldCreateBloqueInExistingProyecto() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(bloqueService.crear(proyecto, NOMBRE_BLOQUE, AHORA)).thenReturn(bloque());
        givenCampoNuevoDe("Monto", campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        givenCamposDelBloque(List.of(), List.of(campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false)));

        ConfirmacionGuardada result = confirmar(loteListo(), confirmacion(
                PROYECTO_ID, null, null, List.of(columnaNueva("monto", "Monto", "NUMERO")), List.of(filaSinPagina(5))));

        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        verify(proyectoService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Suma las filas a un bloque existente, reutiliza sus campos y agrega los nuevos")
    void confirmarShouldAddRowsToExistingBloque() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque());
        Campo monto = campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false);
        Campo nota = campo(FECHA_ID, "Nota", TipoCampo.TEXTO, false);
        givenCampoNuevoDe("Nota", nota);
        givenCamposDelBloque(List.of(monto), List.of(monto, nota));
        List<CampoConfirmado> columnas = List.of(
                new CampoConfirmado("monto", MONTO_ID, "Monto", "NUMERO", List.of()),
                columnaNueva("nota", "Nota", "TEXTO"));
        Map<String, Object> valores = Map.of("monto", 20, "nota", "  fiado ");

        ConfirmacionGuardada result = confirmar(loteListo(), confirmacion(
                null, null, BLOQUE_ID, columnas, List.of(new FilaConfirmada(1, valores))));

        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(thenRegistrosGuardados().get(0).getValores())
                .containsEntry(MONTO_ID.toString(), 20)
                .containsEntry(FECHA_ID.toString(), "fiado");
        verifyNoInteractions(proyectoService);
        verify(bloqueService, never()).crear(any(), any(), any());
    }

    @Test
    @DisplayName("Rechaza una columna que apunta a un campo que no es del bloque")
    void confirmarShouldThrowWhenCampoDoesNotBelongToBloque() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        ConfirmacionLectura confirmacion = confirmacion(null, null, BLOQUE_ID,
                List.of(new CampoConfirmado("monto", MONTO_ID, "Monto", "NUMERO", List.of())), List.of());
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El campo no existe");
        verifyNoInteractions(registroDatosService);
    }

    @Test
    @DisplayName("Ubica el error de una columna nueva por su posición")
    void confirmarShouldLocateInvalidColumnError() {
        givenProyectoYBloqueNuevos();
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(campoService.crear(any(Bloque.class), any(ConfiguracionCampo.class), eq(AHORA)))
                .thenThrow(new CampoInvalidoException(Map.of("nombre", "Escribí un nombre")));
        ConfirmacionLectura confirmacion = confirmacionNueva(filaSinPagina(1));
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOfSatisfying(CampoInvalidoException.class,
                        e -> assertThat(e.getErrores()).containsOnly(Map.entry("campos[0].nombre", "Escribí un nombre")));
        verifyNoInteractions(registroDatosService);
    }

    @Test
    @DisplayName("Ubica el valor inválido de una fila por su posición y su columna y no guarda nada")
    void confirmarShouldLocateInvalidValueAndSaveNothing() {
        givenProyectoYBloqueNuevos();
        givenCampoNuevoDe("Monto", campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        givenCampoNuevoDe("Fecha", campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false));
        givenCamposDelBloque(List.of(), List.of(
                campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false), campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false)));
        ConfirmacionLectura confirmacion = confirmacionNueva(filaConPagina(1, 850, "2026-10-03"), filaSinPagina("abc"));
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOfSatisfying(RegistroInvalidoException.class,
                        e -> assertThat(e.getErrores()).containsOnly(Map.entry("filas[1].monto", "Tiene que ser un número")));
        verifyNoInteractions(registroDatosService);
        verifyNoInteractions(loteSubidaService);
    }

    @Test
    @DisplayName("Marca una página que el lote no tiene")
    void confirmarShouldRejectRowFromMissingPage() {
        givenProyectoYBloqueNuevos();
        givenCampoNuevoDe("Monto", campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false));
        givenCampoNuevoDe("Fecha", campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false));
        givenCamposDelBloque(List.of(), List.of(
                campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false), campo(FECHA_ID, "Fecha", TipoCampo.FECHA, false)));
        ConfirmacionLectura confirmacion = confirmacionNueva(filaConPagina(4, 850, "2026-10-03"));
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOfSatisfying(RegistroInvalidoException.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(Map.entry("filas[0].pagina", "Esta página no está en el cuaderno")));
        verifyNoInteractions(registroDatosService);
    }

    @Test
    @DisplayName("Ubica por id el error de un campo obligatorio del bloque que la revisión no incluyó")
    void confirmarShouldReportMissingRequiredCampoOfExistingBloque() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque());
        Campo monto = campo(MONTO_ID, "Monto", TipoCampo.NUMERO, false);
        Campo extra = campo(EXTRA_ID, "Extra", TipoCampo.TEXTO, true);
        givenCamposDelBloque(List.of(monto, extra), List.of(monto, extra));
        List<CampoConfirmado> columnas = List.of(new CampoConfirmado("monto", MONTO_ID, "Monto", "NUMERO", List.of()));
        ConfirmacionLectura confirmacion = confirmacion(
                null, null, BLOQUE_ID, columnas, List.of(new FilaConfirmada(1, Map.of("monto", 3))));
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOfSatisfying(RegistroInvalidoException.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(Map.entry("filas[0]." + EXTRA_ID, "Este dato es obligatorio")));
    }

    @Test
    @DisplayName("No toca nada cuando el lote no se puede confirmar")
    void confirmarShouldNotTouchAnythingWhenLoteIsNotConfirmable() {
        LoteSubida lote = loteListo();
        lote.setEstado(EstadoLote.CONFIRMADO);
        ConfirmacionLectura confirmacion = confirmacionNueva(filaSinPagina(1));

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este cuaderno ya se guardó");
        verifyNoInteractions(proyectoService, bloqueService, campoService, registroDatosService, loteSubidaService);
    }

    @Test
    @DisplayName("No toca nada cuando la confirmación no tiene columnas")
    void confirmarShouldNotTouchAnythingWhenConfirmacionIsInvalid() {
        ConfirmacionLectura confirmacion = confirmacion(null, NOMBRE_PROYECTO, null, List.of(), List.of());
        LoteSubida lote = loteListo();

        assertThatThrownBy(() -> confirmar(lote, confirmacion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dejá al menos una columna");
        verifyNoInteractions(proyectoService, bloqueService, campoService, registroDatosService, loteSubidaService);
    }

    // --- arrange ---
    private Bloque givenProyectoYBloqueNuevos() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).build();
        Bloque bloque = bloque();
        when(proyectoService.crear(NEGOCIO_ID, NOMBRE_PROYECTO, null, AHORA)).thenReturn(proyecto);
        when(bloqueService.crear(proyecto, NOMBRE_BLOQUE, AHORA)).thenReturn(bloque);
        return bloque;
    }

    private void givenCampoNuevoDe(String nombre, Campo campo) {
        when(campoService.crear(any(Bloque.class), argThat(configuracion -> nombre.equals(configuracion.nombre())),
                eq(AHORA))).thenReturn(campo);
    }

    private void givenCamposDelBloque(List<Campo> antes, List<Campo> despues) {
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(antes, despues);
    }

    // --- act ---
    private ConfirmacionGuardada confirmar(LoteSubida lote, ConfirmacionLectura confirmacion) {
        return confirmacionLecturaService.confirmar(NEGOCIO_ID, lote, confirmacion, AHORA);
    }

    // --- assert ---
    private List<Registro> thenRegistrosGuardados() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Registro>> registros = ArgumentCaptor.forClass(List.class);
        verify(registroDatosService).crearVarios(registros.capture());
        return registros.getValue();
    }

    private void thenLoteConfirmedAfterSavingRegistros(LoteSubida lote, Bloque bloque) {
        InOrder orden = inOrder(registroDatosService, loteSubidaService);
        orden.verify(registroDatosService).crearVarios(any());
        orden.verify(loteSubidaService).confirmar(lote, bloque.getId(), AHORA);
    }

    // --- helpers ---
    private LoteSubida loteListo() {
        LoteSubida lote = LoteTestData.lote(List.of(LoteTestData.foto()));
        lote.setId(LOTE_ID);
        lote.setEstado(EstadoLote.LISTO);
        return lote;
    }

    private Bloque bloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build();
    }

    private Campo campo(UUID id, String nombre, TipoCampo tipo, boolean obligatorio) {
        return Campo.builder().id(id).nombre(nombre).tipo(tipo).obligatorio(obligatorio).build();
    }

    private CampoConfirmado columnaNueva(String clave, String nombre, String tipo) {
        return new CampoConfirmado(clave, null, nombre, tipo, List.of());
    }

    private ConfirmacionLectura confirmacionNueva(FilaConfirmada... filas) {
        return confirmacion(null, NOMBRE_PROYECTO, null, List.of(
                columnaNueva("monto", "Monto", "NUMERO"), columnaNueva("fecha", "Fecha", "FECHA")), List.of(filas));
    }

    private ConfirmacionLectura confirmacion(UUID proyectoId, String nombreProyecto, UUID bloqueId,
                                             List<CampoConfirmado> campos, List<FilaConfirmada> filas) {
        return new ConfirmacionLectura(proyectoId, nombreProyecto, bloqueId, NOMBRE_BLOQUE, campos, filas);
    }

    private FilaConfirmada filaConPagina(int pagina, Object monto, String fecha) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("monto", monto);
        valores.put("fecha", fecha);
        return new FilaConfirmada(pagina, valores);
    }

    private FilaConfirmada filaSinPagina(Object monto) {
        return new FilaConfirmada(null, Map.of("monto", monto));
    }
}
