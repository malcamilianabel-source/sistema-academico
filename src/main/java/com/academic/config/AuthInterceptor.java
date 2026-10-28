package com.academic.config;

import com.academic.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/") || path.equals("/login") || path.equals("/acceso-denegado")
                || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/error")) {
            return true;
        }

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuario");
        if (usuario == null) {
            response.sendRedirect("/login");
            return false;
        }

        String rol = usuario.getRol() == null ? "" : usuario.getRol().toUpperCase();

        if ("ADMIN".equals(rol)) {
            return true;
        }

        if ("DOCENTE".equals(rol)) {
            if (path.startsWith("/contenido-curso")) return true;
            if (path.equals("/dashboard") || path.equals("/logout") || path.equals("/historial") || path.startsWith("/evaluaciones")
                    || path.startsWith("/asistencias") || path.startsWith("/componentes")) {
                return true;
            }
            if (path.startsWith("/sesiones")) {
                return true; // titular o apoyo: autorización por sección en controlador
            }
            if ((path.startsWith("/cursos") || path.startsWith("/periodos")
                    || path.startsWith("/asignaciones"))
                    && "GET".equalsIgnoreCase(method)) {
                return true;
            }
            return denegar(response);
        }

        if ("ESTUDIANTE".equals(rol)) {
            if ((path.equals("/mis-cursos") || path.startsWith("/mis-cursos/")) && "GET".equalsIgnoreCase(method)) return true;
            if (path.equals("/dashboard") || path.equals("/logout") || path.equals("/historial")
                    || (path.equals("/matriculas") && "GET".equalsIgnoreCase(method))
                    || (path.equals("/matriculas/registrar") && "POST".equalsIgnoreCase(method))) {
                return true;
            }
            return denegar(response);
        }

        return denegar(response);
    }

    private boolean denegar(HttpServletResponse response) throws Exception {
        response.sendRedirect("/acceso-denegado");
        return false;
    }
}
