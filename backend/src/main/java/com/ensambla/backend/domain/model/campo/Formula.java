package com.ensambla.backend.domain.model.campo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Expresión aritmética de un campo FORMULA: números, referencias a campos con la forma {@code {id-del-campo}},
 * {@code + - * /}, el menos unario y paréntesis. Se interpreta una sola vez con {@link #parsear(String)} y se evalúa
 * con los valores de un registro.
 */
public final class Formula {

    public static final int MAX_CARACTERES = 500;

    private static final int LARGO_UUID = 36;

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

    private static final class Lector {

        private final String texto;

        private final Set<CampoId> referencias = new LinkedHashSet<>();

        private int posicion;

        private Lector(String texto) {
            this.texto = texto;
        }

        private Nodo expresion() {
            Nodo izquierda = termino();
            while (true) {
                saltarEspacios();
                if (consumir('+')) {
                    izquierda = operar(izquierda, termino(), BigDecimal::add);
                } else if (consumir('-')) {
                    izquierda = operar(izquierda, termino(), BigDecimal::subtract);
                } else {
                    return izquierda;
                }
            }
        }

        private Nodo termino() {
            Nodo izquierda = factor();
            while (true) {
                saltarEspacios();
                if (consumir('*')) {
                    izquierda = operar(izquierda, factor(), BigDecimal::multiply);
                } else if (consumir('/')) {
                    izquierda = operar(izquierda, factor(), Lector::dividir);
                } else {
                    return izquierda;
                }
            }
        }

        private Nodo factor() {
            saltarEspacios();
            if (terminado()) {
                throw error("La fórmula está incompleta");
            }
            char actual = texto.charAt(posicion);
            if (consumir('-')) {
                Nodo interno = factor();
                return valorDe -> negar(interno.evaluar(valorDe));
            }
            if (consumir('(')) {
                Nodo interno = expresion();
                saltarEspacios();
                if (!consumir(')')) {
                    throw error("Falta cerrar un paréntesis");
                }
                return interno;
            }
            if (actual == '{') {
                return referencia();
            }
            return numero();
        }

        private Nodo referencia() {
            int inicio = posicion + 1;
            int fin = inicio + LARGO_UUID;
            if (fin >= texto.length() || texto.charAt(fin) != '}') {
                throw error("Hay una referencia a un campo que no es válida");
            }
            CampoId id;
            try {
                id = CampoId.de(texto.substring(inicio, fin));
            } catch (IllegalArgumentException e) {
                throw error("Hay una referencia a un campo que no es válida");
            }
            posicion = fin + 1;
            referencias.add(id);
            return valorDe -> valorDe.apply(id);
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

        private static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
            return divisor.signum() == 0 ? null : dividendo.divide(divisor, MathContext.DECIMAL64);
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
