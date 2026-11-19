package com.ensambla.backend.domain.model.campo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Expresión aritmética de un campo FORMULA: números, referencias a campos con la forma {@code {id-del-campo}},
 * {@code + - * /}, el menos unario y paréntesis. Se interpreta una sola vez con {@link #parsear(String)} y se evalúa
 * con los valores de un registro.
 */
public final class Formula {

    public static final int MAX_CARACTERES = 500;

    private static final int LARGO_UUID = 36;

    private static final String REFERENCIA_INVALIDA = "Hay una referencia a un campo que no es válida";

    private final Nodo raiz;

    private final Set<CampoId> referencias;

    private Formula(Nodo raiz, Set<CampoId> referencias) {
        this.raiz = raiz;
        this.referencias = referencias;
    }

    /** Resultado de evaluar con los valores ya conocidos; {@code null} si falta un dato o se divide por cero. */
    @FunctionalInterface
    private interface Nodo {
        BigDecimal evaluar(Function<CampoId, BigDecimal> valorDe);
    }

    /**
     * @throws IllegalArgumentException con un mensaje para mostrar a la persona si la expresión no es válida
     */
    public static Formula parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Armá la fórmula");
        }
        if (texto.length() > MAX_CARACTERES) {
            throw new IllegalArgumentException("Máximo " + MAX_CARACTERES + " caracteres");
        }
        Lector lector = new Lector(texto);
        Nodo raiz = lector.expresion();
        lector.saltarEspacios();
        if (!lector.terminado()) {
            throw lector.error("No se entiende la fórmula");
        }
        return new Formula(raiz, lector.referencias);
    }

    /** Ids de los campos que usa la fórmula, en orden de aparición. */
    public Set<CampoId> referencias() {
        return referencias;
    }

    /** Valor de la fórmula; {@code valorDe} devuelve {@code null} si el registro no tiene ese dato. */
    public BigDecimal evaluar(Function<CampoId, BigDecimal> valorDe) {
        BigDecimal resultado = raiz.evaluar(valorDe);
        if (resultado == null) {
            return null;
        }
        BigDecimal limpio = resultado.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    /** Las cuatro operaciones, con el símbolo que las escribe; la división por cero da {@code null}. */
    private enum Operador {

        SUMA('+', BigDecimal::add),
        RESTA('-', BigDecimal::subtract),
        PRODUCTO('*', BigDecimal::multiply),
        DIVISION('/', Formula::dividir);

        static final List<Operador> ADITIVOS = List.of(SUMA, RESTA);
        static final List<Operador> MULTIPLICATIVOS = List.of(PRODUCTO, DIVISION);

        private final char simbolo;

        private final BinaryOperator<BigDecimal> operacion;

        Operador(char simbolo, BinaryOperator<BigDecimal> operacion) {
            this.simbolo = simbolo;
            this.operacion = operacion;
        }

        char simbolo() {
            return simbolo;
        }

        BinaryOperator<BigDecimal> operacion() {
            return operacion;
        }
    }

    private static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        return divisor.signum() == 0 ? null : dividendo.divide(divisor, MathContext.DECIMAL64);
    }

    private static final class Lector {

        private final String texto;

        private final Set<CampoId> referencias = new LinkedHashSet<>();

        private int posicion;

        private Lector(String texto) {
            this.texto = texto;
        }

        private Nodo expresion() {
            return encadenar(this::termino, Operador.ADITIVOS);
        }

        private Nodo termino() {
            return encadenar(this::factor, Operador.MULTIPLICATIVOS);
        }

        /** Operandos unidos por operadores del mismo nivel, de izquierda a derecha: {@code a - b + c}. */
        private Nodo encadenar(Supplier<Nodo> operando, List<Operador> operadores) {
            Nodo izquierda = operando.get();
            Optional<Operador> operador = siguienteOperador(operadores);
            while (operador.isPresent()) {
                izquierda = operar(izquierda, operando.get(), operador.get().operacion());
                operador = siguienteOperador(operadores);
            }
            return izquierda;
        }

        private Optional<Operador> siguienteOperador(List<Operador> operadores) {
            saltarEspacios();
            return operadores.stream().filter(operador -> consumir(operador.simbolo())).findFirst();
        }

        private Nodo factor() {
            saltarEspacios();
            if (terminado()) {
                throw error("La fórmula está incompleta");
            }
            if (consumir('-')) {
                return negado(factor());
            }
            if (consumir('(')) {
                return entreParentesis();
            }
            return texto.charAt(posicion) == '{' ? referencia() : numero();
        }

        private static Nodo negado(Nodo interno) {
            return valorDe -> negar(interno.evaluar(valorDe));
        }

        private Nodo entreParentesis() {
            Nodo interno = expresion();
            saltarEspacios();
            if (!consumir(')')) {
                throw error("Falta cerrar un paréntesis");
            }
            return interno;
        }

        private Nodo referencia() {
            int inicio = posicion + 1;
            int fin = inicio + LARGO_UUID;
            if (fin >= texto.length() || texto.charAt(fin) != '}') {
                throw error(REFERENCIA_INVALIDA);
            }
            CampoId id = campoReferido(texto.substring(inicio, fin));
            posicion = fin + 1;
            referencias.add(id);
            return valorDe -> valorDe.apply(id);
        }

        private CampoId campoReferido(String id) {
            try {
                return CampoId.de(id);
            } catch (IllegalArgumentException e) {
                throw error(REFERENCIA_INVALIDA);
            }
        }
        private Nodo numero() {
            int inicio = posicion;
            while (!terminado() && (Character.isDigit(texto.charAt(posicion)) || texto.charAt(posicion) == '.')) {
                posicion++;
            }
            BigDecimal valor;
            try {
                valor = new BigDecimal(texto.substring(inicio, posicion));
            } catch (NumberFormatException e) {
                throw error("No se entiende la fórmula");
            }
            return valorDe -> valor;
        }

        private static Nodo operar(Nodo izquierda, Nodo derecha,
                                   BinaryOperator<BigDecimal> operacion) {
            return valorDe -> {
                BigDecimal a = izquierda.evaluar(valorDe);
                BigDecimal b = derecha.evaluar(valorDe);
                return a == null || b == null ? null : operacion.apply(a, b);
            };
        }


        private static BigDecimal negar(BigDecimal valor) {
            return valor == null ? null : valor.negate();
        }

        private boolean consumir(char esperado) {
            if (!terminado() && texto.charAt(posicion) == esperado) {
                posicion++;
                return true;
            }
            return false;
        }

        private void saltarEspacios() {
            while (!terminado() && Character.isWhitespace(texto.charAt(posicion))) {
                posicion++;
            }
        }

        private boolean terminado() {
            return posicion >= texto.length();
        }

        private IllegalArgumentException error(String mensaje) {
            return new IllegalArgumentException(mensaje);
        }
    }
}
