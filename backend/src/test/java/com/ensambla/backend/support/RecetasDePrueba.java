package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Recetas, insumos y columnas de precio listos para los tests de recetas y costos. */
public final class RecetasDePrueba {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    public static final RegistroId AZUCAR_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    public static final RegistroId LEVADURA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000004");
    public static final RegistroId BORRADO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000005");
    public static final BloqueId BLOQUE_INSUMOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    public static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    public static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);
    public static final OffsetDateTime AHORA = OffsetDateTime.now(RELOJ);

    private static final BigDecimal DOS = new BigDecimal("2");

    private RecetasDePrueba() {
    }

    public static Registro unProducto() {
        return Registro.builder().id(PRODUCTO_ID).build();
    }

    public static Registro unInsumoSinPrecio(RegistroId id) {
        return Registro.builder().id(id).valores(Map.of()).build();
    }

    public static List<Registro> unosInsumosSinPrecio(RegistroId... ids) {
        return Arrays.stream(ids).map(RecetasDePrueba::unInsumoSinPrecio).toList();
    }

    public static Registro unInsumoConPrecio(RegistroId id, String nombre, Number precio) {
        return unInsumoConPrecioDe(id, nombre, precio);
    }

    public static Registro unInsumoConPrecioQueNoEsUnNumero(RegistroId id, String nombre) {
        return unInsumoConPrecioDe(id, nombre, "no es número");
    }

    private static Registro unInsumoConPrecioDe(RegistroId id, String nombre, Object precio) {
        Map<String, Object> valores = Map.of(NOMBRE_ID.toString(), nombre, PRECIO_ID.toString(), precio);
        return Registro.builder().id(id).valores(valores).build();
    }

    public static Campo unaColumnaDelPrecioDeTipo(TipoCampo tipo) {
        return Campo.builder().id(PRECIO_ID).bloqueId(BLOQUE_INSUMOS_ID).tipo(tipo).build();
    }

    public static List<Campo> losCamposDeLosInsumos() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build(),
                Campo.builder().id(PRECIO_ID).tipo(TipoCampo.NUMERO).posicion((short) 1).build());
    }

    public static LineaReceta unaLinea(RegistroId insumoId, String cantidad, String unidad) {
        return new LineaReceta(insumoId, new BigDecimal(cantidad), unidad);
    }

    public static List<LineaReceta> unasLineasConInsumosDistintos(int cantidad) {
        return IntStream.range(0, cantidad)
                .mapToObj(numero -> new LineaReceta(RegistroId.nuevo(), BigDecimal.ONE, null))
                .toList();
    }

    public static Receta unaRecetaVacia() {
        return Receta.builder().lineas(List.of()).build();
    }

    public static Receta unaRecetaDe(LineaReceta... lineas) {
        return Receta.builder().campoPrecioId(PRECIO_ID).lineas(List.of(lineas)).build();
    }

    public static Receta unaRecetaDelProductoCon(RegistroId... insumos) {
        return Receta.builder()
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(PRECIO_ID)
                .lineas(Arrays.stream(insumos).map(insumo -> new LineaReceta(insumo, DOS, null)).toList())
                .build();
    }

    public static GuardarRecetaRequest unPedidoDeGuardarRecetaCon(RegistroId... insumos) {
        List<LineaRecetaRequest> lineas = Arrays.stream(insumos)
                .map(insumo -> new LineaRecetaRequest(insumo, DOS, "kg"))
                .toList();
        return new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, PRECIO_ID, lineas);
    }

    public static GuardarRecetaRequest unPedidoDeQuitarLaReceta() {
        return new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of());
    }

    public static GuardarRecetaRequest unPedidoSinColumnaDelPrecio() {
        List<LineaRecetaRequest> lineas = List.of(new LineaRecetaRequest(HARINA_ID, BigDecimal.ONE, null));
        return new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, lineas);
    }

    public static RecetaCosteada unaRecetaCosteadaDeHarina() {
        RecetaCosteada.Linea harina = new RecetaCosteada.Linea(
                HARINA_ID, "Harina", new BigDecimal("3"), "kg", BigDecimal.TEN, new BigDecimal("30"));
        return new RecetaCosteada(BLOQUE_INSUMOS_ID, PRECIO_ID, List.of(harina), new BigDecimal("30"), false);
    }
}
