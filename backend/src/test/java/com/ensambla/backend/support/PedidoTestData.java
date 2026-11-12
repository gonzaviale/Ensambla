package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inventario con precio y stock para las pruebas de los pedidos por WhatsApp. */
public final class PedidoTestData {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final CampoId CAMPO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");
    public static final CampoId CAMPO_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000002");
    public static final CampoId CAMPO_STOCK_ID = CampoId.de("f0000000-0000-4000-8000-000000000003");

    private PedidoTestData() {
    }

    public static ProductoEnStock producto(String nombre, String precio, String stock) {
        return new ProductoEnStock(RegistroId.nuevo(), nombre, new BigDecimal(precio), new BigDecimal(stock));
    }

    public static Bloque bloqueInventario() {
        return Bloque.builder().id(INVENTARIO_ID).negocioId(NEGOCIO_ID).tipo(TipoBloque.INVENTARIO)
                .campos(List.of(campo(CAMPO_NOMBRE_ID, "name"), campo(CAMPO_PRECIO_ID, "price"),
                        campo(CAMPO_STOCK_ID, "quantity"), campo(CampoId.nuevo(), null)))
                .build();
    }

    public static Proyecto proyectoConInventario() {
        return Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Almacén")
                .bloques(List.of(bloqueInventario())).build();
    }

    public static BloqueInventario inventario(Registro... registros) {
        Bloque bloque = bloqueInventario();
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().stream().filter(campo -> campo.getClave() != null)
                .forEach(campo -> porClave.put(campo.getClave(), campo));
        return new BloqueInventario(bloque, null, porClave, List.of(registros));
    }

    /** Una fila del inventario; un valor {@code null} deja el campo sin cargar. */
    public static Registro registro(Object nombre, Object precio, Object stock, boolean ejemplo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, CAMPO_NOMBRE_ID, nombre);
        poner(valores, CAMPO_PRECIO_ID, precio);
        poner(valores, CAMPO_STOCK_ID, stock);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID)
                .valores(valores).esEjemplo(ejemplo).build();
    }

    private static Campo campo(CampoId id, String clave) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave).build();
    }

    private static void poner(Map<String, Object> valores, CampoId campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId.toString(), valor);
        }
    }
}
