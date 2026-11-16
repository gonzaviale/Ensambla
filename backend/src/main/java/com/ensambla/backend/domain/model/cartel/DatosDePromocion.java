package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lo que el sistema sabe de lo que se anuncia: qué plantillas sirven, los productos y los números ya calculados
 * (precios, porcentajes, cantidades, fechas). Es lo único que la IA puede nombrar.
 */
public record DatosDePromocion(List<TipoDeCartel> tiposPosibles, List<String> productos, List<BigDecimal> numeros) {

    public DatosDePromocion {
        exigirAlgunTipo(tiposPosibles);
        productos = productos == null ? List.of() : List.copyOf(productos);
        numeros = numeros == null ? List.of() : List.copyOf(numeros);
    }

    /** Los números que un texto puede escribir: los calculados y los que ya trae el nombre de un producto. */
    public Set<BigDecimal> numerosPermitidos() {
        Set<BigDecimal> permitidos = new HashSet<>();
        numeros.stream().map(NumerosDeTexto::normalizado).forEach(permitidos::add);
        productos.forEach(producto -> permitidos.addAll(NumerosDeTexto.en(producto)));
        return permitidos;
    }

    public String primerProducto() {
        return productos.isEmpty() ? "" : productos.getFirst();
    }

    private static void exigirAlgunTipo(List<TipoDeCartel> tipos) {
        if (tipos == null || tipos.isEmpty()) {
            throw new BusinessRuleException("Hace falta al menos un tipo de cartel posible");
        }
    }
}
