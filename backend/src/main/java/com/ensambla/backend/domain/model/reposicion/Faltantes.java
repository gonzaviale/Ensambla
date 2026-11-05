package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Los productos de un sistema que hay que reponer, los más urgentes primero. */
public record Faltantes(List<ItemReposicion> items) {

    private static final Comparator<ItemReposicion> MAS_URGENTE_PRIMERO =
            Comparator.comparingInt(ItemReposicion::urgencia).reversed()
                    .thenComparing(ItemReposicion::nombre, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<GrupoDeProveedor> CON_PROVEEDOR_PRIMERO =
            Comparator.comparing((GrupoDeProveedor grupo) -> grupo.proveedor() == null)
                    .thenComparing(grupo -> Optional.ofNullable(grupo.proveedor()).orElse(""),
                            String.CASE_INSENSITIVE_ORDER);

    public static Faltantes de(DatosDeProyecto datos) {
        return new Faltantes(datos.inventarios().stream()
                .flatMap(Faltantes::itemsDe)
                .sorted(MAS_URGENTE_PRIMERO)
                .toList());
    }

    /** Un grupo por proveedor, ordenados por nombre, y al final el de los productos sin proveedor. */
    public List<GrupoDeProveedor> porProveedor() {
        return items.stream()
                .collect(Collectors.groupingBy(this::proveedorOSinProveedor))
                .entrySet().stream()
                .map(grupo -> new GrupoDeProveedor(grupo.getKey().orElse(null), grupo.getValue()))
                .sorted(CON_PROVEEDOR_PRIMERO)
                .toList();
    }

    /**
     * Los productos elegidos, cada uno con la cantidad que la persona decidió pedir.
     *
     * @throws BusinessRuleException si alguno ya no hace falta reponerlo o la cantidad no es positiva
     */
    public List<ItemReposicion> elegir(Map<RegistroId, BigDecimal> cantidades) {
        return cantidades.entrySet().stream()
                .map(elegido -> itemPara(elegido.getKey()).conCantidad(elegido.getValue()))
                .toList();
    }

    private ItemReposicion itemPara(RegistroId productoId) {
        return items.stream()
                .filter(item -> item.productoId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Uno de los productos ya no hace falta reponerlo"));
    }

    private Optional<String> proveedorOSinProveedor(ItemReposicion item) {
        return Optional.ofNullable(item.proveedor());
    }

    private static Stream<ItemReposicion> itemsDe(BloqueInventario inventario) {
        return inventario.registros().stream()
                .map(registro -> ItemReposicion.de(inventario, registro))
                .flatMap(Optional::stream);
    }
}
