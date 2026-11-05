package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un inventario con proveedor para las pruebas de reposición. */
public final class ReposicionDePrueba {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final RegistroId YERBA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final RegistroId FIDEOS_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    public static final RegistroId ACEITE_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    public static final RegistroId GASEOSA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000004");

    private static final CampoId CAMPO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000002");
    private static final CampoId CAMPO_MINIMO_ID = CampoId.de("f0000000-0000-4000-8000-000000000003");
    private static final CampoId CAMPO_PROVEEDOR_ID = CampoId.de("f0000000-0000-4000-8000-000000000004");

    private ReposicionDePrueba() {
    }

    /** Inventario con las columnas nombre, cantidad, mínimo y proveedor y las filas dadas. */
    public static BloqueInventario inventarioCon(Registro... registros) {
        Bloque bloque = Bloque.builder()
                .id(INVENTARIO_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID, "name"), campo(CAMPO_CANTIDAD_ID, "quantity"),
                        campo(CAMPO_MINIMO_ID, "minimum"), campo(CAMPO_PROVEEDOR_ID, "supplier")))
                .build();
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().forEach(campo -> porClave.put(campo.getClave(), campo));
        return new BloqueInventario(bloque, null, porClave, List.of(registros));
    }

    /** Inventario sin la columna de proveedor: un sistema que todavía no la agregó. */
    public static BloqueInventario inventarioSinProveedorCon(Registro... registros) {
        BloqueInventario completo = inventarioCon(registros);
        Map<String, Campo> sinProveedor = new LinkedHashMap<>(completo.camposPorClave());
        sinProveedor.remove("supplier");
        return new BloqueInventario(completo.bloque(), null, sinProveedor, completo.registros());
    }

    public static DatosDeProyecto datosCon(BloqueInventario... inventarios) {
        return new DatosDeProyecto("Almacén de prueba", List.of(inventarios), List.of(), List.of());
    }

    /** Fila del inventario; los valores en {@code null} quedan sin cargar. */
    public static Registro producto(RegistroId id, String nombre, Object cantidad, Object minimo, Object proveedor) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CAMPO_NOMBRE_ID.toString(), nombre);
        valores.put(CAMPO_CANTIDAD_ID.toString(), cantidad);
        valores.put(CAMPO_MINIMO_ID.toString(), minimo);
        valores.put(CAMPO_PROVEEDOR_ID.toString(), proveedor);
        return Registro.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).valores(valores).build();
    }

    private static Campo campo(CampoId id, String clave) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave).build();
    }
}
