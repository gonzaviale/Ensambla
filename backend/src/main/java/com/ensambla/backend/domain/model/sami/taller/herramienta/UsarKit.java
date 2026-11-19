package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.ConexionKit;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.TurnoDelTaller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Suma las piezas del kit que el sistema todavía no tiene (un tipo que no se repite y ya está, o una tabla a medida
 * con el mismo nombre, se saltean) y después sus conexiones, sin pasarse del lugar que queda en el turno. Si sumó algo,
 * guarda la pregunta del kit. {@code usar_kit} no llega nunca al taller: se expande en {@code agregar_pieza} y
 * {@code conectar}.
 */
public final class UsarKit extends HerramientaQueActua {

    private static final String TIPO_CLIENTES = "clients";
    /** En ventas el dato Cliente es el quinto; en pedidos ya es el segundo. */
    private static final List<String> TIPOS_CON_CLIENTE = List.of("sales");
    /** Etiqueta del dato cliente de ventas y pedidos en el catálogo de componentes. */
    private static final String CAMPO_CLIENTE = "Cliente";

    private final KitsRubro kits;
    private final AgregarPieza agregarPieza;
    private final Conectar conectar;
    private final EditarCampo editarCampo;

    UsarKit(KitsRubro kits, AgregarPieza agregarPieza, Conectar conectar, EditarCampo editarCampo) {
        super(HerramientaTaller.USAR_KIT);
        this.kits = kits;
        this.agregarPieza = agregarPieza;
        this.conectar = conectar;
        this.editarCampo = editarCampo;
    }

    @Override
    protected List<AccionTaller> accionesDe(ArgumentosDeHerramienta argumentos, TurnoDelTaller turno) {
        List<AccionTaller> acciones = new ArrayList<>();
        int lugar = turno.lugar();
        argumentos.de("rubro").clave().flatMap(kits::buscar)
                .ifPresent(kit -> sumarKit(kit, acciones, lugar, turno));
        return acciones;
    }

    private void sumarKit(KitRubro kit, List<AccionTaller> acciones, int lugar, TurnoDelTaller turno) {
        kit.piezas().forEach(pieza -> sumarSiHayLugar(acciones, lugar,
                () -> agregarPieza.aplicar(argumentos(pieza.comoArgumentos()), turno.piezas())));
        kit.conexiones().forEach(conexion -> sumarSiHayLugar(acciones, lugar,
                () -> conectar.aplicar(argumentos(argumentosDe(kit, conexion)), turno.piezas())));
        clienteALaVista(kit).forEach(edicion -> sumarSiHayLugar(acciones, lugar,
                () -> editarCampo.aplicar(argumentos(edicion), turno.piezas())));
        if (!acciones.isEmpty()) {
            turno.guardarPreguntaDelKit(kit.pregunta());
        }
    }

    /**
     * Si el kit trae clientes, en sus ventas el dato Cliente va al lugar 2: las tablas muestran sólo los primeros
     * datos, y lo que más se busca es qué se le vendió a quién.
     */
    private List<Map<String, Object>> clienteALaVista(KitRubro kit) {
        boolean conClientes = kit.piezas().stream().anyMatch(pieza -> TIPO_CLIENTES.equals(pieza.tipo()));
        if (!conClientes) {
            return List.of();
        }
        return kit.piezas().stream()
                .map(PiezaKit::tipo)
                .filter(TIPOS_CON_CLIENTE::contains)
                .<Map<String, Object>>map(tipo -> Map.of("pieza", tipo, "campo", CAMPO_CLIENTE, "posicion", 2))
                .toList();
    }

    private void sumarSiHayLugar(List<AccionTaller> acciones, int lugar, Supplier<Optional<AccionTaller>> accion) {
        if (acciones.size() < lugar) {
            accion.get().ifPresent(acciones::add);
        }
    }

    private Map<String, Object> argumentosDe(KitRubro kit, ConexionKit conexion) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("desde", kit.referenciaDe(conexion.desde()));
        argumentos.put("hacia", kit.referenciaDe(conexion.hacia()));
        argumentos.put("etiqueta", conexion.etiqueta());
        return argumentos;
    }

    private ArgumentosDeHerramienta argumentos(Map<String, Object> valores) {
        return new ArgumentosDeHerramienta(valores);
    }
}
