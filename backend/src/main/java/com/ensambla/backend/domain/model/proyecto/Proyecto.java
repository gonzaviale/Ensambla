package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
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

    private ProyectoId id;

    private NegocioId negocioId;

    private String nombre;

    private String descripcion;

    /** Plantilla de origen; null si se creó en blanco o si la plantilla ya no existe. */
    private PlantillaId plantillaId;

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

    public static Proyecto enBlanco(NegocioId negocioId, String nombre, String descripcion, OffsetDateTime ahora) {
        return Proyecto.builder()
                .negocioId(negocioId)
                .nombre(nombre.strip())
                .descripcion(descripcion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public static Proyecto desdePlantilla(NegocioId negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        Proyecto proyecto = enBlanco(negocioId, plantilla.nombre(), plantilla.descripcion(), ahora);
        proyecto.setPlantillaId(plantilla.id());
        proyecto.setBloques(bloquesDe(plantilla, negocioId, ahora));
        return proyecto;
    }

    private static List<Bloque> bloquesDe(PlantillaProyecto plantilla, NegocioId negocioId, OffsetDateTime ahora) {
        List<Bloque> bloques = new ArrayList<>();
        for (BloquePlantilla definicion : plantilla.estructura().bloques()) {
            bloques.add(bloqueDe(definicion, negocioId, bloques.size(), ahora));
        }
        return bloques;
    }

    private static Bloque bloqueDe(
            BloquePlantilla definicion, NegocioId negocioId, int posicion, OffsetDateTime ahora) {
        Bloque bloque = Bloque.builder()
                .negocioId(negocioId)
                .nombre(definicion.nombre())
                .posicion((short) posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
        bloque.setCampos(camposDe(definicion, negocioId, ahora));
        return bloque;
    }

    private static List<Campo> camposDe(BloquePlantilla definicion, NegocioId negocioId, OffsetDateTime ahora) {
        List<Campo> campos = new ArrayList<>();
        for (CampoPlantilla campo : definicion.campos()) {
            campos.add(campoDe(campo, negocioId, campos.size(), ahora));
        }
        return campos;
    }

    private static Campo campoDe(CampoPlantilla campo, NegocioId negocioId, int posicion, OffsetDateTime ahora) {
        return Campo.builder()
                .negocioId(negocioId)
                .nombre(campo.nombre())
                .tipo(campo.tipo())
                .obligatorio(campo.obligatorio())
                .valorInicial(campo.valorInicial())
                .opciones(campo.opciones())
                .posicion((short) posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
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
        Bloque destino = primerBloqueParaColumnas();
        destino.setCampos(camposConLasColumnas(destino, columnas, ahora));
    }

    private Bloque primerBloqueParaColumnas() {
        if (bloques.isEmpty()) {
            throw new BusinessRuleException("La plantilla no tiene una tabla donde agregar columnas");
        }
        return bloques.getFirst();
    }

    private List<Campo> camposConLasColumnas(Bloque destino, List<ConfiguracionCampo> columnas, OffsetDateTime ahora) {
        List<Campo> campos = new ArrayList<>(destino.getCampos());
        Set<String> nombres = nombresEnMinuscula(campos);
        for (int indice = 0; indice < columnas.size(); indice++) {
            Campo columna = validarColumna(columnas.get(indice), indice);
            exigirNombreLibre(nombres, columna);
            campos.add(columna.ubicadoEn(negocioId, null, (short) campos.size(), ahora));
        }
        return campos;
    }

    private Set<String> nombresEnMinuscula(List<Campo> campos) {
        return campos.stream()
                .map(campo -> campo.getNombre().strip().toLowerCase())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void exigirNombreLibre(Set<String> nombres, Campo columna) {
        if (!nombres.add(columna.getNombre().toLowerCase())) {
            throw new DuplicateResourceException("Ya hay una columna llamada \"" + columna.getNombre() + "\"");
        }
    }

    private Campo validarColumna(ConfiguracionCampo columna, int indice) {
        exigirTipoSugerible(columna, indice);
        try {
            return columna.validar();
        } catch (CampoInvalidoException e) {
            throw conPosicion(e, indice);
        }
    }

    private void exigirTipoSugerible(ConfiguracionCampo columna, int indice) {
        if (TIPOS_NO_SUGERIBLES.contains(columna.tipo())) {
            throw new CampoInvalidoException(Map.of("columnas[" + indice + "].tipo", TIPO_NO_SUGERIBLE));
        }
    }

    private CampoInvalidoException conPosicion(CampoInvalidoException error, int indice) {
        Map<String, String> errores = new LinkedHashMap<>();
        error.getErrores().forEach((dato, mensaje) -> errores.put("columnas[" + indice + "]." + dato, mensaje));
        return new CampoInvalidoException(errores);
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
    public void validarNombreDeBloqueLibre(String nombre, BloqueId bloqueId) {
        String buscado = nombre.strip();
        if (hayOtroBloqueLlamado(buscado, bloqueId)) {
            throw new DuplicateResourceException("Ya hay un bloque llamado \"" + buscado + "\" en este proyecto");
        }
    }

    private boolean hayOtroBloqueLlamado(String nombre, BloqueId bloqueId) {
        return bloques.stream()
                .filter(bloque -> !bloque.getId().equals(bloqueId))
                .anyMatch(bloque -> bloque.getNombre().equalsIgnoreCase(nombre));
    }

    private short siguientePosicionDeBloque() {
        return (short) bloques.stream()
                .mapToInt(bloque -> bloque.getPosicion() + 1)
                .max()
                .orElse(0);
    }
}
