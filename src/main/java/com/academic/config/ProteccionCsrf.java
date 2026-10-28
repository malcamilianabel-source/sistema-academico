package com.academic.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Token contra CSRF: cada sesión tiene un valor aleatorio que viaja oculto en todos los formularios POST
 * ({@link CsrfDataValueProcessor}) y que {@link CsrfInterceptor} exige antes de procesar la operación.
 * Así otra página no puede enviar formularios en nombre de un usuario con la sesión abierta.
 */
public final class ProteccionCsrf {

    public static final String PARAMETRO = "_csrf";
    public static final String CABECERA = "X-CSRF-TOKEN";
    static final String ATRIBUTO_SESION = "CSRF_TOKEN";
    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private ProteccionCsrf() {
    }

    /** Token de la sesión; lo crea si todavía no existe. */
    public static String tokenDe(HttpSession session) {
        Object actual = session.getAttribute(ATRIBUTO_SESION);
        if (actual instanceof String token) return token;
        return renovar(session);
    }

    /** Genera un token nuevo (se usa al iniciar sesión). */
    public static String renovar(HttpSession session) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(ATRIBUTO_SESION, token);
        return token;
    }

    static boolean requiereToken(String metodo) {
        return metodo == null || !METODOS_SEGUROS.contains(metodo.toUpperCase());
    }

    /** Compara en tiempo constante el token enviado con el de la sesión. */
    static boolean esValido(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(ATRIBUTO_SESION) instanceof String esperado)) return false;
        String enviado = request.getParameter(PARAMETRO);
        if (enviado == null) enviado = request.getHeader(CABECERA);
        if (enviado == null) return false;
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                enviado.getBytes(StandardCharsets.UTF_8));
    }
}
