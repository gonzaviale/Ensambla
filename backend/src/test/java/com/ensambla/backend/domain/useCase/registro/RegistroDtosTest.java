package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosResponse;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class RegistroDtosTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Crear devuelve el registro con todos sus datos")
    void crearToResponseShouldMapEveryField() {
        CrearRegistroResponse response = CrearRegistroResponse.de(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Obtener devuelve el registro con todos sus datos")
    void obtenerToResponseShouldMapEveryField() {
        ObtenerRegistroResponse response = ObtenerRegistroResponse.de(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Editar devuelve el registro con todos sus datos")
    void editarToResponseShouldMapEveryField() {
        EditarRegistroResponse response = EditarRegistroResponse.de(registro());

        thenItemHasEveryField(response.registro());
    }

    @Test
    @DisplayName("Listar devuelve los registros en orden con los datos de la página")
    void listarToResponseShouldMapContentAndPageData() {
        Registro segundo = Registro.builder().id(RegistroId.nuevo()).bloqueId(BLOQUE_ID).build();
        Pagina<Registro> pagina = new Pagina<>(List.of(registro(), segundo), 2, 20, 45);

        TotalesRegistros totales = new TotalesRegistros(45, Map.of(CAMPO_ID, new BigDecimal("1500.50")));

        ListarRegistrosResponse response = ListarRegistrosResponse.de(pagina, totales);

        assertThat(response.contenido()).extracting(RegistroItem::id).containsExactly(REGISTRO_ID, segundo.getId());
        assertThat(response.pagina()).isEqualTo(2);
        assertThat(response.tamanio()).isEqualTo(20);
        assertThat(response.totalElementos()).isEqualTo(45);
        assertThat(response.totalPaginas()).isEqualTo(3);
        assertThat(response.totales().cantidad()).isEqualTo(45);
        assertThat(response.totales().sumas()).containsOnly(entry(CAMPO_ID, new BigDecimal("1500.50")));
    }

    @Test
    @DisplayName("Listar devuelve una página vacía cuando no hay registros")
    void listarToResponseShouldReturnEmptyContent() {
        ListarRegistrosResponse response = ListarRegistrosResponse.de(
                new Pagina<>(List.of(), 0, 20, 0), new TotalesRegistros(0, Map.of()));

        assertThat(response.contenido()).isEmpty();
        assertThat(response.totales().cantidad()).isZero();
        assertThat(response.totales().sumas()).isEmpty();
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
