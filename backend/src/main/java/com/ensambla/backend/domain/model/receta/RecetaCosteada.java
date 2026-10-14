package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La receta con el costo calculado: cada línea vale cantidad × precio actual del insumo y el costo es la suma. Si a un
 * insumo le falta el precio (o ya no existe) su línea queda sin valor, no suma y la receta se marca incompleta.
 */
public record RecetaCosteada(
        UUID bloqueInsumosId,
        UUID campoPrecioId,
        List<Linea> lineas,
        BigDecimal costo,
        boolean incompleta) {

    public static final String INSUMO_ELIMINADO = "Insumo eliminado";

    /** {@code precioUnitario} y {@code subtotal} son nulos si no se pudo calcular. */
    public record Linea(
            UUID insumoId,
            String insumo,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }

    public static RecetaCosteada vacia() {
        return new RecetaCosteada(null, null, List.of(), BigDecimal.ZERO, false);
    }

    /**
     * @param bloqueInsumosId la tabla de los insumos: la de la columna del precio
     * @param insumos las filas de los insumos que todavía existen
     * @param camposDeInsumos los campos de la tabla de los insumos: de ahí sale el nombre de cada insumo
     */
    public static RecetaCosteada de(Receta receta, UUID bloqueInsumosId, List<Registro> insumos,
                                    List<Campo> camposDeInsumos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(camposDeInsumos);
        Map<UUID, Registro> porId = insumos.stream().collect(Collectors.toMap(Registro::getId, Function.identity()));
        String clavePrecio = receta.getCampoPrecioId().toString();
        List<Linea> lineas = receta.getLineas().stream()
                .map(linea -> costear(linea, porId.get(linea.insumoId()), clavePrecio, etiquetador))
                .toList();
        BigDecimal costo = lineas.stream()
                .map(Linea::subtotal)
                .filter(subtotal -> subtotal != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean incompleta = lineas.stream().anyMatch(linea -> linea.subtotal() == null);
        return new RecetaCosteada(bloqueInsumosId, receta.getCampoPrecioId(), lineas, costo, incompleta);
    }

    private static Linea costear(LineaReceta linea, Registro insumo, String clavePrecio,
                                 Function<Registro, Referencia> etiquetador) {
        if (insumo == null) {
            return new Linea(linea.insumoId(), INSUMO_ELIMINADO, linea.cantidad(), linea.unidad(), null, null);
        }
        String nombre = etiquetador.apply(insumo).etiqueta();
        if (!(insumo.getValores().get(clavePrecio) instanceof Number numero)) {
            return new Linea(linea.insumoId(), nombre, linea.cantidad(), linea.unidad(), null, null);
        }
        BigDecimal precio = new BigDecimal(numero.toString());
        return new Linea(
                linea.insumoId(), nombre, linea.cantidad(), linea.unidad(), precio, linea.cantidad().multiply(precio));
    }
}
