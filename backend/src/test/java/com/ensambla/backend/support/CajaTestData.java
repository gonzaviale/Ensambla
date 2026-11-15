package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoBuilder;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CajaTestData {

    public static final UsuarioId USUARIO = UsuarioId.nuevo();
    public static final NegocioId NEGOCIO = NegocioId.nuevo();
    public static final ProyectoId PROYECTO = ProyectoId.nuevo();
    public static final BloqueId INVENTARIO = BloqueId.nuevo();
    public static final BloqueId VENTAS = BloqueId.nuevo();
    public static final RegistroId PRODUCTO = RegistroId.nuevo();
    public static final BloqueId CLIENTES = BloqueId.nuevo();
    public static final RegistroId CLIENTE = RegistroId.nuevo();
    public static final VentaCajaId OPERACION = VentaCajaId.nuevo();
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-12T12:00:00Z");

    private CajaTestData() {
    }

    public static Sistema sistema() {
        Bloque inventario = inventario();
        Bloque ventas = ventas();
        Registro producto = producto(inventario);
        return sistemaConProducto(inventario, ventas, producto);
    }

    private static Bloque inventario() {
        Bloque inventario = bloque(INVENTARIO, TipoBloque.INVENTARIO);
        inventario.setNombre("Inventario");
        inventario.setCampos(new ArrayList<>(List.of(
                campo(INVENTARIO, "name", TipoCampo.TEXTO), campo(INVENTARIO, "price", TipoCampo.MONEDA),
                campo(INVENTARIO, "quantity", TipoCampo.NUMERO), campo(INVENTARIO, "saleMode", TipoCampo.TEXTO),
                campo(INVENTARIO, "stockControl", TipoCampo.SI_NO))));
        return inventario;
    }

    private static Bloque ventas() {
        Bloque ventas = bloque(VENTAS, TipoBloque.VENTAS);
        ventas.setNombre("Ventas");
        ventas.setCampos(new ArrayList<>(List.of(campo(VENTAS, "product", TipoCampo.RELACION),
                campo(VENTAS, "price", TipoCampo.MONEDA), campo(VENTAS, "quantity", TipoCampo.NUMERO),
                campo(VENTAS, "date", TipoCampo.FECHA))));
        cambiarCampo(ventas, "product", TipoCampo.RELACION, INVENTARIO);
        return ventas;
    }

    private static Registro producto(Bloque inventario) {
        Registro producto = Registro.nuevo(NEGOCIO, INVENTARIO, new LinkedHashMap<>(), AHORA);
        producto.setId(PRODUCTO);
        poner(inventario, producto, "name", "Pan francés");
        poner(inventario, producto, "price", new BigDecimal("4000"));
        poner(inventario, producto, "quantity", new BigDecimal("2"));
        poner(inventario, producto, "saleMode", "peso");
        poner(inventario, producto, "stockControl", true);
        return producto;
    }

    private static Sistema sistemaConProducto(Bloque inventario, Bloque ventas, Registro producto) {
        return new Sistema(Proyecto.builder().id(PROYECTO).negocioId(NEGOCIO).nombre("Mi negocio")
                .bloques(new ArrayList<>(List.of(inventario, ventas))).creadoEn(AHORA).actualizadoEn(AHORA).build(),
                List.of(), new LinkedHashMap<>(Map.of(INVENTARIO, new ArrayList<>(List.of(producto)),
                        VENTAS, new ArrayList<>())));
    }

    public static CobrarVentaRequest request(String cantidad) {
        return new CobrarVentaRequest(USUARIO, OPERACION, PROYECTO, INVENTARIO, VENTAS,
                List.of(new CobrarVentaRequest.LineaRequest(PRODUCTO, new BigDecimal(cantidad),
                        new BigDecimal("4000"), "PESO")), "EFECTIVO", new BigDecimal("5000"));
    }

    public static void agregarCliente(Sistema sistema) {
        Bloque clientes = clientes();
        Registro cliente = cliente(clientes);
        sistema.proyecto().getBloques().add(1, clientes);
        sistema.registrosPorBloque().put(CLIENTES, new ArrayList<>(List.of(cliente)));
        agregarCamposDeCliente(sistema);
    }

    private static Bloque clientes() {
        Bloque clientes = bloque(CLIENTES, TipoBloque.CLIENTES);
        clientes.setNombre("Clientes");
        clientes.setCampos(new ArrayList<>(List.of(campo(CLIENTES, "name", TipoCampo.TEXTO))));
        return clientes;
    }

    private static Registro cliente(Bloque clientes) {
        Registro cliente = Registro.nuevo(NEGOCIO, CLIENTES, new LinkedHashMap<>(), AHORA);
        cliente.setId(CLIENTE);
        poner(clientes, cliente, "name", "Ana López");
        return cliente;
    }

    private static void agregarCamposDeCliente(Sistema sistema) {
        Bloque ventas = sistema.proyecto().getBloques().stream().filter(bloque -> bloque.getId().equals(VENTAS))
                .findFirst().orElseThrow();
        ventas.getCampos().add(datosDeCampo(VENTAS, "customer", TipoCampo.RELACION)
                .bloqueRelacionadoId(CLIENTES).build());
        ventas.getCampos().add(campo(VENTAS, "credit", TipoCampo.SI_NO));
    }

    public static void poner(Bloque bloque, Registro registro, String clave, Object valor) {
        registro.getValores().put(bloque.getCampos().stream().filter(c -> c.getClave().equals(clave))
                .findFirst().orElseThrow().getId().toString(), valor);
    }

    private static Bloque bloque(BloqueId id, TipoBloque tipo) {
        return Bloque.builder().id(id).negocioId(NEGOCIO).proyectoId(PROYECTO).tipo(tipo)
                .posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build();
    }

    private static Campo campo(BloqueId bloqueId, String clave, TipoCampo tipo) {
        return datosDeCampo(bloqueId, clave, tipo).build();
    }

    private static CampoBuilder datosDeCampo(BloqueId bloqueId, String clave, TipoCampo tipo) {
        return Campo.builder().id(CampoId.nuevo()).negocioId(NEGOCIO).bloqueId(bloqueId).clave(clave)
                .nombre(clave).tipo(tipo).obligatorio(false).opciones(List.of()).posicion((short) 0)
                .creadoEn(AHORA).actualizadoEn(AHORA);
    }

    public static void cambiarCampo(Bloque bloque, String clave, TipoCampo tipo, BloqueId relacionado) {
        Campo anterior = bloque.getCampos().stream().filter(campo -> campo.getClave().equals(clave))
                .findFirst().orElseThrow();
        Campo nuevo = datosDeCampo(bloque.getId(), clave, tipo).id(anterior.getId())
                .bloqueRelacionadoId(relacionado).build();
        bloque.getCampos().set(bloque.getCampos().indexOf(anterior), nuevo);
    }
}
