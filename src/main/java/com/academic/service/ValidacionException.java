package com.academic.service;

/**
 * Regla de negocio incumplida en un campo concreto del formulario.
 * Los controladores la convierten en un error de ese campo para mostrarlo dentro del modal.
 */
public class ValidacionException extends IllegalArgumentException {

    private final String campo;

    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
