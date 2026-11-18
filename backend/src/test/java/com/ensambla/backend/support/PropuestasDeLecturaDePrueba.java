package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Columnas, filas, lecturas de fotos y bloques del negocio para probar cómo se propone guardar lo leído. */
public final class PropuestasDeLecturaDePrueba {

    private PropuestasDeLecturaDePrueba() {
    }

    public static CampoPropuesto unaColumna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    /** {@code datos}: pares clave, valor. */
    public static FilaLeida unaFila(int renglon, String... datos) {
        Map<String, DatoLeido> mapa = new LinkedHashMap<>();
        IntStream.iterate(0, i -> i < datos.length, i -> i + 2)
                .forEach(i -> mapa.put(datos[i], new DatoLeido(datos[i + 1], 0.9)));
        return new FilaLeida(1, renglon, mapa);
    }

    public static LecturaCuaderno unaLecturaDeVentas(List<CampoPropuesto> columnas, FilaLeida... filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Ventas", null, columnas),
                List.of(filas), List.of("Aviso"), new ConsumoIa("simulado", "demo", "v3", 1, 10, 20, 30));
    }

    /** Un bloque con un solo campo, el de {@link ProcesamientoTestData#CAMPO_ID}. */
    public static BloqueLectura unBloque(String nombre, String campo, TipoCampo tipo, List<String> opciones) {
        return new BloqueLectura(BloqueId.nuevo(), nombre,
                List.of(new CampoConocido(ProcesamientoTestData.CAMPO_ID, campo, tipo, opciones)));
    }

    public static BloqueLectura unBloqueConLosCampos(String nombre, CampoConocido... campos) {
        return new BloqueLectura(BloqueId.nuevo(), nombre, List.of(campos));
    }

    public static CampoConocido unCampoDeTexto(CampoId id, String nombre) {
        return new CampoConocido(id, nombre, TipoCampo.TEXTO, List.of());
    }
}
