package com.academic.service;

/**
 * Reglas de formato compartidas por los servicios.
 * Repiten en el servidor las validaciones que los formularios hacen en el navegador.
 */
final class Validaciones {

    static final String SOLO_LETRAS = "^[A-Za-záéíóúÁÉÍÓÚñÑ\\s]+$";
    static final String DNI = "^\\d{8}$";
    static final String TELEFONO = "^\\d{9}$";
    static final String EMAIL_INSTITUCIONAL = "^[a-zA-Z0-9._%+\\-]+@academico\\.com$";

    private Validaciones() {
    }

    static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }

    static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    static boolean coincide(String valor, String regex) {
        return !vacio(valor) && valor.matches(regex);
    }

    /** Un valor nulo se considera válido: el campo es opcional. */
    static boolean enRango(Integer valor, int min, int max) {
        return valor == null || (valor >= min && valor <= max);
    }

    static void nombreYApellido(String nombre, String apellido) {
        exigir(coincide(nombre, SOLO_LETRAS) && nombre.length() <= 100,
                "El nombre solo puede contener letras y espacios.");
        exigir(coincide(apellido, SOLO_LETRAS) && apellido.length() <= 100,
                "El apellido solo puede contener letras y espacios.");
    }

    /** Datos comunes de estudiantes y docentes. */
    static void persona(String nombre, String apellido, String dni, String email, String telefono) {
        nombreYApellido(nombre, apellido);
        exigir(coincide(dni, DNI), "El DNI debe tener exactamente 8 dígitos numéricos.");
        exigir(coincide(email, EMAIL_INSTITUCIONAL) && email.length() <= 150,
                "El email debe tener el formato nombre@academico.com");
        exigir(vacio(telefono) || telefono.matches(TELEFONO),
                "El teléfono debe tener exactamente 9 dígitos numéricos.");
    }
}
