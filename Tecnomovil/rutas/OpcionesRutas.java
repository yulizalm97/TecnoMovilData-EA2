package rutas;

// List: lista ordenada de elementos (java.util). List.of(...) crea una lista INMUTABLE:
// no admite add(), remove() ni set(); si se intenta, lanza UnsupportedOperationException.
import java.util.List;
// Map: colección de pares clave -> valor (java.util). Map.of(...) crea un mapa INMUTABLE.
import java.util.Map;

/**
 * Datos fijos de la red de transporte (rutas, estaciones de cada ruta y demanda).
 *
 * Es una CLASE DE UTILIDAD (<<utility>> en el UML):
 *   - "final" en la clase: nadie puede heredar de ella.
 *   - constructor privado: nadie puede hacer "new OpcionesRutas()". No hace falta, porque
 *     todo lo que contiene es estático y se usa así: OpcionesRutas.rutas
 *   - "static final" en los atributos: pertenecen a la clase (no a un objeto) y no se pueden
 *     reasignar. Combinado con List.of/Map.of, los datos quedan 100 % de solo lectura
 *     ({readOnly} en el UML). Por eso es seguro dejarlos "public".
 */
public final class OpcionesRutas {

    // Todas las rutas de la red.
    public static final List<String> rutas = List.of(
            "T-01 Troncal Norte",
            "T-02 Troncal Sur",
            "A-11 Alimentador Oriente",
            "A-12 Alimentador Occidente",
            "C-21 Circular Centro");

    // Map<clave, valor>: ruta -> lista de sus estaciones (en orden de recorrido).
    public static final Map<String, List<String>> estacionesPorRuta = Map.of(
            "T-01 Troncal Norte", List.of("Portal Norte", "La Ceiba", "San Jose", "Centro Mayor"),
            "T-02 Troncal Sur", List.of("Portal Sur", "El Mirador", "Las Palmas", "Centro Mayor"),
            "A-11 Alimentador Oriente", List.of("Loma Verde", "El Rosal", "San Jose"),
            "A-12 Alimentador Occidente", List.of("Villa Luz", "El Prado", "Las Palmas"),
            "C-21 Circular Centro", List.of("Centro Mayor", "Plaza Bolivar", "Estadio", "Universidad"));

    // Map<String, Integer>: ruta -> peso de demanda. Una ruta con peso 32 se elige unas
    // tres veces más que una con peso 10 al generar los datos simulados.
    public static final Map<String, Integer> pesoDemanda = Map.of(
            "T-01 Troncal Norte", 32,
            "T-02 Troncal Sur", 28,
            "A-11 Alimentador Oriente", 16,
            "A-12 Alimentador Occidente", 14,
            "C-21 Circular Centro", 10);

    // Constructor privado: impide crear objetos de esta clase (es solo de utilidad).
    private OpcionesRutas() {
    }

    /**
     * Devuelve las estaciones de UNA ruta. "static": se llama sin crear objeto,
     * así: OpcionesRutas.estacionesDeRuta("T-01 Troncal Norte").
     * getOrDefault(clave, valorPorDefecto): busca la ruta en el mapa y, si no existe,
     * devuelve una lista vacía en lugar de fallar con null.
     */
    public static List<String> estacionesDeRuta(String ruta) {
        return estacionesPorRuta.getOrDefault(ruta, List.of());
    }
}
