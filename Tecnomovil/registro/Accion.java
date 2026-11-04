package registro;

/**
 * ENUMERACIÓN (enum).
 *
 * Un enum es un tipo de dato cuyos únicos valores posibles se escriben aquí, uno por uno.
 * Una variable de tipo Accion solo puede valer ENTRADA o SALIDA; cualquier otra cosa
 * ni siquiera compila.
 *
 * NOTA SOBRE EL ENUNCIADO: el enunciado dice que la acción es el texto "entrada" o "salida".
 * Aquí se usa un enum en lugar de un String porque con texto un error de escritura
 * ("entrda", "Entrada") no daría error y falsearía los conteos; con el enum el compilador
 * lo detecta. Es una decisión de diseño que mantiene el mismo significado.
 */
public enum Accion {
    ENTRADA,   // el usuario ingresa a la estación
    SALIDA     // el usuario valida para salir de la estación
}
