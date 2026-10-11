package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest.CampoItem;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest.FilaItem;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaResponse;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmarLecturaMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000011");
    private static final UUID LOTE_ID = UUID.fromString("00000000-0000-4000-8000-000000000013");
    private static final UUID PROYECTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000021");
    private static final UUID BLOQUE_ID = UUID.fromString("00000000-0000-4000-8000-000000000022");
    private static final UUID CAMPO_ID = UUID.fromString("00000000-0000-4000-8000-000000000023");

    private ConfirmarLecturaMapper confirmarLecturaMapper;

    @BeforeEach
    void setUp() {
        confirmarLecturaMapper = new ConfirmarLecturaMapper();
    }

    @Test
    @DisplayName("Pasa el pedido a la confirmación del dominio conservando destino, columnas y filas")
    void toConfirmacionShouldMapEveryField() {
        ConfirmarLecturaRequest request = request();

        ConfirmacionLectura result = confirmarLecturaMapper.toConfirmacion(request);

        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.nombreProyecto()).isEqualTo("Ventas");
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombreBloque()).isEqualTo("Hoja 1");
        assertThat(result.campos()).hasSize(1);
        assertThat(result.campos().get(0).clave()).isEqualTo("rubro");
        assertThat(result.campos().get(0).campoId()).isEqualTo(CAMPO_ID);
        assertThat(result.campos().get(0).nombre()).isEqualTo("Rubro");
        assertThat(result.campos().get(0).tipo()).isEqualTo("OPCION");
        assertThat(result.campos().get(0).opciones()).containsExactly("A", "B");
        assertThat(result.filas()).hasSize(1);
        assertThat(result.filas().get(0).pagina()).isEqualTo(2);
        assertThat(result.filas().get(0).valores()).containsEntry("rubro", "A");
    }

    @Test
    @DisplayName("Conserva listas vacías")
    void toConfirmacionShouldKeepEmptyLists() {
        ConfirmarLecturaRequest request = new ConfirmarLecturaRequest(
                USUARIO_ID, LOTE_ID, null, null, null, null, List.of(), List.of());

        ConfirmacionLectura result = confirmarLecturaMapper.toConfirmacion(request);

        assertThat(result.campos()).isEmpty();
        assertThat(result.filas()).isEmpty();
    }

    @Test
    @DisplayName("Arma la respuesta con el proyecto, el bloque y la cantidad de registros")
    void toResponseShouldMapEveryField() {
        ConfirmarLecturaResponse result = confirmarLecturaMapper.toResponse(
                new ConfirmacionGuardada(PROYECTO_ID, BLOQUE_ID, 7));

        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.registros()).isEqualTo(7);
    }

    // --- helpers ---
    private ConfirmarLecturaRequest request() {
        return new ConfirmarLecturaRequest(
                USUARIO_ID,
                LOTE_ID,
                PROYECTO_ID,
                "Ventas",
                BLOQUE_ID,
                "Hoja 1",
                List.of(new CampoItem("rubro", CAMPO_ID, "Rubro", "OPCION", List.of("A", "B"))),
                List.of(new FilaItem(2, Map.of("rubro", "A"))));
    }
}
