package com.ensambla.backend.infrastructure.ia;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Los proveedores de IA registrados, por nombre. Un nombre desconocido no deja levantar la aplicación. */
public class ProveedoresIa {

    private final Map<String, ProveedorIa> porNombre = new TreeMap<>();

    public ProveedoresIa(List<ProveedorIa> proveedores) {
        proveedores.forEach(proveedor -> porNombre.put(proveedor.nombre(), proveedor));
    }

    /** @throws IllegalStateException si no hay un proveedor con ese nombre */
    public ProveedorIa elegir(String nombre) {
        ProveedorIa proveedor = porNombre.get(nombre);
        if (proveedor == null) {
            throw new IllegalStateException("No hay un proveedor de IA «%s»; los posibles son %s."
                    .formatted(nombre, String.join(", ", porNombre.keySet())));
        }
        return proveedor;
    }
}
