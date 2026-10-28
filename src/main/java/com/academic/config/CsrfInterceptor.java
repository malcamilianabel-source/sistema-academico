package com.academic.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rechaza toda operación que modifica datos (POST) sin el token CSRF de la sesión.
 * Se ejecuta antes que {@link AuthInterceptor}.
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!ProteccionCsrf.requiereToken(request.getMethod()) || ProteccionCsrf.esValido(request)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        boolean conUsuario = session != null && session.getAttribute("usuario") != null;
        // Sin sesión (expiró o el formulario quedó abierto mucho tiempo): se vuelve a ingresar
        response.sendRedirect(conUsuario ? "/acceso-denegado" : "/login?expirada");
        return false;
    }
}
