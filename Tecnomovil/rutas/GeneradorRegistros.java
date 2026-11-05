package rutas;

// Accion y Registro viven en la carpeta/paquete "registro", por eso se importan.
// OpcionesRutas está en este mismo paquete (rutas), así que no necesita import.
import registro.Accion;
import registro.Registro;

// LocalDate: una fecha sin hora (año, mes, día). Se usa para fijar el día simulado.
import java.time.LocalDate;
// LocalDateTime: fecha + hora. Es el tipo del timestamp de cada Registro.
import java.time.LocalDateTime;
// ArrayList: lista modificable. Aquí se usa SOLO como lista local temporal, que nadie
// más ve, para armar los registros de un usuario.
import java.util.ArrayList;
// List: la interfaz de lista (tipo de lo que se devuelve).
import java.util.List;
// SplittableRandom: generador de números aleatorios. Con la misma semilla produce siempre
// la misma secuencia. Se prefiere a Random porque está hecho para crear un generador
// independiente por cada tarea, sin compartir estado entre hilos.
import java.util.SplittableRandom;
// IntStream: un stream de números int (0, 1, 2, ...). Sirve para repetir algo N veces.
import java.util.stream.IntStream;
// Stream: un flujo de elementos sobre el que se encadenan operaciones (filter, map...).
import java.util.stream.Stream;

/**
 * Fabrica el conjunto grande de datos simulados que pide el enunciado (sección 2).
 *
 * Es DETERMINISTA: con los mismos argumentos (cantidadUsuarios, iniciador) devuelve siempre
 * exactamente la misma lista. Cada usuario usa su propio generador aleatorio derivado del
 * "iniciador" y de su número, así que los usuarios no comparten estado.
 *
 * NOTA: esta clase simula datos, no los analiza. Por dentro usa un ciclo for y una lista
 * local modificable; hacia afuera se comporta como función pura (mismos argumentos, mismo
 * resultado) y entrega una lista inmutable. Las seis tareas de análisis viven en Tareas.
 */
public final class GeneradorRegistros {

    // Día simulado. "static final": constante de la clase.
    private static final LocalDate DIA = LocalDate.of(2026, 10, 26);

    // Peso relativo de cada hora del día: la posición 0 es la medianoche y la 23 las 11 p. m.
    // Una hora con peso 24 (las 7 a. m.) recibe unas 24 veces más viajes que una con peso 1.
    // Así los datos tienen horas pico reales. "int[]" es un arreglo de tamaño fijo.
    private static final int[] pesoHora = {
            1, 1, 1, 1, 2, 6, 16, 24, 18, 9, 6, 6,
            8, 7, 6, 7, 13, 22, 16, 9, 5, 3, 2, 1};

    private GeneradorRegistros() {
    }

    /**
     * Genera todos los registros de "cantidadUsuarios" usuarios.
     * "iniciador" es la semilla: mismo número, mismos datos.
     */
    public static List<Registro> generar(int cantidadUsuarios, long iniciador) {
        return IntStream.range(0, cantidadUsuarios)  // 0, 1, 2, ... cantidadUsuarios-1 (un número por usuario)
                .boxed()                              // convierte int en Integer (objeto), necesario para flatMap
                .flatMap(indice -> registrosDeUsuario(indice, new SplittableRandom(iniciador + indice)))
                // flatMap: cada usuario produce VARIOS registros (un Stream); flatMap los junta
                // todos en un solo flujo plano. "indice -> ..." es una lambda: recibe el número
                // del usuario y devuelve sus registros. El generador aleatorio es nuevo para cada usuario.
                .toList();                            // operación terminal: recoge todo en una List INMUTABLE
    }

