package com.ensambla.backend.infrastructure.presentation.mapper.negocio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.useCase.negocio.ListaNegociosResponse;
import com.ensambla.backend.domain.useCase.negocio.NegocioDeLaCuentaItem;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.CrearNegocioWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.UbicacionNegocioWeb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class NegociosPresentationMapperTest {

    private final NegociosPresentationMapper mapper = new NegociosPresentationMapper();

    @Test
    @DisplayName("Mapea las operaciones de la cuenta con ids tipados")
    void shouldMapAccountRequests() {
        assertThat(mapper.toListarRequest(USUARIO_ID).usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(mapper.toCambiarRequest(USUARIO_ID, NEGOCIO_ID))
                .extracting("usuarioId", "negocioId").containsExactly(USUARIO_ID, NEGOCIO_ID);
        assertThat(mapper.toEliminarRequest(USUARIO_ID, NEGOCIO_ID))
                .extracting("usuarioId", "negocioId").containsExactly(USUARIO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Mapea una creación sin ubicación")
    void shouldMapCreateWithoutLocation() {
        var result = mapper.toCrearRequest(USUARIO_ID,
                new CrearNegocioWebRequest("Sucursal", null, null));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.nombre()).isEqualTo("Sucursal");
        assertThat(result.rubro()).isNull();
        assertThat(result.ubicacion()).isNull();
    }

    @Test
    @DisplayName("Mapea una creación con ubicación completa")
    void shouldMapCreateWithLocation() {
        BigDecimal latitud = new BigDecimal("-34.670123");
        BigDecimal longitud = new BigDecimal("-58.560789");
        var result = mapper.toCrearRequest(USUARIO_ID, new CrearNegocioWebRequest(
                "Sucursal", RubroNegocio.ALMACEN,
                new UbicacionNegocioWeb("Arieta 3200", "San Justo", latitud, longitud)));

        assertThat(result.rubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(result.ubicacion()).extracting(
                "direccion", "localidad", "latitud", "longitud")
                .containsExactly("Arieta 3200", "San Justo", latitud, longitud);
    }

    @Test
    @DisplayName("Mapea la lista y cada negocio al contrato web")
    void shouldMapResponses() {
        NegocioId segundoId = NegocioId.nuevo();
        OffsetDateTime despues = FECHA.plusDays(1);
        ListaNegociosResponse response = new ListaNegociosResponse(NEGOCIO_ID, 6, true, List.of(
                new NegocioDeLaCuentaItem(NEGOCIO_ID, "Central", "ALMACEN", true, FECHA),
                new NegocioDeLaCuentaItem(segundoId, "Sucursal", "KIOSCO", false, despues)));

        var result = mapper.toResponse(response);

        assertThat(result.activoId()).isEqualTo(NEGOCIO_ID.toString());
        assertThat(result.limite()).isEqualTo(6);
        assertThat(result.puedeCrear()).isTrue();
        assertThat(result.negocios()).hasSize(2);
        assertThat(result.negocios().get(0)).extracting(
                "id", "nombre", "rubro", "activo", "creadoEn")
                .containsExactly(NEGOCIO_ID.toString(), "Central", "ALMACEN", true, FECHA);
        assertThat(mapper.toResponse(response.negocios().get(1))).extracting(
                "id", "nombre", "rubro", "activo", "creadoEn")
                .containsExactly(segundoId.toString(), "Sucursal", "KIOSCO", false, despues);
    }
}
