# Laboratorio de decisiones contable

Fecha: 5 de noviembre de 2026  
Rama: `feat/laboratorio-decisiones-contable`

## Objetivo

Convertir los datos reales de un cuaderno en un análisis económico comprensible y en simulaciones que permitan probar
decisiones sin modificar el negocio. La primera versión debe demostrar fundamentos contables, trazabilidad de los
cálculos y explicaciones de Sami; no debe presentarse como contabilidad legal ni como una predicción garantizada.

La promesa para la presentación es:

> Ensambla no solo registra lo que pasó: arma una representación económica del negocio para explicar sus resultados y
> probar decisiones antes de arriesgar dinero real.

## Alcance de la primera versión

### Incluido

1. **Salud del negocio** para un cuaderno y un período:
   - ventas devengadas;
   - costo estimado de la mercadería vendida;
   - resultado bruto y margen bruto;
   - gastos operativos registrados;
   - resultado operativo estimado;
   - ventas fiadas del período;
   - punto de equilibrio y avance de recuperación de gastos;
   - cobertura y nivel de confianza según la completitud de costos y la cantidad de datos.
2. **Laboratorio de decisiones**:
   - meta de resultado mensual;
   - tres caminos calculados: volumen, margen y combinación equilibrada;
   - simulación editable de variación de ventas, margen y gastos;
   - escenarios `si vendo menos`, `ritmo esperado` y `camino elegido`;
   - análisis de sensibilidad: cuánto pueden caer las ventas antes de dejar de cumplir la meta.
3. **Sami como traductor contable**:
   - explicaciones deterministas basadas en los mismos importes de la respuesta;
   - fundamento y supuestos visibles;
   - advertencias sobre datos faltantes;
   - preguntas sugeridas que abren la explicación correspondiente.
4. **Interfaz responsive y accesible**:
   - ruta privada propia;
   - acceso desde la navegación principal;
   - estados de carga, error, falta de datos y datos parciales;
   - valores monetarios y porcentajes con formato local.

### Excluido

- Libro diario persistente, plan de cuentas y asientos de partida doble.
- Balance general, impuestos, amortizaciones y ajustes por inflación.
- Predicción estadística o aprendizaje automático.
- Integraciones bancarias y conciliación.
- Flujo de caja real, porque todavía no existe un circuito completo de cobros y pagos.
- Aplicación automática de cambios sobre precios, gastos o registros.
- Uso de una IA externa para inventar recomendaciones o importes.

Estas exclusiones son deliberadas: el primer corte ofrece un estado de resultados de gestión y un simulador económico
con datos comprobables. El libro diario y el flujo de caja requieren eventos contables persistentes y serán una etapa
posterior.

## Fundamento contable

La pantalla usa estas relaciones:

```text
Ventas devengadas
- Costo estimado de la mercadería vendida
= Resultado bruto

Resultado bruto
- Gastos operativos registrados
= Resultado operativo estimado

Margen bruto % = Resultado bruto / Ventas
Punto de equilibrio = Gastos operativos / Margen bruto %
Avance de recuperación = Ventas / Punto de equilibrio
```

`Ventas devengadas` incluye una venta cuando se registra, aunque sea fiada. Por eso el resultado no equivale a dinero
disponible. La interfaz debe mostrar esta diferencia explícitamente y nunca llamar `ingresos de caja` a las ventas.

El costo es estimado con el costo actual del producto, porque el sistema no conserva todavía el costo histórico por
unidad vendida. Esta limitación aparece junto al cálculo. Si faltan costos, se informa qué proporción de las ventas está
cubierta; no se completa con cero ni se presenta un resultado exacto.

Para el punto de equilibrio, la primera versión trata los gastos registrados como gastos operativos del período. No
afirma que todos sean fijos. La explicación indica que el cálculo es una referencia y mejora cuando el negocio registra
y clasifica más información.

## Datos y cálculos

Se reutiliza la lectura que ya realiza `ResumenNegocio`: inventarios, ventas, gastos, clientes y fiados aislados por
negocio y cuaderno. Se extraerá una estructura contable pura para que tanto el PDF como el Laboratorio usen los mismos
datos sin duplicar criterios.

La respuesta base debe contener valores numéricos y una lista de advertencias; los textos largos de Sami se componen en
el dominio a partir de esos valores. El frontend no recalcula contabilidad: solo envía supuestos de simulación y presenta
la respuesta.

### Confianza

El nivel no representa probabilidad estadística:

- `ALTA`: período con ventas, gastos y costo informado para al menos 90 % del importe vendido.
- `MEDIA`: costo informado para 60 % a menos de 90 %, o menos de 30 ventas.
- `BAJA`: costo informado para menos de 60 %, no hay gastos utilizables o hay menos de 7 ventas.

La API devuelve además los motivos concretos. La interfaz siempre muestra esos motivos al lado del nivel.

