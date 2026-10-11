package com.ensambla.backend.infrastructure.presentation.mapper.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.CrearBloqueRequest;
import com.ensambla.backend.domain.dto.bloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.dto.bloque.ListarBloquesRequest;
import com.ensambla.backend.domain.dto.bloque.RenombrarBloqueRequest;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloquePresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final BloquePresentationMapper bloquePresentationMapper = new BloquePresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar con el usuario y el proyecto")
    void toListarRequestShouldCarryUsuarioAndProyecto() {
        ListarBloquesRequest result = bloquePresentationMapper.toListarRequest(USUARIO_ID, PROYECTO_ID);

        assertThat(result).isEqualTo(new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de crear con el nombre del body")
    void toCrearRequestShouldCarryNombre() {
        CrearBloqueRequest result = bloquePresentationMapper.toCrearRequest(
                USUARIO_ID, PROYECTO_ID, new BloqueWebRequest("Clientes"));

        assertThat(result).isEqualTo(new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Clientes"));
    }

    @Test
    @DisplayName("Arma el pedido de renombrar con el bloque y el nombre del body")
    void toRenombrarRequestShouldCarryBloqueAndNombre() {
        RenombrarBloqueRequest result = bloquePresentationMapper.toRenombrarRequest(
                USUARIO_ID, BLOQUE_ID, new BloqueWebRequest("Compradores"));

        assertThat(result).isEqualTo(new RenombrarBloqueRequest(USUARIO_ID, BLOQUE_ID, "Compradores"));
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el bloque")
    void toEliminarRequestShouldCarryUsuarioAndBloque() {
        EliminarBloqueRequest result = bloquePresentationMapper.toEliminarRequest(USUARIO_ID, BLOQUE_ID);

        assertThat(result).isEqualTo(new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("Pasa todos los datos del bloque a la respuesta HTTP")
    void toResponseShouldMapEveryField() {
        BloqueResponse result = bloquePresentationMapper.toResponse(givenItem("Productos"));

        assertThat(result.id()).isEqualTo(BLOQUE_ID);
        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.nombre()).isEqualTo("Productos");
        assertThat(result.posicion()).isEqualTo((short) 1);
        assertThat(result.creadoEn()).isEqualTo(CREADO);
        assertThat(result.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Una lista conserva el orden y una vacía queda vacía")
    void toResponseListShouldKeepOrder() {
        List<BloqueResponse> result = bloquePresentationMapper.toResponse(
                List.of(givenItem("Primero"), givenItem("Segundo")));

        assertThat(result).extracting(BloqueResponse::nombre).containsExactly("Primero", "Segundo");
        assertThat(bloquePresentationMapper.toResponse(List.<BloqueItem>of())).isEmpty();
    }

    // --- helpers ---
    private BloqueItem givenItem(String nombre) {
        return new BloqueItem(BLOQUE_ID, PROYECTO_ID, nombre, (short) 1, CREADO, ACTUALIZADO);
    }
}
