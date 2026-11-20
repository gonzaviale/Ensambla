package com.ensambla.backend.infrastructure.presentation.mapper.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampoRequest;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampoRequest;
import com.ensambla.backend.domain.useCase.campo.eliminarCampo.EliminarCampoRequest;
import com.ensambla.backend.domain.useCase.campo.listarCampos.ListarCamposRequest;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final CampoWebRequest TALLE = new CampoWebRequest("Talle", "OPCION", true, "Chico", TALLES, null, null);
    private static final CampoWebRequest CLIENTE =
            new CampoWebRequest("Cliente", "RELACION", false, null, null, RELACIONADO_ID, null);

    private final CampoPresentationMapper mapper = new CampoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar con el usuario y el bloque")
    void armaElPedidoDeListar() {
        entoncesElPedidoEs(mapper.toListarRequest(USUARIO_ID, BLOQUE_ID), new ListarCamposRequest(USUARIO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("Arma el pedido de crear con toda la configuración del body")
    void armaElPedidoDeCrearConTodaLaConfiguracion() {
        entoncesElPedidoEs(mapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, TALLE),
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));
    }

    @Test
    @DisplayName("Arma el pedido de editar con el campo y toda la configuración del body")
    void armaElPedidoDeEditarConTodaLaConfiguracion() {
        entoncesElPedidoEs(mapper.toEditarRequest(USUARIO_ID, CAMPO_ID, TALLE),
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));
    }

    @Test
    @DisplayName("Pasa la tabla conectada de un campo de conexión al pedido de crear")
    void pasaLaTablaConectadaAlPedidoDeCrear() {
        entoncesConectaConLaTabla(mapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, CLIENTE).bloqueRelacionadoId());
    }

    @Test
    @DisplayName("Pasa la tabla conectada de un campo de conexión al pedido de editar")
    void pasaLaTablaConectadaAlPedidoDeEditar() {
        entoncesConectaConLaTabla(mapper.toEditarRequest(USUARIO_ID, CAMPO_ID, CLIENTE).bloqueRelacionadoId());
    }

    @Test
    @DisplayName("Pasa a la respuesta la tabla con la que se conecta el campo")
    void pasaALaRespuestaLaTablaConectada() {
        CampoResponse respuesta = mapper.toResponse(unCampoDeConexion());

        entoncesConectaConLaTabla(respuesta.bloqueRelacionadoId());
        entoncesEsDeTipo(respuesta, "RELACION");
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el campo")
    void armaElPedidoDeEliminar() {
        entoncesElPedidoEs(mapper.toEliminarRequest(USUARIO_ID, CAMPO_ID), new EliminarCampoRequest(USUARIO_ID, CAMPO_ID));
    }

    @Test
    @DisplayName("Pasa todos los datos del campo a la respuesta HTTP")
    void pasaTodosLosDatosDelCampoALaRespuesta() {
        entoncesElCampoSeRespondeComo(unCampoDeTalle("Talle"), new CampoResponse(CAMPO_ID, BLOQUE_ID, "Talle", "OPCION",
                true, "Chico", TALLES, null, null, (short) 1, CREADO, ACTUALIZADO));
    }

    @Test
    @DisplayName("Una lista conserva el orden de los campos")
    void unaListaConservaElOrdenDeLosCampos() {
        entoncesLaListaTieneLosNombres(List.of(unCampoDeTalle("Primero"), unCampoDeTalle("Segundo")), "Primero", "Segundo");
    }

    @Test
    @DisplayName("Una lista vacía queda vacía")
    void unaListaVaciaQuedaVacia() {
        entoncesLaListaTieneLosNombres(List.of());
    }

    private static CampoItem unCampoDeTalle(String nombre) {
        return new CampoItem(CAMPO_ID, BLOQUE_ID, nombre, "OPCION", true, "Chico", TALLES, null, null, (short) 1, CREADO,
                ACTUALIZADO);
    }

    private static CampoItem unCampoDeConexion() {
        return new CampoItem(CAMPO_ID, BLOQUE_ID, "Cliente", "RELACION", false, null, List.of(), RELACIONADO_ID, null,
                (short) 2, CREADO, ACTUALIZADO);
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private static void entoncesConectaConLaTabla(BloqueId bloqueRelacionado) {
        assertThat(bloqueRelacionado).isEqualTo(RELACIONADO_ID);
    }

    private static void entoncesEsDeTipo(CampoResponse campo, String tipo) {
        assertThat(campo.tipo()).isEqualTo(tipo);
    }

    private void entoncesElCampoSeRespondeComo(CampoItem campo, CampoResponse esperada) {
        assertThat(mapper.toResponse(campo)).isEqualTo(esperada);
    }

    private void entoncesLaListaTieneLosNombres(List<CampoItem> campos, String... nombres) {
        assertThat(mapper.toResponse(campos)).extracting(CampoResponse::nombre).containsExactly(nombres);
    }
}