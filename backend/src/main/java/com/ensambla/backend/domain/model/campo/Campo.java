package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campo {

    static final int MAX_CARACTERES_TEXTO = 1000;

    private UUID id;

    private UUID negocioId;

    private UUID bloqueId;

    private String nombre;

    private TipoCampo tipo;

    private Boolean obligatorio;

    /** Valor inicial serializado como texto ("12", "true", "2026-10-09"); se interpreta según el tipo. */
    private String valorInicial;

    /** Opciones válidas; sólo aplica a campos de tipo OPCION. */
    private List<String> opciones;

    /** Bloque al que apunta; sólo aplica a campos de tipo RELACION. */
    private UUID bloqueRelacionadoId;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** Campo nuevo del bloque, todavía sin guardar; {@code configuracion} ya viene validada. */
    public static Campo nuevo(
            UUID negocioId,
            UUID bloqueId,
            short posicion,
            ConfiguracionCampo.Validada configuracion,
            OffsetDateTime ahora) {
        Campo campo = Campo.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .posicion(posicion)
                .creadoEn(ahora)
                .build();
        campo.configurar(configuracion, ahora);
        return campo;
    }

    /** Reemplaza toda la configuración; conserva id, bloque, posición y fecha de creación. */
    public void configurar(ConfiguracionCampo.Validada configuracion, OffsetDateTime ahora) {
        this.nombre = configuracion.nombre();
        this.tipo = configuracion.tipo();
        this.obligatorio = configuracion.obligatorio();
        this.valorInicial = configuracion.valorInicial();
        this.opciones = configuracion.opciones();
        this.bloqueRelacionadoId = configuracion.bloqueRelacionadoId();
        this.actualizadoEn = ahora;
    }

    public boolean esObligatorio() {
        return Boolean.TRUE.equals(obligatorio);
    }

    public boolean esRelacion() {
        return tipo == TipoCampo.RELACION;
    }

    /** Mensaje de error si el valor no corresponde al tipo del campo; vacío si es válido. */
    public Optional<String> errorDe(Object valor) {
        return switch (tipo) {
            case TEXTO -> errorDeTexto(valor);
            case NUMERO -> errorSi(!esNumeroFinito(valor), "Tiene que ser un número");
            case FECHA -> errorSi(!esFechaIso(valor), "Tiene que ser una fecha válida");
            case OPCION -> errorSi(!esOpcion(valor), "Elegí una de las opciones");
            case SI_NO -> errorSi(!(valor instanceof Boolean), "Tiene que ser sí o no");
            case RELACION -> errorSi(!esUuid(valor), "Elegí una fila de la tabla relacionada");
        };
    }

    private Optional<String> errorDeTexto(Object valor) {
        if (!(valor instanceof String texto)) {
            return Optional.of("Tiene que ser un texto");
        }
        return errorSi(texto.length() > MAX_CARACTERES_TEXTO, "Máximo " + MAX_CARACTERES_TEXTO + " caracteres");
    }

    private static Optional<String> errorSi(boolean invalido, String mensaje) {
        return invalido ? Optional.of(mensaje) : Optional.empty();
    }

    private static boolean esNumeroFinito(Object valor) {
        return valor instanceof Number numero && Double.isFinite(numero.doubleValue());
    }

    private static boolean esFechaIso(Object valor) {
        if (!(valor instanceof String texto)) {
            return false;
        }
        try {
            LocalDate.parse(texto);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** El valor de una conexión es el id del registro referenciado, en su forma canónica de 36 caracteres. */
    private static boolean esUuid(Object valor) {
        if (!(valor instanceof String texto)) {
            return false;
        }
        try {
            return UUID.fromString(texto).toString().equalsIgnoreCase(texto);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean esOpcion(Object valor) {
        return valor instanceof String texto && opciones.contains(texto);
    }
}
