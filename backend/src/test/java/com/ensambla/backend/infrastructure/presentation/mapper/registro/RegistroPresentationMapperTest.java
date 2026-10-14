package com.ensambla.backend.infrastructure.presentation.mapper.registro;

import com.ensambla.backend.domain.dto.registro.TotalesItem;
import java.math.BigDecimal;
import com.ensambla.backend.domain.dto.registro.CrearRegistroRequest;
import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EditarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ListarReferenciasRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.dto.registro.ReferenciaItem;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.infrastructure.presentation.dto.registro.PaginaRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.ReferenciaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThat;

class RegistroPresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID FOTO_ID = UUID.fromString("f0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final Map<String, Object> VALORES = Map.of("campo", "dato");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final RegistroPresentationMapper mapper = new RegistroPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de referencias con el texto y los ids pedidos")
    void toReferenciasRequestShouldCarryTextoAndIds() {
        ListarReferenciasRequest result =
                mapper.toReferenciasRequest(USUARIO_ID, BLOQUE_ID, "ana", List.of(REGISTRO_ID));

        assertThat(result).isEqualTo(new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, "ana", List.of(REGISTRO_ID)));
    }

    @Test
    @DisplayName("Sin el parámetro ids el pedido de referencias lleva una lista vacía")
    void toReferenciasRequestShouldUseEmptyListWhenIdsAreMissing() {
        ListarReferenciasRequest result = mapper.toReferenciasRequest(USUARIO_ID, BLOQUE_ID, null, null);

        assertThat(result.ids()).isEmpty();
    }

    @Test
    @DisplayName("Pasa el id y la etiqueta de cada referencia, en orden")
    void toResponseShouldMapReferencias() {
        List<ReferenciaResponse> result = mapper.toResponse(
                List.of(new ReferenciaItem(REGISTRO_ID, "Ana"), new ReferenciaItem(OTRO_REGISTRO_ID, "Beto")));

        assertThat(result).containsExactly(
                new ReferenciaResponse(REGISTRO_ID, "Ana"), new ReferenciaResponse(OTRO_REGISTRO_ID, "Beto"));
    }

    @Test
    @DisplayName("Arma el pedido de crear con el usuario de la sesión, el bloque y los valores")
    void toCrearRequestShouldCarryUsuarioBloqueAndValores() {
        CrearRegistroRequest result = mapper.toCrearRequest(USUARIO_ID, BLOQUE_ID, new RegistroRequest(VALORES));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.valores()).isEqualTo(VALORES);
    }

    @Test
    @DisplayName("Arma el pedido de listar con todos los parámetros de la consulta")
    void toListarRequestShouldCarryEveryParameter() {
        ListarRegistrosRequest result = mapper.toListarRequest(
                USUARIO_ID, BLOQUE_ID, "harina", "creadoEn", "asc", 2, 10);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.texto()).isEqualTo("harina");
        assertThat(result.orden()).isEqualTo("creadoEn");
        assertThat(result.direccion()).isEqualTo("asc");
        assertThat(result.pagina()).isEqualTo(2);
        assertThat(result.tamanio()).isEqualTo(10);
    }

    @Test
    @DisplayName("Arma el pedido de obtener con el usuario y el registro")
    void toObtenerRequestShouldCarryUsuarioAndRegistro() {
        ObtenerRegistroRequest result = mapper.toObtenerRequest(USUARIO_ID, REGISTRO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.registroId()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("Arma el pedido de editar con el usuario, el registro y los cambios")
    void toEditarRequestShouldCarryUsuarioRegistroAndValores() {
        EditarRegistroRequest result = mapper.toEditarRequest(USUARIO_ID, REGISTRO_ID, new RegistroRequest(VALORES));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.registroId()).isEqualTo(REGISTRO_ID);
        assertThat(result.valores()).isEqualTo(VALORES);
    }

    @Test
    @DisplayName("Arma el pedido de la foto de origen con el usuario y el registro")
    void toFotoOrigenRequestShouldCarryUsuarioAndRegistro() {
        ObtenerFotoOrigenRequest result = mapper.toFotoOrigenRequest(USUARIO_ID, REGISTRO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.registroId()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el registro")
    void toEliminarRequestShouldCarryUsuarioAndRegistro() {
        EliminarRegistroRequest result = mapper.toEliminarRequest(USUARIO_ID, REGISTRO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.registroId()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("Convierte la respuesta de crear, obtener y editar en el mismo registro web")
    void toResponseShouldMapSingleRegistroResponses() {
        RegistroItem item = new RegistroItem(REGISTRO_ID, BLOQUE_ID, VALORES, FOTO_ID, CREADO, ACTUALIZADO);

        RegistroResponse creado = mapper.toResponse(new CrearRegistroResponse(item));
        RegistroResponse obtenido = mapper.toResponse(new ObtenerRegistroResponse(item));
        RegistroResponse editado = mapper.toResponse(new EditarRegistroResponse(item));

        assertThat(creado).isEqualTo(obtenido).isEqualTo(editado);
        assertThat(creado.id()).isEqualTo(REGISTRO_ID);
        assertThat(creado.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(creado.valores()).isEqualTo(VALORES);
        assertThat(creado.fotoOrigenId()).isEqualTo(FOTO_ID);
        assertThat(creado.creadoEn()).isEqualTo(CREADO);
        assertThat(creado.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Marca la primera página y no la última cuando hay más páginas")
    void toResponseShouldFlagFirstPageWhenThereAreMore() {
        PaginaRegistrosResponse result = mapper.toResponse(pagina(0, 3));

        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(45);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("Marca la última página y no la primera")
    void toResponseShouldFlagLastPage() {
        PaginaRegistrosResponse result = mapper.toResponse(pagina(2, 3));

        assertThat(result.first()).isFalse();
        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("Pasa los totales de la búsqueda a la respuesta web")
    void toResponseShouldCarryTotals() {
        PaginaRegistrosResponse result = mapper.toResponse(pagina(0, 3));

        assertThat(result.totales().cantidad()).isEqualTo(45);
        assertThat(result.totales().sumas()).containsOnly(entry(CAMPO_ID, new BigDecimal("1500.50")));
    }

    // --- helpers ---
    private ListarRegistrosResponse pagina(int numero, int totalPaginas) {
        return new ListarRegistrosResponse(
                List.of(new RegistroItem(REGISTRO_ID, BLOQUE_ID, VALORES, FOTO_ID, CREADO, ACTUALIZADO)),
                numero,
                20,
                45,
                totalPaginas,
                new TotalesItem(45, Map.of(CAMPO_ID, new BigDecimal("1500.50"))));
    }
}
