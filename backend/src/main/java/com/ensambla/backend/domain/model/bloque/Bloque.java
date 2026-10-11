package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bloque {

    private UUID id;

    private UUID negocioId;

    private UUID proyectoId;

    private String nombre;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    @Builder.Default
    private List<Campo> campos = List.of();

    public static Bloque nuevo(UUID negocioId, UUID proyectoId, String nombre, short posicion, OffsetDateTime ahora) {
        return Bloque.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre.strip())
                .posicion(posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public void renombrar(String nombre, OffsetDateTime ahora) {
        this.nombre = nombre.strip();
        this.actualizadoEn = ahora;
    }

    /** Campo nuevo al final de la lista del bloque; todavía sin guardar. Requiere los campos cargados. */
    public Campo agregarCampo(ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        ConfiguracionCampo.Validada validada = configuracion.validar();
        validarNombreDeCampoLibre(validada.nombre(), null);
        return Campo.nuevo(negocioId, id, siguientePosicionDeCampo(), validada, ahora);
    }

    /**
     * Reemplaza la configuración de un campo de este bloque. Valida todo antes de tocar el campo, así un error no lo
     * deja a medio modificar. Requiere los campos cargados.
     */
    public void editarCampo(Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        ConfiguracionCampo.Validada validada = configuracion.validar();
        validarNombreDeCampoLibre(validada.nombre(), campo.getId());
        campo.configurar(validada, ahora);
    }

    /**
     * El nombre de un campo es único dentro del bloque sin distinguir mayúsculas ni espacios de los extremos.
     * {@code campoId} es el campo que se está editando (puede conservar su propio nombre); null al crear.
     */
    public void validarNombreDeCampoLibre(String nombre, UUID campoId) {
        String buscado = nombre.strip();
        boolean ocupado = campos.stream()
                .filter(campo -> !campo.getId().equals(campoId))
                .anyMatch(campo -> campo.getNombre().strip().equalsIgnoreCase(buscado));
        if (ocupado) {
            throw new DuplicateResourceException("Ya hay un campo llamado \"" + buscado + "\" en este bloque");
        }
    }

    private short siguientePosicionDeCampo() {
        return (short) campos.stream()
                .mapToInt(campo -> campo.getPosicion() + 1)
                .max()
                .orElse(0);
    }
}
