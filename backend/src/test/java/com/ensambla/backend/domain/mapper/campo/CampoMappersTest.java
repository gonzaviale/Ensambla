package com.ensambla.backend.domain.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.CrearCampoRequest;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CampoMappersTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final UUID RELACIONADO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");

    private final CampoItemMapper campoItemMapper = new CampoItemMapper();

    @Test
    @DisplayName("Pasa todos los datos del campo a la respuesta, con el tipo como texto")
    void campoItemMapperShouldMapEveryField() {
        CampoItem result = campoItemMapper.toResponse(givenCampo("Talle", true));

        assertThat(result.id()).isEqualTo(CAMPO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombre()).isEqualTo("Talle");
        assertThat(result.tipo()).isEqualTo("OPCION");
        assertThat(result.obligatorio()).isTrue();
        assertThat(result.valorInicial()).isEqualTo("Chico");
        assertThat(result.opciones()).containsExactly("Chico", "Grande");
        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
        assertThat(result.posicion()).isEqualTo((short) 1);
        assertThat(result.creadoEn()).isEqualTo(CREADO);
        assertThat(result.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    @Test
    @DisplayName("Un campo sin la marca de obligatorio sale como no obligatorio")
    void campoItemMapperShouldMapNullObligatorioAsFalse() {
        CampoItem result = campoItemMapper.toResponse(givenCampo("Talle", null));

        assertThat(result.obligatorio()).isFalse();
    }

    @Test
    @DisplayName("Listar conserva el orden de los campos")
    void listarCamposMapperShouldKeepOrder() {
        List<CampoItem> result = new ListarCamposMapper(campoItemMapper)
                .toResponse(List.of(givenCampo("Primero", true), givenCampo("Segundo", true)));

        assertThat(result).extracting(CampoItem::nombre).containsExactly("Primero", "Segundo");
    }

    @Test
    @DisplayName("Listar sin campos devuelve una lista vacía")
    void listarCamposMapperShouldReturnEmptyList() {
        List<CampoItem> result = new ListarCamposMapper(campoItemMapper).toResponse(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Crear arma la configuración con todos los datos del pedido")
    void crearCampoMapperShouldBuildConfiguracion() {
        CrearCampoRequest request =
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, RELACIONADO_ID);

        ConfiguracionCampo result = new CrearCampoMapper(campoItemMapper).toConfiguracion(request);

        assertThat(result).isEqualTo(new ConfiguracionCampo("Talle", "OPCION", true, "Chico", TALLES, RELACIONADO_ID));
    }

    @Test
    @DisplayName("Crear devuelve el campo mapeado")
    void crearCampoMapperShouldMapCampo() {
        CampoItem result = new CrearCampoMapper(campoItemMapper).toResponse(givenCampo("Talle", true));

        assertThat(result.nombre()).isEqualTo("Talle");
    }

    @Test
    @DisplayName("Editar arma la configuración con todos los datos del pedido")
    void editarCampoMapperShouldBuildConfiguracion() {
        EditarCampoRequest request =
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Talle", "OPCION", false, null, TALLES, RELACIONADO_ID);

        ConfiguracionCampo result = new EditarCampoMapper(campoItemMapper).toConfiguracion(request);

        assertThat(result).isEqualTo(new ConfiguracionCampo("Talle", "OPCION", false, null, TALLES, RELACIONADO_ID));
    }

    @Test
    @DisplayName("Editar devuelve el campo mapeado")
    void editarCampoMapperShouldMapCampo() {
        CampoItem result = new EditarCampoMapper(campoItemMapper).toResponse(givenCampo("Medida", true));

        assertThat(result.nombre()).isEqualTo("Medida");
    }

    // --- helpers ---
    private Campo givenCampo(String nombre, Boolean obligatorio) {
        return Campo.builder()
                .id(CAMPO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.OPCION)
                .obligatorio(obligatorio)
                .valorInicial("Chico")
                .opciones(TALLES)
                .bloqueRelacionadoId(RELACIONADO_ID)
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }
}
