package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proyecto {

    private UUID id;

    private UUID negocioId;

    private String nombre;

    private String descripcion;

    /** Plantilla de origen; null si se creó en blanco o si la plantilla ya no existe. */
    private UUID plantillaId;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    @Builder.Default
    private List<Bloque> bloques = List.of();

    public static Proyecto enBlanco(UUID negocioId, String nombre, String descripcion, OffsetDateTime ahora) {
        return Proyecto.builder()
                .negocioId(negocioId)
                .nombre(nombre.strip())
                .descripcion(descripcion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public static Proyecto desdePlantilla(UUID negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        Proyecto proyecto = enBlanco(negocioId, plantilla.nombre(), plantilla.descripcion(), ahora);
        proyecto.setPlantillaId(plantilla.id());
        List<Bloque> bloques = new ArrayList<>();
        for (BloquePlantilla definicion : plantilla.estructura().bloques()) {
            Bloque bloque = Bloque.builder()
                    .negocioId(negocioId)
                    .nombre(definicion.nombre())
                    .posicion((short) bloques.size())
                    .creadoEn(ahora)
                    .actualizadoEn(ahora)
                    .build();
            List<Campo> campos = new ArrayList<>();
            for (CampoPlantilla campo : definicion.campos()) {
                campos.add(Campo.builder()
                        .negocioId(negocioId)
                        .nombre(campo.nombre())
                        .tipo(campo.tipo())
                        .obligatorio(campo.obligatorio())
                        .valorInicial(campo.valorInicial())
                        .opciones(campo.opciones())
                        .posicion((short) campos.size())
                        .creadoEn(ahora)
                        .actualizadoEn(ahora)
                        .build());
            }
            bloque.setCampos(campos);
            bloques.add(bloque);
        }
        proyecto.setBloques(bloques);
        return proyecto;
    }

    public void renombrar(String nombre, OffsetDateTime ahora) {
        this.nombre = nombre.strip();
        this.actualizadoEn = ahora;
    }

    /** Bloque nuevo al final de la lista del constructor; todavía sin guardar. */
    public Bloque agregarBloque(String nombre, OffsetDateTime ahora) {
        validarNombreDeBloqueLibre(nombre, null);
        return Bloque.nuevo(negocioId, id, nombre, siguientePosicionDeBloque(), ahora);
    }

    /**
     * El nombre de un bloque es único dentro del proyecto sin distinguir mayúsculas ni espacios de los extremos.
     * {@code bloqueId} es el bloque que se está renombrando (puede conservar su propio nombre); null al crear.
     */
    public void validarNombreDeBloqueLibre(String nombre, UUID bloqueId) {
        String buscado = nombre.strip();
        boolean ocupado = bloques.stream()
                .filter(bloque -> !bloque.getId().equals(bloqueId))
                .anyMatch(bloque -> bloque.getNombre().equalsIgnoreCase(buscado));
        if (ocupado) {
            throw new DuplicateResourceException("Ya hay un bloque llamado \"" + buscado + "\" en este proyecto");
        }
    }

    private short siguientePosicionDeBloque() {
        return (short) bloques.stream()
                .mapToInt(bloque -> bloque.getPosicion() + 1)
                .max()
                .orElse(0);
    }
}
