package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroMappersTest {

    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Crear devuelve el registro con todos sus datos")
    void crearToResponseShouldMapEveryField() {
        CrearRegistroResponse response = new CrearRegistroMapper().toResponse(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Obtener devuelve el registro con todos sus datos")
    void obtenerToResponseShouldMapEveryField() {
        ObtenerRegistroResponse response = new ObtenerRegistroMapper().toResponse(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Editar devuelve el registro con todos sus datos")
    void editarToResponseShouldMapEveryField() {
        EditarRegistroResponse response = new EditarRegistroMapper().toResponse(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Listar devuelve los registros en orden con los datos de la página")
    void listarToResponseShouldMapContentAndPageData() {
        Registro segundo = Registro.builder().id(UUID.randomUUID()).bloqueId(BLOQUE_ID).build();
        Pagina<Registro> pagina = new Pagina<>(List.of(registro(), segundo), 2, 20, 45);

        ListarRegistrosResponse response = new ListarRegistrosMapper().toResponse(pagina);

        assertThat(response.contenido()).extracting(RegistroItem::id).containsExactly(REGISTRO_ID, segundo.getId());
        assertThat(response.pagina()).isEqualTo(2);
        assertThat(response.tamanio()).isEqualTo(20);
        assertThat(response.totalElementos()).isEqualTo(45);
        assertThat(response.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Listar devuelve una página vacía cuando no hay registros")
    void listarToResponseShouldReturnEmptyContent() {
        ListarRegistrosResponse response = new ListarRegistrosMapper().toResponse(new Pagina<>(List.of(), 0, 20, 0));

        assertThat(response.contenido()).isEmpty();
        assertThat(response.totalPaginas()).isEqualTo(1);
    }

    // --- assert ---
    private void thenItemHasEveryField(RegistroItem item) {
        assertThat(item.id()).isEqualTo(REGISTRO_ID);
        assertThat(item.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(item.valores()).containsEntry("campo", "dato");
        assertThat(item.creadoEn()).isEqualTo(CREADO);
        assertThat(item.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    // --- helpers ---
    private Registro registro() {
        return Registro.builder()
                .id(REGISTRO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of("campo", "dato"))
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }
}
