package com.ensambla.backend.domain.model.objetivo;

/** Algo que el monitoreo detectó y deja como estado, sin avisar: la persona lo ve al entrar a la app. */
public enum HitoObjetivo {
    /** Un mini-objetivo llegó a su meta. */
    MINI_LOGRADO,
    /** El valor de un mini-objetivo en curso no mejora desde hace dos semanas. */
    SIN_AVANCE,
    /** El resultado del negocio llegó a la meta del objetivo. */
    OBJETIVO_ALCANZADO
}
