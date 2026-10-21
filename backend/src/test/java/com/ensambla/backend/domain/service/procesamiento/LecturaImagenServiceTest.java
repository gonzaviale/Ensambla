package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LecturaImagenServiceTest {

    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final LecturaImagenService service = new LecturaImagenService(
            lectorCuadernoPort, new PropuestaLecturaService());

    private final ImagenCuaderno imagen = ImagenCuaderno.de(ImagenTestData.jpeg());
    private final BloqueDeImagen bloque = BloqueDeImagen.de("Ventas", List.of(
            CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of())));

    @Test
    @DisplayName("Devuelve lo leído con las claves del bloque y la columna que falta como nueva")
    void leerShouldReturnTheReadingWithTheBlockKeys() {
        givenLectura(List.of(columna("monto", "Monto"), columna("telefono", "Teléfono")),
                new FilaLeida(1, 1, Map.of("monto", new DatoLeido("1500", 0.95),
                        "telefono", new DatoLeido("1155", 0.5))));

        LecturaImagen lectura = service.leer(imagen, bloque, true);

        assertThat(lectura.camposNuevos()).extracting(CampoPropuesto::clave, CampoPropuesto::nombre)
                .containsExactly(tuple("telefono", "Teléfono"));
        assertThat(lectura.filas().getFirst().keySet()).containsExactlyInAnyOrder("importe", "telefono");
        assertThat(lectura.filas().getFirst().get("importe").valor()).isEqualTo("1500");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("La IA ve sólo el bloque elegido: una página y el bloque como destino")
    void leerShouldAskTheReaderForOnlyTheChosenBlock() {
        givenLectura(List.of(columna("monto", "Monto")), new FilaLeida(1, 1, Map.of()));

        service.leer(imagen, bloque, true);

        ArgumentCaptor<List<PaginaCuaderno>> paginas = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<BloqueConocido>> bloques = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<UUID> destino = ArgumentCaptor.forClass(UUID.class);
        verify(lectorCuadernoPort).leer(paginas.capture(), bloques.capture(), destino.capture());
        assertThat(paginas.getValue()).hasSize(1);
        assertThat(paginas.getValue().getFirst().contenido()).isEqualTo(imagen.contenido());
        assertThat(paginas.getValue().getFirst().tipoContenido()).isEqualTo("image/jpeg");
        assertThat(bloques.getValue()).hasSize(1);
        assertThat(bloques.getValue().getFirst().id()).isEqualTo(destino.getValue()).isEqualTo(bloque.id());
        assertThat(bloques.getValue().getFirst().nombre()).isEqualTo("Ventas");
        assertThat(bloques.getValue().getFirst().campos()).extracting("nombre").containsExactly("Monto");
    }

    @Test
    @DisplayName("Rechaza la foto cuando ninguna columna leída es del bloque")
    void leerShouldRejectAPhotoThatIsNotFromTheBlock() {
        givenLectura(List.of(columna("color", "Color")),
                new FilaLeida(1, 1, Map.of("color", new DatoLeido("Rojo", 0.9))));

        assertThatThrownBy(() -> service.leer(imagen, bloque, true))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    @Test
    @DisplayName("Propaga el error de la IA")
    void leerShouldPropagateTheReaderFailure() {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla de la IA"));

        assertThatThrownBy(() -> service.leer(imagen, bloque, true))
                .isInstanceOf(LecturaCuadernoException.class).hasMessage("Falla de la IA");
    }

    // --- arrange ---
    private void givenLectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(
                TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, campos), List.of(filas), List.of(),
                new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1)));
    }

    private CampoPropuesto columna(String clave, String nombre) {
        return new CampoPropuesto(clave, nombre, nombre.equals("Monto") ? TipoCampo.NUMERO : TipoCampo.TEXTO, List.of());
    }
}
