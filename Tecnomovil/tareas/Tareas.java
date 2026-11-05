package tareas;

import registro.Registro;

// ───────────────────────── LIBRERÍAS USADAS ─────────────────────────
// Duration (java.time): una cantidad de tiempo (por ejemplo 20 min 31 s).
//   Duration.between(inicio, fin) calcula el tiempo entre dos LocalDateTime.
import java.time.Duration;
// Collections (java.util): utilidades para colecciones. Se usa unmodifiableMap(mapa), que
//   envuelve un mapa para que NO se pueda modificar (put/remove lanzan excepción).
import java.util.Collections;
// Comparator (java.util): define cómo comparar dos elementos para ordenarlos.
//   Comparator.comparing(Registro::timestamp) ordena por fecha/hora.
import java.util.Comparator;
// LinkedHashMap (java.util): un Map que conserva el ORDEN en que se insertaron las claves.
//   El HashMap normal no garantiza orden; para un ranking (mayor a menor) hace falta este.
import java.util.LinkedHashMap;
// List (java.util): lista ordenada de elementos.
import java.util.List;
// Map (java.util): pares clave -> valor. Map.Entry es un par (una clave y su valor).
import java.util.Map;
// Collectors (java.util.stream): "recolectores" que dicen cómo juntar el resultado final de un
//   stream: agrupar (groupingBy), contar (counting), promediar (averagingLong), etc.
import java.util.stream.Collectors;
// IntStream (java.util.stream): stream de enteros (0, 1, 2...). Se usa para recorrer posiciones.
import java.util.stream.IntStream;
// Stream (java.util.stream): flujo de elementos sobre el que se encadenan operaciones.
import java.util.stream.Stream;

/**
 * Las seis tareas de análisis del caso de estudio (sección 3.2 del enunciado).
 *
 * ═══════════════ ¿SE CUMPLE LO QUE PIDE EL ENUNCIADO? ═══════════════
 *
 * TAREAS
 *   a) Afluencia por estación ........ CUMPLE  -> calculoAfluencia
 *   b) Horas pico .................... CUMPLE  -> horasPico
 *   c) Rutas más utilizadas .......... CUMPLE  -> rutas
 *   d) Patrones de viaje por usuario . CUMPLE  -> patronesViaje
 *   e) Tiempo promedio entre estaciones CUMPLE -> calculoPromedioEstaciones (ver nota)
 *   f) Rutas con sobrecarga (crítica)  CUMPLE  -> sobrecargaRutas
 *
 * CONSIDERACIONES TÉCNICAS
 *   - Funciones puras ................ CUMPLE: cada método usa solo sus parámetros y devuelve un
 *                                      resultado; no lee ni escribe nada externo.
 *   - No modificar listas originales . CUMPLE: solo se leen; los ordenamientos se hacen sobre
 *                                      copias y los resultados son estructuras nuevas.
 *   - Lambdas y pipelines con Streams  CUMPLE: todos los métodos son pipelines con lambdas o
 *                                      referencias a método.
 *   - Enfoque declarativo ............ CUMPLE: se describe QUÉ se quiere (filtrar, agrupar,
 *                                      contar) y no CÓMO recorrer (sin for ni contadores).
 *   - Permitir parallelStream() ...... CUMPLE: se usa parallelStream y da el mismo resultado
 *                                      que con un solo hilo (se comprobó al probarlo).
 *   - Evitar efectos secundarios ..... CUMPLE: no imprimen ni cambian variables compartidas.
 *                                      La impresión está solo en Main.
 *   - Colecciones inmutables ......... CUMPLE: todos los resultados son de solo lectura.
 *
 * MATICES (decisiones de interpretación, conviene mencionarlas en el informe)
 *   1. La acción se guarda como enum (ENTRADA/SALIDA) y no como texto "entrada"/"salida".
 *   2. Tarea a: cuenta ENTRADAS. Si un mismo usuario ingresa dos veces a una estación se cuenta
 *      dos veces (el enunciado dice "cuántos usuarios ingresan"; no pide usuarios distintos).
 *   3. Tareas b, c y f cuentan todos los registros (entradas y salidas) de la hora o la ruta.
 *   4. Tarea e: en los datos simulados cada viaje tiene solo una entrada y una salida, así que
 *      "estaciones consecutivas" se interpreta como la estación de entrada y la de salida de
 *      un mismo viaje. No se mide el tiempo entre viajes distintos.
 *   5. Tarea f: devuelve solo las rutas críticas (con su ocupación), no todas con una etiqueta.
 *      El umbral es un parámetro (simulado), lo elige quien llama (Main).
 *
 * LO QUE NO ES CÓDIGO Y TAMBIÉN SE PIDE: subir el proyecto a GitHub en la rama principal.
 * ════════════════════════════════════════════════════════════════════
 *
 * SINTAXIS QUE SE REPITE EN ESTE ARCHIVO:
 *   - "static": el método se llama con el nombre de la clase, sin crear objeto: Tareas.horasPico(...).
 *   - "->"  lambda: función corta sin nombre. "r -> r.hora()" se lee "dado r, devuelve r.hora()".
 *   - "::"  referencia a método: atajo de la lambda. "Registro::estacion" equivale a
 *           "r -> r.estacion()".
 *   - "Map<String, Long>": mapa cuya clave es String y cuyo valor es Long (entero grande;
 *           se escribe con mayúscula porque los genéricos solo aceptan objetos).
 *   - Un pipeline tiene tres partes: FUENTE (stream), pasos INTERMEDIOS (filter, map...,
 *     que no ejecutan nada por sí solos) y una operación TERMINAL (collect, count, toList...)
 *     que es la que pone todo en marcha y produce el resultado.
 */
