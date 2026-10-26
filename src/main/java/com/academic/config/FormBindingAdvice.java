package com.academic.config;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;

/**
 * En los formularios enlazados a entidades (@ModelAttribute), un texto vacío llega como null:
 * así @NotBlank lo rechaza y los campos opcionales con @Pattern (teléfono) lo aceptan.
 * No recorta espacios, para no alterar contraseñas.
 */
@ControllerAdvice
public class FormBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Los @RequestParam sueltos (login, búsquedas) no tienen objeto destino y se dejan como están
        if (binder.getTarget() == null && binder.getTargetType() == null) return;
        binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : text);
            }
        });
    }
}
