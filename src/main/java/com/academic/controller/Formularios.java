package com.academic.controller;

import com.academic.service.ValidacionException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

/**
 * Convierte los errores de los servicios en errores del formulario, para que se muestren
 * dentro del modal (junto al campo cuando se sabe cuál es) en lugar de un aviso después de recargar.
 */
final class Formularios {

    static final String REGISTRO_SIMULTANEO =
            "No se pudo guardar porque otro registro con los mismos datos se guardó al mismo tiempo. "
                    + "Revisa los campos e inténtalo de nuevo.";

    private Formularios() {
    }

    static void rechazar(BindingResult result, IllegalArgumentException e) {
        if (e instanceof ValidacionException v && esCampo(result, v.getCampo())) {
            result.rejectValue(v.getCampo(), "invalido", v.getMessage());
        } else {
            result.reject("invalido", e.getMessage());
        }
    }

    /** Lo usa quien guarda cuando la base rechaza un dato único que otra persona acaba de registrar. */
    static void rechazarRegistroSimultaneo(BindingResult result) {
        result.reject("duplicado", REGISTRO_SIMULTANEO);
    }

    private static boolean esCampo(BindingResult result, String campo) {
        Object destino = result.getTarget();
        return campo != null && destino != null && new BeanWrapperImpl(destino).isReadableProperty(campo);
    }
}
