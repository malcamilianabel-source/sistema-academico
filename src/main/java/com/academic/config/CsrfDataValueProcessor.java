package com.academic.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.util.Map;

/**
 * Thymeleaf consulta este procesador en cada {@code <form th:action>}: a los formularios que no son GET
 * les agrega el campo oculto {@code _csrf}. Así ninguna plantilla tiene que escribirlo a mano.
 */
public class CsrfDataValueProcessor implements RequestDataValueProcessor {

    private static final String METODO_FORMULARIO = CsrfDataValueProcessor.class.getName() + ".metodo";

    @Override
    public String processAction(HttpServletRequest request, String action, String httpMethod) {
        request.setAttribute(METODO_FORMULARIO, httpMethod);
        return action;
    }

    @Override
    public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
        return value;
    }

    @Override
    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        Object metodo = request.getAttribute(METODO_FORMULARIO);
        request.removeAttribute(METODO_FORMULARIO);
        if (metodo instanceof String m && !ProteccionCsrf.requiereToken(m)) return Map.of();
        return Map.of(ProteccionCsrf.PARAMETRO, ProteccionCsrf.tokenDe(request.getSession(true)));
    }

    @Override
    public String processUrl(HttpServletRequest request, String url) {
        return url;
    }
}
