package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.ContextoObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.EleccionIa;
import com.ensambla.backend.domain.model.objetivo.MensajeObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.RolMensaje;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;

/** La conversación para armar un objetivo con Sami: mensajes, catálogo de métricas y respuestas de la IA. */
public final class ConversacionObjetivoTestData {

    public static final String OBJETIVO = "Quiero vender más y que me conozcan en el barrio";
    public static final String RESPUESTA_IA = "Entendí que querés vender más. Lo que vendés lo puedo medir.";
    public static final String ENTENDIDO = "Vender más y darte a conocer";
    public static final String TITULO_ACCION = "Pedile a 3 clientes que te recomienden";
    public static final String JUSTIFICACION_ACCION = "Es la forma más barata de que te conozcan en el barrio.";
    public static final ConsumoIaSami CONSUMO = new ConsumoIaSami("simulado", "simulado", 120, 80);

    private ConversacionObjetivoTestData() {
    }

    public static List<MensajeObjetivo> mensajes() {
        return List.of(new MensajeObjetivo(RolMensaje.PERSONA, OBJETIVO));
    }

    /** c1 (subir el precio de la yerba), c2 (el margen), c3 (las ventas) y c4 (poner a la vista la misma yerba). */
    public static List<CandidatoMiniObjetivo> catalogo() {
        return List.of(
                candidato(),
                candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "5000"),
                candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "9000"),
                new CandidatoMiniObjetivo(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO, VALOR_INICIAL,
                        META_MINI, APORTE, "Poné a la vista la yerba", "Es de las que más dejan.", false));
    }

    public static ContextoObjetivoIa contexto() {
        return new ContextoObjetivoIa(true, new BigDecimal("180000"), new BigDecimal("35.71"),
                new BigDecimal("182000"),
                List.of(new ContextoObjetivoIa.ProductoIa(PRODUCTO, new BigDecimal("4200"), new BigDecimal("40"),
                        new BigDecimal("60"))),
                List.of(new ContextoObjetivoIa.GastoIa("Servicios", new BigDecimal("40000"))),
                List.of(new ContextoObjetivoIa.ProductoZonaIa("Budín", 5, new BigDecimal("1800"),
                        new BigDecimal("2000"), new BigDecimal("2300"))));
    }

    public static ConsultaObjetivoIa consulta() {
        return new ConsultaObjetivoIa(mensajes(), catalogo(), contexto());
    }

    public static EleccionIa medido(String codigo) {
        return new EleccionIa(ModoMiniObjetivo.MEDIDO, codigo, null, null);
    }

    public static EleccionIa manual(String titulo, String justificacion) {
        return new EleccionIa(ModoMiniObjetivo.MANUAL, null, titulo, justificacion);
    }

    public static PropuestaObjetivoIa respuestaIa(EleccionIa... elecciones) {
        return new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, false, List.of(elecciones), CONSUMO);
    }
}
