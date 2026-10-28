package com.academic.config;

import jakarta.servlet.SessionTrackingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.util.EnumSet;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private CsrfInterceptor csrfInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Primero se comprueba el token CSRF y después los permisos del rol
        registry.addInterceptor(csrfInterceptor).addPathPatterns("/**").order(0);
        registry.addInterceptor(authInterceptor).addPathPatterns("/**").order(1);
    }

    /** Agrega el token CSRF a todos los formularios POST de Thymeleaf (el nombre del bean es obligatorio). */
    @Bean(name = "requestDataValueProcessor")
    public RequestDataValueProcessor requestDataValueProcessor() {
        return new CsrfDataValueProcessor();
    }

    /**
     * La sesión viaja solo en la cookie, nunca en la URL (";jsessionid=..."). Además de ser más seguro,
     * corrige el primer ingreso: el formulario de login apuntaba a "/login;jsessionid=..." y
     * AuthInterceptor lo devolvía al login.
     */
    @Bean
    public ServletContextInitializer sesionSoloPorCookie() {
        return servletContext -> servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
    }

    /** La cookie de sesión no se envía en peticiones POST que vienen de otros sitios. */
    @Bean
    public CookieSameSiteSupplier cookieSesionSameSite() {
        return CookieSameSiteSupplier.ofLax().whenHasName("JSESSIONID");
    }
}
