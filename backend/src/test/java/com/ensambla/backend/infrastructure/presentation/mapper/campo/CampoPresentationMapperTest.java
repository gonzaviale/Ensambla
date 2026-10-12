package com.ensambla.backend.infrastructure.presentation.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.CrearCampoRequest;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.dto.campo.EliminarCampoRequest;
import com.ensambla.backend.domain.dto.campo.ListarCamposRequest;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CampoPresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final UUID RELACIONADO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final CampoWebRequest BODY = new CampoWebRequest("Talle", "OPCION", true, "Chico", TALLES, null);

    private final CampoPresentationMapper campoPresentationMapper = new CampoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar con el usuario y el bloque")
    void toListarRequestShouldCarryUsuarioAndBloque() {
        ListarCamposRequest result = campoPresentationMapper.toListarRequest(USUARIO_ID, BLOQUE_ID);

        assertThat(result).isEqualTo(new ListarCamposRequest(USUARIO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("Arma el pedido de crear con toda la configuración del body")
    void toCrearRequestShouldCarryConfiguracion() {
        CrearCampoRequest result = campoPresentationMapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, BODY);

        assertThat(result).isEqualTo(
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null));
    }

    @Test
    @DisplayName("Arma el pedido de editar con el campo y toda la configuración del body")
    void toEditarRequestShouldCarryCampoAndConfiguracion() {
        EditarCampoRequest result = campoPresentationMapper.toEditarRequest(USUARIO_ID, CAMPO_ID, BODY);

        assertThat(result).isEqualTo(
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Talle", "OPCION", true, "Chico", TALLES, null));
    }

    @Test
    @DisplayName("Pasa la tabla conectada de un campo de conexión al pedido de crear")
    void toCrearRequestShouldCarryBloqueRelacionado() {
        CampoWebRequest body = new CampoWebRequest("Cliente", "RELACION", false, null, null, RELACIONADO_ID);

        CrearCampoRequest result = campoPresentationMapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, body);

        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Pasa la tabla conectada de un campo de conexión al pedido de editar")
    void toEditarRequestShouldCarryBloqueRelacionado() {
        CampoWebRequest body = new CampoWebRequest("Cliente", "RELACION", false, null, null, RELACIONADO_ID);

        EditarCampoRequest result = campoPresentationMapper.toEditarRequest(USUARIO_ID, CAMPO_ID, body);

        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Pasa a la respuesta la tabla con la que se conecta el campo")
    void toResponseShouldMapBloqueRelacionado() {
        CampoItem item = new CampoItem(
                CAMPO_ID, BLOQUE_ID, "Cliente", "RELACION", false, null, List.of(), RELACIONADO_ID, (short) 2, CREADO,
                ACTUALIZADO);

        CampoResponse result = campoPresentationMapper.toResponse(item);

        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
        assertThat(result.tipo()).isEqualTo("RELACION");
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el campo")
    void toEliminarRequestShouldCarryUsuarioAndCampo() {
        EliminarCampoRequest result = campoPresentationMapper.toEliminarRequest(USUARIO_ID, CAMPO_ID);

        assertThat(result).isEqualTo(new EliminarCampoRequest(USUARIO_ID, CAMPO_ID));
    }

    @Test
    @DisplayName("Pasa todos los datos del campo a la respuesta HTTP")
    void toResponseShouldMapEveryField() {
        CampoResponse result = campoPresentationMapper.toResponse(givenItem("Talle"));

        assertThat(result.id()).isEqualTo(CAMPO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombre()).isEqualTo("Talle");
        assertThat(result.tipo()).isEqualTo("OPCION");
        assertThat(result.obligatorio()).isTrue();
        assertThat(result.valorInicial()).isEqualTo("Chico");
        assertThat(result.opciones()).containsExactly("Chico", "Grande");
        assertThat(result.bloqueRelacionadoId()).isNull();
        assertThat(result.posicion()).isEqualTo((short) 1);
        assertThat(result.creadoEn()).isEqualTo(CREADO);
        assertThat(result.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Una lista conserva el orden de los campos")
    void toResponseListShouldKeepOrder() {
        List<CampoResponse> result = campoPresentationMapper.toResponse(
                List.of(givenItem("Primero"), givenItem("Segundo")));

        assertThat(result).extracting(CampoResponse::nombre).containsExactly("Primero", "Segundo");
    }

    @Test
    @DisplayName("Una lista vacía queda vacía")
    void toResponseListShouldReturnEmptyList() {
        List<CampoResponse> result = campoPresentationMapper.toResponse(List.<CampoItem>of());

        assertThat(result).isEmpty();
    }

    // --- helpers ---
    private CampoItem givenItem(String nombre) {
        return new CampoItem(
                CAMPO_ID, BLOQUE_ID, nombre, "OPCION", true, "Chico", TALLES, null, (short) 1, CREADO, ACTUALIZADO);
    }
}
