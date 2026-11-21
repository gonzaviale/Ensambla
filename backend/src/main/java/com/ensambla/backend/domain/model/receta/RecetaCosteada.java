package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La receta con el costo calculado: cada línea vale cantidad × precio actual del insumo y el costo es la suma. Si a un
 * insumo le falta el precio (o ya no existe) su línea queda sin valor, no suma y la receta se marca incompleta.
 */
public record RecetaCosteada(
        BloqueId bloqueInsumosId,
        CampoId campoPrecioId,
        List<Linea> lineas,
        BigDecimal costo,
        boolean incompleta) {

    public static final String INSUMO_ELIMINADO = "Insumo eliminado";

    /** {@code precioUnitario} y {@code subtotal} son nulos si no se pudo calcular. */
    public record Linea(
            RegistroId insumoId,
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
    public static RecetaCosteada de(Receta receta, BloqueId bloqueInsumosId, List<Registro> insumos,
                                    List<Campo> camposDeInsumos) {
        List<Linea> lineas = costearLineas(receta, insumos, camposDeInsumos);
        return new RecetaCosteada(bloqueInsumosId, receta.getCampoPrecioId(), lineas,
                sumaDeSubtotales(lineas), hayLineasSinValor(lineas));
    }

    private static List<Linea> costearLineas(Receta receta, List<Registro> insumos, List<Campo> camposDeInsumos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(camposDeInsumos);
        Map<RegistroId, Registro> porId = insumos.stream()
                .collect(Collectors.toMap(Registro::getId, Function.identity()));
        String clavePrecio = receta.getCampoPrecioId().toString();
        return receta.getLineas().stream()
                .map(linea -> costear(linea, porId.get(linea.insumoId()), clavePrecio, etiquetador))
                .toList();
    }

    private static BigDecimal sumaDeSubtotales(List<Linea> lineas) {
        return lineas.stream()
                .map(Linea::subtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean hayLineasSinValor(List<Linea> lineas) {
        return lineas.stream().anyMatch(linea -> linea.subtotal() == null);
    }

    private static Linea costear(LineaReceta linea, Registro insumo, String clavePrecio,
                                 Function<Registro, Referencia> etiquetador) {
        if (insumo == null) {
            return sinValor(linea, INSUMO_ELIMINADO);
        }
        String nombre = etiquetador.apply(insumo).etiqueta();
        if (!(insumo.getValores().get(clavePrecio) instanceof Number numero)) {
            return sinValor(linea, nombre);
        }
        return conValor(linea, nombre, new BigDecimal(numero.toString()));
    }

    private static Linea sinValor(LineaReceta linea, String nombre) {
        return new Linea(linea.insumoId(), nombre, linea.cantidad(), linea.unidad(), null, null);
    }

    private static Linea conValor(LineaReceta linea, String nombre, BigDecimal precio) {
        BigDecimal subtotal = linea.cantidad().multiply(precio);
        return new Linea(linea.insumoId(), nombre, linea.cantidad(), linea.unidad(), precio, subtotal);
    }
}
