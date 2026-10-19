package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloqueTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID PRECIO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Test
    @DisplayName("Crea un bloque nuevo con el nombre normalizado, sin campos y con las dos fechas iguales")
    void nuevoShouldNormalizeNameAndSetDates() {
        Bloque result = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, "  Clientes  ", (short) 3, AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getPosicion()).isEqualTo((short) 3);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getCampos()).isEmpty();
    }

    @Test
    @DisplayName("Renombra conservando la fecha de creación y actualizando la de modificación")
    void renombrarShouldPreserveCreationDate() {
        Bloque bloque = Bloque.nuevo(NEGOCIO_ID, PROYECTO_ID, "Clientes", (short) 0, AHORA);

        bloque.renombrar("  Compradores ", AHORA.plusHours(1));

        assertThat(bloque.getNombre()).isEqualTo("Compradores");
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Agrega el campo después del último, en el bloque y el negocio del bloque")
    void agregarCampoShouldPlaceCampoAfterTheLastOne() {
        Bloque bloque = givenBloqueConCampos();

        Campo result = bloque.agregarCampo(configuracion(" Stock "), AHORA);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getNombre()).isEqualTo("Stock");
        assertThat(result.getPosicion()).isEqualTo((short) 4);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("El primer campo de un bloque vacío va en la posición 0")
    void agregarCampoShouldStartAtZeroWhenBloqueIsEmpty() {
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();

        Campo result = bloque.agregarCampo(configuracion("Producto"), AHORA);

        assertThat(result.getPosicion()).isEqualTo((short) 0);
    }

    @Test
    @DisplayName("Rechaza un nombre que ya usa otro campo sin distinguir mayúsculas ni espacios")
    void agregarCampoShouldRejectRepeatedNameIgnoringCase() {
        Bloque bloque = givenBloqueConCampos();

        assertThatThrownBy(() -> bloque.agregarCampo(configuracion("  PRECIO "), AHORA))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya hay un campo llamado \"PRECIO\" en este bloque");
    }

    @Test
    @DisplayName("Una configuración inválida se informa antes que un nombre repetido")
    void agregarCampoShouldReportInvalidConfiguracionFirst() {
        Bloque bloque = givenBloqueConCampos();
        ConfiguracionCampo invalida = new ConfiguracionCampo("Precio", "ARCHIVO", null, null, null, null, null);

        assertThatThrownBy(() -> bloque.agregarCampo(invalida, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    @DisplayName("Editar permite que el campo conserve su propio nombre con otras mayúsculas")
    void editarCampoShouldAllowTheSameCampo() {
        Bloque bloque = givenBloqueConCampos();
        Campo precio = givenCampo(PRECIO_ID, "Precio", 3);

        bloque.editarCampo(precio, configuracion("precio"), AHORA.plusHours(1));

        assertThat(precio.getNombre()).isEqualTo("precio");
        assertThat(precio.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Editar con el nombre de otro campo no modifica el campo")
    void editarCampoShouldRejectNameOfAnotherCampo() {
        Bloque bloque = givenBloqueConCampos();
        Campo precio = givenCampo(PRECIO_ID, "Precio", 3);

        assertThatThrownBy(() -> bloque.editarCampo(precio, configuracion("Producto"), AHORA))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(precio.getNombre()).isEqualTo("Precio");
    }

    @Test
    @DisplayName("Editar con una configuración inválida no modifica el campo")
    void editarCampoShouldNotModifyWhenConfiguracionIsInvalid() {
        Bloque bloque = givenBloqueConCampos();
        Campo precio = givenCampo(PRECIO_ID, "Precio", 3);
        ConfiguracionCampo invalida = new ConfiguracionCampo("Costo", "OPCION", null, null, List.of(), null, null);

        assertThatThrownBy(() -> bloque.editarCampo(precio, invalida, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        assertThat(precio.getNombre()).isEqualTo("Precio");
        assertThat(precio.getTipo()).isEqualTo(TipoCampo.NUMERO);
    }

    // --- helpers ---
    private Bloque givenBloqueConCampos() {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .negocioId(NEGOCIO_ID)
                .campos(List.of(givenCampo(PRODUCTO_ID, "Producto", 0), givenCampo(PRECIO_ID, " Precio", 3)))
                .build();
    }

    private Campo givenCampo(UUID id, String nombre, int posicion) {
        return Campo.builder()
                .id(id)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.NUMERO)
                .posicion((short) posicion)
                .build();
    }

    private ConfiguracionCampo configuracion(String nombre) {
        return new ConfiguracionCampo(nombre, "TEXTO", null, null, null, null, null);
    }
}