public final class Tareas {

    // Constructor privado: es una clase de utilidad, no se crean objetos de ella.
    private Tareas() {
    }

    // ───────────────────────────── a) AFLUENCIA ─────────────────────────────
    // Enunciado: "Contar cuántos usuarios ingresan a cada estación durante el día".
    public static Map<String, Long> calculoAfluencia(List<Registro> registros) {
        return registros.parallelStream()          // FUENTE: lee la lista repartiéndola entre varios hilos (no la modifica)
                .filter(Registro::esEntrada)       // INTERMEDIO: deja pasar solo los registros cuya acción es ENTRADA
                .collect(Collectors.collectingAndThen(          // TERMINAL: junta el resultado y luego le aplica un paso final
                        Collectors.groupingBy(                  //   agrupa los registros en un Map...
                                Registro::estacion,             //   ...usando la estación como CLAVE
                                Collectors.counting()),         //   ...y contando cuántos hay en cada grupo como VALOR
                        Collections::unmodifiableMap));         //   paso final: vuelve el mapa de solo lectura
    }

    // ───────────────────────────── b) HORAS PICO ─────────────────────────────
    // Enunciado: "Agrupar los registros por hora del día y calcular momentos de mayor flujo".
    // "cantidad" indica cuántas horas mostrar en el ranking (por ejemplo las 5 primeras).
    public static Map<Integer, Long> horasPico(List<Registro> registros, int cantidad) {
        // Primero: cuántos registros hay en cada hora. Clave: Integer (la hora 0-23). Valor: Long (cantidad).
        Map<Integer, Long> porHora = registros.parallelStream()
                .collect(Collectors.groupingBy(Registro::hora, Collectors.counting()));
        // Segundo: ordenar de mayor a menor y quedarse con las primeras "cantidad".
        return masFrecuentes(porHora, cantidad);
    }

    // ───────────────────────── c) RUTAS MÁS UTILIZADAS ─────────────────────────
    // Enunciado: "Determinar cuáles rutas tienen mayor volumen de uso".
    // (El mismo método rutas con distinto número de parámetros sería una SOBRECARGA.)
    public static Map<String, Long> rutas(List<Registro> registros, int cantidad) {
        Map<String, Long> porRuta = registros.parallelStream()
                .collect(Collectors.groupingBy(Registro::ruta, Collectors.counting()));
        return masFrecuentes(porRuta, cantidad);
    }

