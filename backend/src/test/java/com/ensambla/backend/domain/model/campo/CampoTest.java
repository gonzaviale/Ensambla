package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CampoTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final UUID RELACIONADO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Un campo marcado como obligatorio es obligatorio")
    void esObligatorioShouldBeTrueWhenMarked() {
        Campo campo = Campo.builder().obligatorio(true).build();

        assertThat(campo.esObligatorio()).isTrue();
    }

    @Test
    @DisplayName("Un campo sin marcar o sin dato no es obligatorio")
    void esObligatorioShouldBeFalseWhenNotMarkedOrNull() {
        Campo sinMarcar = Campo.builder().obligatorio(false).build();
        Campo sinDato = Campo.builder().build();

        assertThat(sinMarcar.esObligatorio()).isFalse();
        assertThat(sinDato.esObligatorio()).isFalse();
    }

    @Test
    @DisplayName("Acepta un texto de hasta 1000 caracteres")
    void errorDeShouldBeEmptyWhenTextoIsValid() {
        assertThat(errorDe(TipoCampo.TEXTO, "x".repeat(1000))).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto que supera los 1000 caracteres")
    void errorDeShouldReportWhenTextoIsTooLong() {
        assertThat(errorDe(TipoCampo.TEXTO, "x".repeat(1001))).contains("Máximo 1000 caracteres");
    }

    @Test
    @DisplayName("Rechaza un número cargado donde se espera un texto")
    void errorDeShouldReportWhenTextoIsNotAString() {
        assertThat(errorDe(TipoCampo.TEXTO, 12)).contains("Tiene que ser un texto");
    }

    @Test
    @DisplayName("Acepta enteros y decimales como número")
    void errorDeShouldBeEmptyWhenNumeroIsNumber() {
        assertThat(errorDe(TipoCampo.NUMERO, 12)).isEmpty();
        assertThat(errorDe(TipoCampo.NUMERO, 12.5)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto donde se espera un número")
    void errorDeShouldReportWhenNumeroIsString() {
        assertThat(errorDe(TipoCampo.NUMERO, "12")).contains("Tiene que ser un número");
    }

    @Test
    @DisplayName("Rechaza un número infinito")
    void errorDeShouldReportWhenNumeroIsNotFinite() {
        assertThat(errorDe(TipoCampo.NUMERO, Double.POSITIVE_INFINITY)).contains("Tiene que ser un número");
    }

    @Test
    @DisplayName("Acepta una fecha en formato AAAA-MM-DD")
    void errorDeShouldBeEmptyWhenFechaIsIso() {
        assertThat(errorDe(TipoCampo.FECHA, "2026-10-09")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una fecha que no existe en el calendario")
    void errorDeShouldReportWhenFechaDoesNotExist() {
        assertThat(errorDe(TipoCampo.FECHA, "2026-02-31")).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Rechaza una fecha con otro formato")
    void errorDeShouldReportWhenFechaHasOtherFormat() {
        assertThat(errorDe(TipoCampo.FECHA, "09/10/2026")).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Rechaza una fecha que no es un texto")
    void errorDeShouldReportWhenFechaIsNotAString() {
        assertThat(errorDe(TipoCampo.FECHA, 20261009)).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Acepta una de las opciones del campo")
    void errorDeShouldBeEmptyWhenOpcionIsListed() {
        assertThat(errorDe(TipoCampo.OPCION, "Mayorista")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una opción que no está en la lista")
    void errorDeShouldReportWhenOpcionIsNotListed() {
        assertThat(errorDe(TipoCampo.OPCION, "Otra")).contains("Elegí una de las opciones");
    }

    @Test
    @DisplayName("Rechaza una opción que no es un texto")
    void errorDeShouldReportWhenOpcionIsNotAString() {
        assertThat(errorDe(TipoCampo.OPCION, true)).contains("Elegí una de las opciones");
    }

    @Test
    @DisplayName("Acepta verdadero y falso como sí o no")
    void errorDeShouldBeEmptyWhenSiNoIsBoolean() {
        assertThat(errorDe(TipoCampo.SI_NO, false)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto donde se espera sí o no")
    void errorDeShouldReportWhenSiNoIsString() {
        assertThat(errorDe(TipoCampo.SI_NO, "sí")).contains("Tiene que ser sí o no");
    }

    @Test
    @DisplayName("Acepta el id de una fila como valor de una conexión")
    void errorDeShouldBeEmptyWhenRelacionIsUuid() {
        assertThat(errorDe(TipoCampo.RELACION, RELACIONADO_ID.toString())).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto que no es un id como valor de una conexión")
    void errorDeShouldReportWhenRelacionIsNotUuid() {
        assertThat(errorDe(TipoCampo.RELACION, "Ana")).contains("Elegí una fila de la tabla relacionada");
    }

    @Test
    @DisplayName("Rechaza un id abreviado como valor de una conexión")
    void errorDeShouldReportWhenRelacionIsNotCanonicalUuid() {
        assertThat(errorDe(TipoCampo.RELACION, "1-1-1-1-1")).contains("Elegí una fila de la tabla relacionada");
    }

    @Test
    @DisplayName("Rechaza un valor de conexión que no es un texto")
    void errorDeShouldReportWhenRelacionIsNotAString() {
        assertThat(errorDe(TipoCampo.RELACION, 12)).contains("Elegí una fila de la tabla relacionada");
    }

    @Test
    @DisplayName("Un campo de conexión es una relación")
    void esRelacionShouldBeTrueWhenTipoIsRelacion() {
        assertThat(campo(TipoCampo.RELACION).esRelacion()).isTrue();
    }

    @Test
    @DisplayName("Un campo de otro tipo no es una relación")
    void esRelacionShouldBeFalseWhenTipoIsOther() {
        assertThat(campo(TipoCampo.TEXTO).esRelacion()).isFalse();
    }

    @Test
    @DisplayName("Configurar un campo de conexión guarda la tabla con la que se conecta")
    void configurarShouldKeepBloqueRelacionado() {
        Campo campo = Campo.nuevo(NEGOCIO_ID, BLOQUE_ID, (short) 0, configuracion("Talle"), AHORA);
        ConfiguracionCampo.Validada relacion =
                new ConfiguracionCampo.Validada("Cliente", TipoCampo.RELACION, false, null, List.of(), RELACIONADO_ID);

        campo.configurar(relacion, AHORA);

        assertThat(campo.getBloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Crea un campo nuevo con la configuración validada y las dos fechas iguales")
    void nuevoShouldApplyConfiguracionAndSetDates() {
        Campo result = Campo.nuevo(NEGOCIO_ID, BLOQUE_ID, (short) 2, configuracion("Talle"), AHORA);

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getNombre()).isEqualTo("Talle");
        assertThat(result.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(result.getObligatorio()).isTrue();
        assertThat(result.getValorInicial()).isEqualTo("Chico");
        assertThat(result.getOpciones()).containsExactly("Chico", "Grande");
        assertThat(result.getBloqueRelacionadoId()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Configurar reemplaza la configuración y conserva la fecha de creación")
    void configurarShouldReplaceConfiguracionAndKeepCreationDate() {
        Campo campo = Campo.nuevo(NEGOCIO_ID, BLOQUE_ID, (short) 0, configuracion("Talle"), AHORA);
        ConfiguracionCampo.Validada nueva =
                new ConfiguracionCampo.Validada("Precio", TipoCampo.NUMERO, false, null, List.of(), null);

        campo.configurar(nueva, AHORA.plusHours(1));

        assertThat(campo.getNombre()).isEqualTo("Precio");
        assertThat(campo.getTipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(campo.getObligatorio()).isFalse();
        assertThat(campo.getValorInicial()).isNull();
        assertThat(campo.getOpciones()).isEmpty();
        assertThat(campo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campo.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    // --- act ---
    private Optional<String> errorDe(TipoCampo tipo, Object valor) {
        return campo(tipo).errorDe(valor);
    }

    // --- helpers ---
    private ConfiguracionCampo.Validada configuracion(String nombre) {
        return new ConfiguracionCampo.Validada(
                nombre, TipoCampo.OPCION, true, "Chico", List.of("Chico", "Grande"), null);
    }

    private Campo campo(TipoCampo tipo) {
        return Campo.builder()
                .tipo(tipo)
                .opciones(List.of("Minorista", "Mayorista"))
                .build();
    }
}
