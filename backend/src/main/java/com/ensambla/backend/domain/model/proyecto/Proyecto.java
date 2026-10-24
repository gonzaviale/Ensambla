package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proyecto {

    private static final Set<String> TIPOS_NO_SUGERIBLES = Set.of("RELACION", "FORMULA");
    private static final String TIPO_NO_SUGERIBLE =
            "Sami sólo agrega columnas de texto, número, fecha, opción o sí/no";

    private UUID id;

    private UUID negocioId;

    private String nombre;

    private String descripcion;

    /** Plantilla de origen; null si se creó en blanco o si la plantilla ya no existe. */
    private UUID plantillaId;

    /** Plantilla de sistema del frontend (almacén, depósito, pedidos y clientes); null si se armó a medida. */
    private PlantillaSistema plantillaClave;

    @Builder.Default
    private DistribucionProyecto distribucion = DistribucionProyecto.MOSAICO;

    @Builder.Default
    private Boolean conMovimientos = false;

    @Builder.Default
    private Boolean conGastos = false;

    /** El almacén de ejemplo que arma el panel de avisos. */
    @Builder.Default
    private Boolean esDemo = false;

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

    /**
     * Suma al primer bloque las columnas que Sami propuso y la persona aceptó. Cada una se valida como si se creara
     * a mano; si alguna falla se lanza el error y el proyecto no se guarda (todo se guarda en una sola transacción).
     * Sólo admite tipos que se configuran con un nombre: no conexiones ni fórmulas.
     */
    public void agregarColumnas(List<ConfiguracionCampo> columnas, OffsetDateTime ahora) {
        if (columnas.isEmpty()) {
            return;
        }
        if (bloques.isEmpty()) {
            throw new BusinessRuleException("La plantilla no tiene una tabla donde agregar columnas");
        }
        Bloque destino = bloques.getFirst();
        List<Campo> campos = new ArrayList<>(destino.getCampos());
        Set<String> nombres = campos.stream()
                .map(campo -> campo.getNombre().strip().toLowerCase())
                .collect(Collectors.toCollection(HashSet::new));
        for (int i = 0; i < columnas.size(); i++) {
            ConfiguracionCampo.Validada validada = validarColumna(columnas.get(i), i);
            if (!nombres.add(validada.nombre().toLowerCase())) {
                throw new DuplicateResourceException("Ya hay una columna llamada \"" + validada.nombre() + "\"");
            }
            campos.add(Campo.nuevo(negocioId, null, (short) campos.size(), validada, ahora));
        }
        destino.setCampos(campos);
    }

    private ConfiguracionCampo.Validada validarColumna(ConfiguracionCampo columna, int indice) {
        if (TIPOS_NO_SUGERIBLES.contains(columna.tipo())) {
            throw new CampoInvalidoException(Map.of("columnas[" + indice + "].tipo", TIPO_NO_SUGERIBLE));
        }
        try {
            return columna.validar();
        } catch (CampoInvalidoException e) {
            Map<String, String> errores = new LinkedHashMap<>();
            e.getErrores().forEach((dato, error) -> errores.put("columnas[" + indice + "]." + dato, error));
            throw new CampoInvalidoException(errores);
        }
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