    /**
     * Crea los registros de un usuario: de 1 a 3 viajes, y cada viaje son 2 registros
     * (una ENTRADA y una SALIDA).
     */
    private static Stream<Registro> registrosDeUsuario(int indice, SplittableRandom azar) {
        // %06d = número entero rellenado con ceros hasta 6 dígitos -> "U-000042"
        String usuario = String.format("U-%06d", indice);
        int cantidadViajes = 1 + azar.nextInt(3);                 // nextInt(3) da 0, 1 o 2 -> viajes: 1, 2 o 3
        LocalDateTime disponibleDesde = primeraSalida(azar);      // hora en que empieza su primer viaje
        List<Registro> registros = new ArrayList<>();             // lista local: nadie más la ve

        for (int viaje = 0; viaje < cantidadViajes && !disponibleDesde.toLocalDate().isAfter(DIA); viaje++) {
            String ruta = rutaSegunDemanda(azar);                              // elige ruta según su demanda
            List<String> estaciones = OpcionesRutas.estacionesDeRuta(ruta);    // estaciones de esa ruta
            int origen = azar.nextInt(estaciones.size());                      // posición de la estación de entrada
            int saltos = 1 + azar.nextInt(estaciones.size() - 1);              // cuántas estaciones avanza (mínimo 1)
            int destino = (origen + saltos) % estaciones.size();               // % da la vuelta si se pasa del final
            // Duración del viaje: 4 min fijos + 7 min por estación recorrida + 0 a 5 min al azar.
            // El "L" en 7L hace que el cálculo sea long (entero grande), lo que pide plusMinutes.
            LocalDateTime salida = disponibleDesde.plusMinutes(4 + saltos * 7L + azar.nextInt(6));

            // new Registro(...) usa el constructor que Java creó automáticamente para el record.
            registros.add(new Registro(usuario, ruta, estaciones.get(origen), Accion.ENTRADA, disponibleDesde));
            registros.add(new Registro(usuario, ruta, estaciones.get(destino), Accion.SALIDA, salida));

            // El siguiente viaje empieza entre 45 y 344 minutos después de esta salida.
            disponibleDesde = salida.plusMinutes(45 + azar.nextInt(300));
        }
        return registros.stream();   // convierte la lista local en Stream para que flatMap lo junte
    }

    /**
     * Elige una ruta con probabilidad proporcional a su peso de demanda (técnica del "dardo"):
     * se suman todos los pesos, se lanza un número al azar entre 0 y el total, y se recorren
     * las rutas acumulando pesos hasta ver en cuál "cayó" el dardo.
     */
    private static String rutaSegunDemanda(SplittableRandom azar) {
        int total = OpcionesRutas.rutas.stream()
                .mapToInt(OpcionesRutas.pesoDemanda::get)   // "::" referencia a método: para cada ruta, pesoDemanda.get(ruta)
                .sum();                                      // suma de todos los pesos (100)
        int dardo = azar.nextInt(total);                     // número entre 0 y total-1
        int acumulado = 0;
        for (String ruta : OpcionesRutas.rutas) {            // "for each": recorre cada ruta de la lista
            acumulado += OpcionesRutas.pesoDemanda.get(ruta);
            if (dardo < acumulado) {
                return ruta;
            }
        }
        return OpcionesRutas.rutas.get(0);                   // solo por seguridad; no debería llegar aquí
    }

    /**
     * Elige la hora de la primera salida con el mismo método del dardo, usando pesoHora,
     * y le agrega minutos y segundos al azar.
     */
    private static LocalDateTime primeraSalida(SplittableRandom azar) {
        int total = IntStream.of(pesoHora).sum();            // suma de los 24 pesos
        int dardo = azar.nextInt(total);
        int acumulado = 0;
        int hora = 0;
        for (int i = 0; i < pesoHora.length; i++) {          // "i" es la hora (0 a 23)
            acumulado += pesoHora[i];
            if (dardo < acumulado) {
                hora = i;
                break;                                       // ya encontró la hora: sale del ciclo
            }
        }
        // atTime(hora, minuto, segundo): combina el día simulado con esa hora.
        return DIA.atTime(hora, azar.nextInt(60), azar.nextInt(60));
    }
}
