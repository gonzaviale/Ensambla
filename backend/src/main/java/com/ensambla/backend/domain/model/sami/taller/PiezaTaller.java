package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una pieza del sistema que se está armando en el taller (inventario, ventas, clientes…).
 *
 * @param id        identificador que le puso el taller; las piezas que se suman en el mismo turno todavía no lo tienen
 * @param tipo      tipo del catálogo de componentes ({@link CatalogoComponentes})
 * @param seccion   nombre de la sección donde está
 * @param registros los registros reales de la pieza, también los ya guardados (sin las filas de ejemplo), para
 *                  poder corregirlos
 * @param nombres   si otra pieza se vincula con esta (clientes, productos), los nombres que ya tiene, también los
 *                  guardados: Sami los usa tal cual en vez de inventar otro
 */
public record PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos,
                          List<RegistroTaller> registros, List<String> nombres) {

    public PiezaTaller {
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
        registros = List.copyOf(Objects.requireNonNullElse(registros, List.of()));
        nombres = List.copyOf(Objects.requireNonNullElse(nombres, List.of()));
    }

    /** Una pieza sin nombres existentes. */
    public PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos,
                       List<RegistroTaller> registros) {
        this(id, tipo, nombre, seccion, campos, registros, List.of());
    }

    /** Una pieza sin registros ni nombres existentes. */
    public PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos) {
        this(id, tipo, nombre, seccion, campos, List.of());
    }

    public ListaDeCampos listaDeCampos() {
        return new ListaDeCampos(campos);
    }

    public boolean sinCampos() {
        return campos.isEmpty();
    }

    public boolean tieneLugarParaOtroCampo() {
        return campos.size() < CatalogoTaller.MAX_CAMPOS_POR_PIEZA;
    }

    /** Sin importar mayúsculas, tildes ni plural: «Quien me debe» encuentra a «Quién me debe». */
    public boolean seLlama(String buscado) {
        return TextoNormalizado.clave(Objects.requireNonNullElse(nombre, "")).equals(TextoNormalizado.clave(buscado));
    }

    /** Cómo nombran las acciones a la pieza: por su id o, si se sumó en este turno, por su tipo (o su nombre si es a medida). */
    public String referencia() {
        if (id != null) {
            return id;
        }
        return CatalogoComponentes.TIPO_A_MEDIDA.equals(tipo) ? nombre : tipo;
    }

    public Optional<RegistroTaller> registroDe(String idBuscado) {
        return registros.stream().filter(registro -> idBuscado.equals(registro.id())).findFirst();
    }

    public List<RegistroTaller> registrosSin(List<String> ids) {
        return registros.stream().filter(registro -> !ids.contains(registro.id())).toList();
    }

    public PiezaTaller conNombre(String nuevo) {
        return new PiezaTaller(id, tipo, nuevo, seccion, campos, registros, nombres);
    }

    public PiezaTaller conSeccion(String nueva) {
        return new PiezaTaller(id, tipo, nombre, nueva, campos, registros, nombres);
    }

    public PiezaTaller conCampos(List<CampoTaller> nuevos) {
        return new PiezaTaller(id, tipo, nombre, seccion, nuevos, registros, nombres);
    }

    public PiezaTaller conCampo(CampoTaller campo) {
        return conCampos(listaDeCampos().con(campo));
    }

    public PiezaTaller conRegistros(List<RegistroTaller> nuevos) {
        return new PiezaTaller(id, tipo, nombre, seccion, campos, nuevos, nombres);
    }
}
