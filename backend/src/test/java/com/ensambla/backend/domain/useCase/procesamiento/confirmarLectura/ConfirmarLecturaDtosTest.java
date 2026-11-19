package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaRequest.CampoItem;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaRequest.FilaItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmarLecturaDtosTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000011");
    private static final LoteId LOTE_ID = LoteId.de("00000000-0000-4000-8000-000000000013");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("00000000-0000-4000-8000-000000000021");
    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000022");
    private static final CampoId CAMPO_ID = CampoId.de("00000000-0000-4000-8000-000000000023");

    private ConfirmacionLectura confirmacion;
    private ConfirmarLecturaResponse respuesta;

    @Test
    @DisplayName("Pasa el pedido a la confirmación del dominio conservando destino, columnas y filas")
    void pasaElPedidoALaConfirmacionDelDominioConservandoDestinoColumnasYFilas() {
        cuandoSePasaElPedidoCompletoALaConfirmacion();

        entoncesLaConfirmacionConservaDestinoColumnasYFilas();
    }

    @Test
    @DisplayName("Conserva listas vacías")
    void conservaListasVacias() {
        cuandoSePasaUnPedidoSinColumnasNiFilasALaConfirmacion();

        entoncesLaConfirmacionNoTieneColumnasNiFilas();
    }

    @Test
    @DisplayName("Arma la respuesta con el proyecto, el bloque y la cantidad de registros")
    void armaLaRespuestaConElProyectoElBloqueYLaCantidadDeRegistros() {
        cuandoSeArmaLaRespuestaDeSieteRegistros();

        entoncesLaRespuestaTraeProyectoBloqueYSieteRegistros();
    }

    // --- cuando ---
    private void cuandoSePasaElPedidoCompletoALaConfirmacion() {
        confirmacion = new ConfirmarLecturaRequest(USUARIO_ID, LOTE_ID, PROYECTO_ID, "Ventas", BLOQUE_ID, "Hoja 1",
                List.of(new CampoItem("rubro", CAMPO_ID, "Rubro", "OPCION", List.of("A", "B"))),
                List.of(new FilaItem(2, Map.of("rubro", "A")))).aConfirmacionLectura();
    }

    private void cuandoSePasaUnPedidoSinColumnasNiFilasALaConfirmacion() {
        confirmacion = new ConfirmarLecturaRequest(
                USUARIO_ID, LOTE_ID, null, null, null, null, List.of(), List.of()).aConfirmacionLectura();
    }

    private void cuandoSeArmaLaRespuestaDeSieteRegistros() {
        respuesta = ConfirmarLecturaResponse.de(new ConfirmacionGuardada(PROYECTO_ID, BLOQUE_ID, 7));
    }

    // --- entonces ---
    private void entoncesLaConfirmacionConservaDestinoColumnasYFilas() {
        assertThat(confirmacion.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(confirmacion.nombreProyecto()).isEqualTo("Ventas");
        assertThat(confirmacion.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(confirmacion.nombreBloque()).isEqualTo("Hoja 1");
        assertThat(confirmacion.campos()).hasSize(1);
        assertThat(confirmacion.campos().get(0).clave()).isEqualTo("rubro");
        assertThat(confirmacion.campos().get(0).campoId()).isEqualTo(CAMPO_ID);
        assertThat(confirmacion.campos().get(0).nombre()).isEqualTo("Rubro");
        assertThat(confirmacion.campos().get(0).tipo()).isEqualTo("OPCION");
        assertThat(confirmacion.campos().get(0).opciones()).containsExactly("A", "B");
        assertThat(confirmacion.filas()).hasSize(1);
        assertThat(confirmacion.filas().get(0).pagina()).isEqualTo(2);
        assertThat(confirmacion.filas().get(0).valores()).containsEntry("rubro", "A");
    }

    private void entoncesLaConfirmacionNoTieneColumnasNiFilas() {
        assertThat(confirmacion.campos()).isEmpty();
        assertThat(confirmacion.filas()).isEmpty();
    }

    private void entoncesLaRespuestaTraeProyectoBloqueYSieteRegistros() {
        assertThat(respuesta.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(respuesta.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(respuesta.registros()).isEqualTo(7);
    }
}