### Simulación

Entrada:

- meta de resultado, opcional;
- variación porcentual de ventas, entre -50 % y +100 %;
- variación del margen en puntos porcentuales, limitada para conservar un margen entre 0 % y 95 %;
- variación absoluta de gastos, sin permitir gastos proyectados negativos.

La simulación parte de la base del período:

```text
ventas proyectadas = ventas base × (1 + variación de ventas)
resultado bruto proyectado = ventas proyectadas × margen proyectado
gastos proyectados = máximo(0, gastos base + variación de gastos)
resultado proyectado = resultado bruto proyectado - gastos proyectados
```

Los caminos hacia una meta se calculan sin optimización opaca:

- **Volumen:** mantiene margen y gastos; calcula las ventas necesarias.
- **Margen:** mantiene ventas y gastos; calcula el margen necesario.
- **Equilibrado:** distribuye la brecha entre aumento de ventas, mejora del margen y reducción de gastos dentro de topes
  conservadores. Si la meta no es alcanzable dentro de esos topes, se informa en vez de forzar un resultado.

## API

Área nueva de dominio: `decision`.

```text
GET  /api/proyectos/{proyectoId}/laboratorio?desde=AAAA-MM-DD&hasta=AAAA-MM-DD
POST /api/proyectos/{proyectoId}/laboratorio/simulaciones
```

El `NegocioId` se obtiene de la sesión. Un cuaderno inexistente o ajeno responde 404. La simulación no persiste ni
modifica registros.

Casos de uso:

- `ObtenerLaboratorio`: carga los datos del cuaderno, arma la base contable y devuelve salud, explicaciones y caminos.
- `SimularDecision`: carga la misma base, valida los supuestos y devuelve los escenarios comparados.

La lógica matemática vive en modelos puros del área `decision`. La lectura de registros se comparte mediante los
puertos y estructuras existentes; ningún caso de uso llama a otro. Los beans se registran manualmente.

## Frontend

Ruta propuesta:

```text
/laboratorio
```

El usuario elige el cuaderno y período dentro de la pantalla. Si solo tiene uno, se selecciona automáticamente.

Flujo:

1. **Portada:** elegir `Quiero alcanzar una meta`, `Quiero probar una decisión` o `Quiero anticipar problemas`.
2. **Salud contable:** estado de resultados explicado, punto de equilibrio, cobertura y advertencias.
3. **Meta:** importe deseado y comparación de los tres caminos.
4. **Simulación:** controles de ventas, margen y gastos con resultados calculados por la API.
5. **Sami explica:** panel contextual que muestra fundamento, supuesto, dato utilizado y limitación.

La primera versión implementa las pantallas 1 a 4. `Anticipar problemas` muestra alertas derivadas solo de datos
disponibles: peso de ventas fiadas, margen insuficiente, datos de costos incompletos y punto de equilibrio no alcanzado.
No anuncia fechas de falta de caja.

## Errores y estados parciales

- Sin cuadernos: invitar a crear uno.
- Sin ventas: explicar que se necesitan ventas registradas para calcular el resultado.
- Sin gastos: mostrar resultado bruto y ocultar punto de equilibrio/resultado operativo.
- Costos incompletos: mostrar cobertura y evitar una falsa precisión.
- Margen nulo o negativo: no calcular punto de equilibrio y explicar la causa.
- Supuestos inválidos: 400 con errores por campo.
- Error de red: conservar los supuestos escritos y ofrecer reintentar.

## Pruebas

Backend:

- fórmulas y redondeos en casos normales y límites;
- costos/gastos ausentes y cobertura parcial;
- ventas fiadas sin tratarlas como caja;
- meta alcanzable y no alcanzable;
- aislamiento por negocio;
- validación de porcentajes y gastos proyectados;
- mappers, controller, configuración y arquitectura;
- `clean verify` con cobertura requerida.

Frontend:

- render de estado completo, parcial, vacío y error;
- navegación del flujo;
- envío y conservación de supuestos;
- explicaciones de Sami asociadas a los importes correctos;
- accesibilidad de pestañas, controles y mensajes;
- lint, tipos, tests y build.

## División del trabajo

1. **Backend contable:** base económica, confianza, endpoint de consulta y pruebas.
2. **Backend simulador:** metas, caminos, escenarios, validaciones y pruebas.
3. **Frontend:** navegación, estado de resultados, metas, simulador y estados parciales.
4. **Integración y demo:** contratos, datos preparados, pruebas de extremo a extremo y guion.

## Criterio de aceptación de la demo

Con un cuaderno preparado, la persona puede abrir el Laboratorio, comprender cómo se forma el resultado, identificar
que una venta fiada no equivale a caja, fijar una meta, comparar caminos y cambiar un supuesto. Cada cifra importante
permite ver su fórmula, datos utilizados y limitaciones. Ninguna simulación altera los registros reales.
