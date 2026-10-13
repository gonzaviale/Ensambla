package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Registro {

    private static final String MENSAJE_OBLIGATORIO = "Este dato es obligatorio";

    private UUID id;

    private UUID negocioId;

    private UUID bloqueId;

    /** Valores cargados, indexados por el id del campo (así renombrar un campo no rompe los datos). */
    private Map<String, Object> valores;

    /** Foto del cuaderno de la que salió el registro; {@code null} si se cargó a mano. */
    private UUID fotoOrigenId;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public static Registro nuevo(UUID negocioId, UUID bloqueId, Map<String, Object> valores, OffsetDateTime ahora) {
        return Registro.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .valores(valores)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** Registro que salió de una página del cuaderno; {@code fotoOrigenId} es null si la fila se agregó a mano. */
    public static Registro desdeCuaderno(UUID negocioId, UUID bloqueId, Map<String, Object> valores,
                                         UUID fotoOrigenId, OffsetDateTime ahora) {
        Registro registro = nuevo(negocioId, bloqueId, valores, ahora);
        registro.setFotoOrigenId(fotoOrigenId);
        return registro;
    }

    /**
     * Controla los valores contra los campos del bloque: tipo, obligatoriedad y opciones. Descarta las claves que no
     * son campos del bloque y los valores vacíos; los textos se devuelven sin espacios en los bordes.
     *
     * @throws RegistroInvalidoException con un mensaje por cada campo inválido
     */
    public static Map<String, Object> validar(List<Campo> campos, Map<String, Object> valores) {
        Map<String, String> errores = new LinkedHashMap<>();
        Map<String, Object> limpios = new LinkedHashMap<>();
        for (Campo campo : campos.stream().filter(campo -> !campo.esFormula()).toList()) {
            String clave = campo.getId().toString();
            Object valor = sinVacios(valores.get(clave));
            if (valor == null) {
                registrarFaltante(campo, clave, errores);
            } else {
                campo.errorDe(valor).ifPresentOrElse(error -> errores.put(clave, error),
                        () -> limpios.put(clave, valor));
            }
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
        return limpios;
    }

    /**
     * Copia del registro con el valor de cada campo FORMULA calculado a partir de sus datos; no se guarda. Una fórmula
     * a la que le falta un dato (o que divide por cero) queda sin valor.
     */
    public Registro conFormulas(List<Campo> campos) {
        Map<String, Object> conCalculados = new LinkedHashMap<>(valores);
        for (Campo campo : campos.stream().filter(Campo::esFormula).toList()) {
            BigDecimal calculado = campo.calcular(this::numeroDe);
            if (calculado != null) {
                conCalculados.put(campo.getId().toString(), calculado);
            }
        }
        return toBuilder().valores(conCalculados).build();
    }

    private BigDecimal numeroDe(UUID campoId) {
        return valores.get(campoId.toString()) instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    /**
     * @throws ResourceNotFoundException si el registro es de otro bloque: responde igual que uno inexistente
     */
    public void exigirDelBloque(UUID otroBloqueId) {
        if (!bloqueId.equals(otroBloqueId)) {
            throw new ResourceNotFoundException("El registro no existe");
        }
    }

    /**
     * Id de la foto de la que salió el registro.
     *
     * @throws ResourceNotFoundException si se cargó a mano y no tiene foto de origen
     */
    public UUID exigirFotoOrigen() {
        if (fotoOrigenId == null) {
            throw new ResourceNotFoundException("El registro no tiene foto de origen");
        }
        return fotoOrigenId;
    }

    /** Valores actuales con los cambios encima; un cambio {@code null} o en blanco borra el dato. */
    public Map<String, Object> combinarCon(Map<String, Object> cambios) {
        Map<String, Object> combinados = new LinkedHashMap<>(valores);
        combinados.putAll(cambios);
        return combinados;
    }

    public void reemplazarValores(Map<String, Object> nuevosValores, OffsetDateTime ahora) {
        this.valores = nuevosValores;
        this.actualizadoEn = ahora;
    }

    private static void registrarFaltante(Campo campo, String clave, Map<String, String> errores) {
        if (campo.esObligatorio()) {
            errores.put(clave, MENSAJE_OBLIGATORIO);
        }
    }

    private static Object sinVacios(Object valor) {
        if (valor instanceof String texto) {
            String limpio = texto.strip();
            return limpio.isEmpty() ? null : limpio;
        }
        return valor;
    }
}