    // ───────────────────────── d) PATRONES DE VIAJE ─────────────────────────
    // Enunciado: "para cada usuario, un listado de estaciones visitadas en el orden en que
    // aparecen en los registros".
    public static Map<String, List<String>> patronesViaje(List<Registro> registros) {
        return registros.parallelStream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(                  // agrupa por usuario (CLAVE: idUsuario)
                                Registro::idUsuario,
                                Collectors.mapping(             // de cada registro del grupo, guarda solo...
                                        Registro::estacion,     // ...su estación (transforma Registro -> String)
                                        Collectors.toUnmodifiableList())),  // ...en una lista inmutable
                        Collections::unmodifiableMap));         // y el mapa final también de solo lectura
        // El orden se respeta: con una lista como fuente, groupingBy conserva el orden en que
        // aparecen los registros dentro de cada grupo, incluso con parallelStream.
    }

    // ─────────────────── e) TIEMPO PROMEDIO ENTRE ESTACIONES ───────────────────
    // Enunciado: "estimar el tiempo que toman los usuarios en moverse entre estaciones
    // consecutivas". Resultado: clave = tramo ("Portal Norte -> Centro Mayor"), valor = promedio.
    public static Map<String, Duration> calculoPromedioEstaciones(List<Registro> registros) {
        return registros.parallelStream()
                .collect(Collectors.groupingBy(Registro::idUsuario))   // Map<usuario, lista de sus registros>
                .values().parallelStream()                             // se queda solo con las listas (una por usuario)
                .flatMap(Tareas::tramosDe)                             // cada usuario -> sus tramos (pares tramo/duración), todos en un solo flujo
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(                         // agrupa los tramos...
                                Map.Entry::getKey,                     // ...por el nombre del tramo (clave del par)
                                Collectors.collectingAndThen(
                                        Collectors.averagingLong(      // calcula el PROMEDIO de...
                                                tramo -> tramo.getValue().toSeconds()),  // ...los segundos de cada duración
                                        segundos -> Duration.ofSeconds(Math.round(segundos)))),  // y vuelve a convertir el promedio en Duration
                        Collections::unmodifiableMap));
    }

    // ───────────────────────── f) SOBRECARGA EN RUTAS ─────────────────────────
    // Enunciado: "Si una ruta supera un umbral de ocupación (simulado), marcarla como crítica".
    // "tamano" es el umbral. Devuelve solo las rutas críticas con su ocupación.
    public static Map<String, Long> sobrecargaRutas(List<Registro> registros, long tamano) {
        return registros.parallelStream()
                .collect(Collectors.groupingBy(Registro::ruta, Collectors.counting()))  // Map<ruta, ocupación>
                .entrySet().stream()                       // recorre los pares (ruta, ocupación)
                .filter(ruta -> ruta.getValue() > tamano)  // se queda con los que SUPERAN el umbral
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),  // arma un Map nuevo con esos pares
                        Collections::unmodifiableMap));
    }

    // ═══════════════ MÉTODOS AUXILIARES PRIVADOS (también puros) ═══════════════

    /**
     * Ordena un mapa de conteos de MAYOR a MENOR, se queda con los primeros "cantidad" y
     * devuelve un mapa que conserva ese orden.
     *
     * <K extends Comparable<K>>: método GENÉRICO. K es "el tipo de la clave" (String o Integer
     * según quién llame) y debe poder compararse, para poder desempatar por clave.
     */
    private static <K extends Comparable<K>> Map<K, Long> masFrecuentes(Map<K, Long> conteos, int cantidad) {
        return conteos.entrySet().stream()      // recorre los pares clave/valor del mapa
                .sorted(                        // INTERMEDIO: ordena...
                        Map.Entry.<K, Long>comparingByValue(Comparator.reverseOrder())  // ...por valor, de mayor a menor
                                .thenComparing(Map.Entry.comparingByKey()))             // ...y si hay empate, por clave (resultado estable)
                .limit(cantidad)                // INTERMEDIO: corta después de "cantidad" elementos
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(       // TERMINAL: convierte los pares en un Map
                                Map.Entry::getKey,                            // clave de cada par
                                Map.Entry::getValue,                          // valor de cada par
                                (primero, segundo) -> primero,                // qué hacer si hubiera claves repetidas (no ocurre)
                                LinkedHashMap::new),                          // tipo de mapa: conserva el orden de inserción
                        Collections::unmodifiableMap));                       // de solo lectura
    }

    /**
     * Para los registros de UN usuario, produce los tramos recorridos: cada ENTRADA seguida de su
     * SALIDA en la misma ruta. Cada tramo es un par (nombre del tramo, duración).
     * Recibe la lista del usuario y NO la modifica: ordena una copia.
     */
    private static Stream<Map.Entry<String, Duration>> tramosDe(List<Registro> delUsuario) {
        List<Registro> ordenados = delUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))   // ordena por fecha y hora (en una copia)
                .toList();                                           // lista nueva e inmutable
        return IntStream.range(0, ordenados.size() - 1)              // posiciones 0, 1, ..., penúltima
                .filter(i -> ordenados.get(i).esEntrada()            // el registro i es una ENTRADA...
                        && ordenados.get(i + 1).esSalida()           // ...y el siguiente es una SALIDA...
                        && ordenados.get(i).ruta().equals(ordenados.get(i + 1).ruta()))  // ...de la misma ruta
                .mapToObj(i -> Map.entry(                            // convierte cada posición i en un par (clave, valor)
                        ordenados.get(i).estacion() + " -> " + ordenados.get(i + 1).estacion(),   // clave: "origen -> destino"
                        Duration.between(ordenados.get(i).timestamp(), ordenados.get(i + 1).timestamp())));  // valor: tiempo entre ambos
    }
}
