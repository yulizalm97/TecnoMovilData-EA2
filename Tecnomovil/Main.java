// Main está en la raíz del proyecto y no declara "package". Importa las clases de las
// carpetas/paquetes registro, rutas y tareas.
import rutas.GeneradorRegistros;
import registro.Registro;
import tareas.Tareas;

// Duration (java.time): cantidad de tiempo; aquí solo se usa para mostrar minutos y segundos.
import java.time.Duration;
// List y Map (java.util): lista y mapa clave -> valor.
import java.util.List;
import java.util.Map;
// TreeMap (java.util): un Map que ordena sus claves de la A a la Z. Se usa solo para IMPRIMIR
// la afluencia en orden alfabético (los mapas normales no garantizan orden).
import java.util.TreeMap;

/**
 * Programa principal. Aquí está el ÚNICO lugar donde se imprime: las tareas calculan
 * (funciones puras) y Main muestra los resultados. Así las tareas no tienen efectos secundarios.
 *
 * Flujo:  GeneradorRegistros.generar -> lista de registros -> Tareas.* -> mapas -> impresión.
 */
public class Main {

    // Punto de entrada: "public" para que Java lo vea, "static" porque aún no existe ningún
    // objeto, "void" porque no devuelve nada, "args" son los argumentos de la consola.
    public static void main(String[] args) {
        // 100_000 usuarios y la semilla 42 (los guiones bajos solo hacen legible el número).
        // La misma semilla da siempre los mismos datos.
        List<Registro> registros = GeneradorRegistros.generar(100_000, 42L);

        // hashCode() resume el contenido de la lista en un número. Se guarda ahora y se compara
        // al final para demostrar que las tareas NO modificaron la lista original.
        int huellaAntes = registros.hashCode();

        System.out.println("=== TecnoMovil Data ===");
        System.out.println("Registros generados: " + registros.size());
        probarInmutabilidad(registros);

        // a) Afluencia. new TreeMap<>(mapa) copia el mapa ordenado por clave (solo para imprimir).
        // forEach((clave, valor) -> ...) recorre el mapa; la lambda recibe DOS parámetros.
        // printf: %-14s = texto alineado a la izquierda en 14 espacios; %,d = número con
        // separador de miles; %n = salto de línea.
        System.out.println("\na) Afluencia por estacion (entradas)");
        new TreeMap<>(Tareas.calculoAfluencia(registros))
                .forEach((estacion, cantidad) -> System.out.printf("   %-14s %,d%n", estacion, cantidad));

        // b) Las 5 horas con más flujo. %02d = número con 2 dígitos (7 -> 07).
        System.out.println("\nb) Las 5 horas pico");
        Tareas.horasPico(registros, 5)
                .forEach((hora, cantidad) -> System.out.printf("   %02d:00 a %02d:59  %,d%n", hora, hora, cantidad));

        // c) Rutas de mayor a menor uso.
        System.out.println("\nc) Rutas mas utilizadas");
        Tareas.rutas(registros, 5)
                .forEach((ruta, cantidad) -> System.out.printf("   %-28s %,d%n", ruta, cantidad));

        // d) Hay 100.000 usuarios: se muestran solo 5. entrySet().stream() convierte el mapa en un
        // flujo de pares; sorted(...comparingByKey()) los ordena por usuario; limit(5) corta.
        System.out.println("\nd) Patrones de viaje (primeros 5 usuarios)");
        Tareas.patronesViaje(registros).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(5)
                .forEach(usuario -> System.out.println("   " + usuario.getKey() + ": " + usuario.getValue()));

        // e) Primeros 8 tramos en orden alfabético.
        System.out.println("\ne) Tiempo promedio entre estaciones (primeros 8 tramos)");
        Tareas.calculoPromedioEstaciones(registros).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(8)
                .forEach(tramo -> System.out.printf("   %-34s %s%n", tramo.getKey(), minutos(tramo.getValue())));

        // f) Umbral simulado: una ruta es crítica si tiene más de 100.000 registros.
        long umbral = 100_000;
        System.out.println("\nf) Rutas criticas (mas de " + String.format("%,d", umbral) + " registros)");
        Tareas.sobrecargaRutas(registros, umbral)
                .forEach((ruta, cantidad) -> System.out.printf("   %-28s %,d%n", ruta, cantidad));

        // Si la huella es igual, la lista no cambió: se cumple la inmutabilidad.
        System.out.println("\nLista original sin modificar: " + (huellaAntes == registros.hashCode()));
    }

    // Convierte una Duration en texto "20 min 31 s". toMinutes() son los minutos totales y
    // toSecondsPart() los segundos sobrantes (0 a 59).
    private static String minutos(Duration duracion) {
        return String.format("%d min %02d s", duracion.toMinutes(), duracion.toSecondsPart());
    }

    // Intenta agregar un registro a la lista. Como .toList() devuelve una lista inmutable, Java
    // lanza UnsupportedOperationException y el programa lo atrapa con try/catch.
    private static void probarInmutabilidad(List<Registro> registros) {
        try {
            registros.add(registros.get(0));
            System.out.println("ERROR: la lista se pudo modificar");
        } catch (UnsupportedOperationException e) {
            System.out.println("La lista es inmutable: agregar un registro lanza UnsupportedOperationException");
        }
    }
}
