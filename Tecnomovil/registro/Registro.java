package registro;

// LocalDateTime (paquete java.time): guarda una fecha y una hora juntas, sin zona horaria.
// Ejemplo: 2026-10-26T07:12:30. Es inmutable: sus métodos devuelven una fecha nueva en
// lugar de cambiar la existente. Se usa para el campo "timestamp" del enunciado.
import java.time.LocalDateTime;

/**
 * RECORD: una clase pensada solo para guardar datos, e inmutable.
 *
 * Al escribir "record Registro(String idUsuario, ...)" Java crea automáticamente:
 *   - los atributos, privados y final (no se pueden cambiar después de crear el objeto),
 *   - el constructor:  new Registro(id, ruta, estacion, accion, timestamp),
 *   - un método de lectura por cada campo:  registro.estacion(), registro.ruta(), ...
 *     (sin "get": el nombre del método es igual al del campo),
 *   - equals(), hashCode() y toString().
 *
 * Por ser inmutable, un Registro puede compartirse entre hilos (parallelStream)
 * sin riesgo: nadie puede modificarlo.
 *
 * Campos del enunciado (3.1): idUsuario, ruta, estación, acción, timestamp.
 */
public record Registro(String idUsuario,
                       String ruta,
                       String estacion,
                       Accion accion,
                       LocalDateTime timestamp) {

    // ¿Es una entrada? Devuelve true/false. Sirve como criterio en filter(Registro::esEntrada).
    // "==" compara directamente porque los valores de un enum son únicos.
    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }

    // ¿Es una salida?
    public boolean esSalida() {
        return accion == Accion.SALIDA;
    }

    // Hora del día (0 a 23) del timestamp. Es la clave para agrupar por hora (tarea b).
    public int hora() {
        return timestamp.getHour();
    }
}

