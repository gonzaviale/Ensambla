package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest.CampoItem;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class LeerImagenMapperTest {

    private final LeerImagenMapper mapper = new LeerImagenMapper(0.8);

    @Test
    @DisplayName("Arma la imagen con el tipo detectado")
    void toImagenShouldBuildTheImage() {
        LeerImagenRequest request = request(ImagenTestData.png(), campo("importe", "Monto", "NUMERO"));

        ImagenCuaderno imagen = mapper.toImagen(request);

        assertThat(imagen.contenido()).isEqualTo(request.contenido());
        assertThat(imagen.tipoContenido()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("No arma una imagen inválida")
    void toImagenShouldRejectAnInvalidImage() {
        LeerImagenRequest request = request(new byte[]{1, 2, 3}, campo("importe", "Monto", "NUMERO"));

        assertThatThrownBy(() -> mapper.toImagen(request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Arma el bloque con el tipo de cada campo y sus opciones")
    void toBloqueShouldBuildTheBlockWithFieldTypes() {
        LeerImagenRequest request = request(ImagenTestData.jpeg(), campo("importe", "Monto", "NUMERO"),
                new CampoItem("estado", "Estado", "OPCION", List.of("Pagó", "Debe")));

        BloqueDeImagen bloque = mapper.toBloque(request);

        assertThat(bloque.nombre()).isEqualTo("Ventas");
        assertThat(bloque.campos()).extracting("clave", "nombre", "tipo", "opciones").containsExactly(
                tuple("importe", "Monto", TipoCampo.NUMERO, List.of()),
                tuple("estado", "Estado", TipoCampo.OPCION, List.of("Pagó", "Debe")));
    }

    @Test
    @DisplayName("No arma un bloque sin campos")
    void toBloqueShouldRejectABlockWithoutFields() {
        LeerImagenRequest request = request(ImagenTestData.jpeg());

        assertThatThrownBy(() -> mapper.toBloque(request))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("no tiene datos");
    }

    @Test
    @DisplayName("Marca como dudoso el dato que no llega al umbral de confianza y devuelve las columnas nuevas")
    void toResponseShouldFlagLowConfidenceValuesAndListNewColumns() {
        LecturaImagen lectura = new LecturaImagen(
                List.of(Map.of("importe", new DatoLeido("1500", 0.95), "cliente", new DatoLeido("Ana", 0.3)),
                        Map.of("importe", new DatoLeido(null, 0.8))),
                List.of(new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of("A"))),
                List.of("Página torcida"));

        LeerImagenItem item = mapper.toResponse(lectura);

        assertThat(item.filas()).hasSize(2);
        assertThat(item.filas().get(0).get("importe")).isEqualTo(new LeerImagenItem.DatoItem("1500", false));
        assertThat(item.filas().get(0).get("cliente")).isEqualTo(new LeerImagenItem.DatoItem("Ana", true));
        assertThat(item.filas().get(1).get("importe")).isEqualTo(new LeerImagenItem.DatoItem(null, false));
        assertThat(item.camposNuevos()).containsExactly(
                new LeerImagenItem.CampoItem("telefono", "Teléfono", "TEXTO", List.of("A")));
        assertThat(item.advertencias()).containsExactly("Página torcida");
    }

    // --- arrange ---
    private LeerImagenRequest request(byte[] contenido, CampoItem... campos) {
        return new LeerImagenRequest(UUID.randomUUID(), contenido, "Ventas", List.of(campos), true);
    }

    private CampoItem campo(String clave, String nombre, String tipo) {
        return new CampoItem(clave, nombre, tipo, null);
    }
}
