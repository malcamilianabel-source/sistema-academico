package com.academic.controller;

import com.academic.config.ProteccionCsrf;
import com.academic.model.Docente;
import com.academic.model.Usuario;
import com.academic.service.DocenteService;
import com.academic.service.EstudianteService;
import com.academic.service.PeriodoAcademicoService;
import com.academic.service.UsuarioService;
import com.academic.service.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pantallas reales (controlador + plantilla Thymeleaf + interceptores), con los servicios simulados.
 */
@WebMvcTest(controllers = {DocenteController.class, EstudianteController.class,
        PeriodoAcademicoController.class, AuthController.class})
class FormulariosWebTest {

    @Autowired MockMvc mvc;
    @MockBean DocenteService docenteService;
    @MockBean EstudianteService estudianteService;
    @MockBean PeriodoAcademicoService periodoService;
    @MockBean UsuarioService usuarioService;

    private MockHttpSession admin;

    private static MockHttpSession sesionDe(String rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Ana");
        u.setApellido("Torres");
        u.setRol(rol);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", u);
        ProteccionCsrf.renovar(session);
        return session;
    }

    private static String token(MockHttpSession session) {
        return ProteccionCsrf.tokenDe(session);
    }

    @BeforeEach
    void preparar() {
        admin = sesionDe("ADMIN");
        when(docenteService.listarTodos()).thenReturn(List.of());
        when(estudianteService.listarTodos()).thenReturn(List.of());
        when(periodoService.listarTodos()).thenReturn(List.of());
    }

    @Test
    void telefonoRepetidoReabreElModalConElMensajeYLosDatosEscritos() throws Exception {
        when(docenteService.registrar(any())).thenThrow(
                new ValidacionException("telefono", "Este teléfono ya está registrado en otro docente."));

        mvc.perform(post("/docentes/guardar").session(admin).param("_csrf", token(admin))
                        .param("nombre", "Carlos").param("apellido", "Ramírez").param("dni", "40123456")
                        .param("email", "cramirez@academico.com").param("telefono", "987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("docentes/lista"))
                .andExpect(model().attribute("abrirModal", "modalNuevo"))
                .andExpect(model().attributeHasFieldErrors("docente", "telefono"))
                .andExpect(content().string(containsString("Este teléfono ya está registrado en otro docente.")))
                // El campo queda en rojo y conserva lo escrito
                .andExpect(content().string(matchesPattern(
                        "(?s).*id=\"nTelefono\"[^>]*class=\"form-control is-invalid\".*")))
                .andExpect(content().string(containsString("value=\"40123456\"")));
    }

    @Test
    void docenteValidoSeGuardaYVuelveALaLista() throws Exception {
        when(docenteService.registrar(any())).thenReturn(new Docente());

        mvc.perform(post("/docentes/guardar").session(admin).param("_csrf", token(admin))
                        .param("nombre", "Carlos").param("apellido", "Ramírez").param("dni", "40123456")
                        .param("email", "cramirez@academico.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/docentes"))
                .andExpect(flash().attribute("mensaje", "Docente guardado correctamente."));
    }

    @Test
    void telefonoRepetidoAlEditarEstudianteReabreElModalDeEdicion() throws Exception {
        when(estudianteService.buscarPorId(5L)).thenReturn(Optional.of(new com.academic.model.Estudiante()));
        when(estudianteService.actualizar(any())).thenThrow(
                new ValidacionException("telefono", "Este teléfono ya está registrado en otro estudiante."));

        mvc.perform(post("/estudiantes/actualizar").session(admin).param("_csrf", token(admin))
                        .param("id", "5").param("nombre", "María").param("apellido", "Quispe")
                        .param("dni", "72845619").param("email", "mquispe@academico.com").param("telefono", "987654321"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("abrirModal", "modalEditar"))
                .andExpect(model().attributeHasFieldErrors("estudianteEditar", "telefono"))
                .andExpect(content().string(containsString("Este teléfono ya está registrado en otro estudiante.")));
    }

    @Test
    void periodoConFechasInvertidasReabreElModal() throws Exception {
        when(periodoService.guardar(any())).thenThrow(
                new ValidacionException("fechaFin", "La fecha de fin debe ser posterior a la fecha de inicio."));

        mvc.perform(post("/periodos/guardar").session(admin).param("_csrf", token(admin))
                        .param("nombre", "2026-I").param("fechaInicio", "2026-07-15").param("fechaFin", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("abrirModal", "modalNuevo"))
                .andExpect(model().attributeHasFieldErrors("periodo", "fechaFin"))
                .andExpect(content().string(containsString("La fecha de fin debe ser posterior a la fecha de inicio.")));
    }

    @Test
    void losFormulariosPostLlevanElTokenYLosGetNo() throws Exception {
        String html = mvc.perform(get("/docentes").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String campo = "name=\"_csrf\" value=\"" + token(admin) + "\"";
        // Nuevo, editar y cerrar sesión (POST) llevan token; la búsqueda (GET) no
        assertTrue(html.split(java.util.regex.Pattern.quote(campo), -1).length - 1 >= 3);
        String busqueda = html.substring(html.indexOf("action=\"/docentes\""), html.indexOf("name=\"buscar\""));
        assertFalse(busqueda.contains("_csrf"));
    }

    @Test
    void postSinTokenSeRechazaYNoGuarda() throws Exception {
        mvc.perform(post("/docentes/guardar").session(admin)
                        .param("nombre", "Carlos").param("apellido", "Ramírez").param("dni", "40123456")
                        .param("email", "cramirez@academico.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acceso-denegado"));
        verify(docenteService, never()).registrar(any());
    }

    @Test
    void postConTokenDeOtraSesionSeRechaza() throws Exception {
        MockHttpSession otra = sesionDe("ADMIN");
        mvc.perform(post("/docentes/eliminar/3").session(admin).param("_csrf", token(otra)))
                .andExpect(redirectedUrl("/acceso-denegado"));
        verify(docenteService, never()).eliminar(any());
    }

    @Test
    void unEstudianteNoPuedeRegistrarDocentesNiConToken() throws Exception {
        MockHttpSession estudiante = sesionDe("ESTUDIANTE");
        mvc.perform(post("/docentes/guardar").session(estudiante).param("_csrf", token(estudiante))
                        .param("nombre", "Carlos"))
                .andExpect(redirectedUrl("/acceso-denegado"));
        verify(docenteService, never()).registrar(any());
    }

    @Test
    void loginCambiaElIdDeSesionYNoGuardaLaContrasena() throws Exception {
        Usuario u = new Usuario();
        u.setId(9L);
        u.setNombre("Ana");
        u.setApellido("Torres");
        u.setEmail("ana@academico.com");
        u.setPassword("$2a$10$hash");
        u.setRol("ADMIN");
        when(usuarioService.login("ana@academico.com", "clave123")).thenReturn(Optional.of(u));

        MockHttpSession session = new MockHttpSession();
        String tokenAntes = ProteccionCsrf.tokenDe(session);
        String idAntes = session.getId();

        mvc.perform(post("/login").session(session).param("_csrf", tokenAntes)
                        .param("email", "ana@academico.com").param("password", "clave123"))
                .andExpect(redirectedUrl("/dashboard"));

        Usuario enSesion = (Usuario) session.getAttribute("usuario");
        assertNotNull(enSesion);
        assertNull(enSesion.getPassword());
        assertNotEquals(idAntes, session.getId());
        assertNotEquals(tokenAntes, ProteccionCsrf.tokenDe(session));
    }

    @Test
    void loginSinTokenPideIngresarDeNuevo() throws Exception {
        mvc.perform(post("/login").param("email", "ana@academico.com").param("password", "clave123"))
                .andExpect(redirectedUrl("/login?expirada"));
        verify(usuarioService, never()).login(any(), any());
    }

    @Test
    void cerrarSesionSoloPorPost() throws Exception {
        mvc.perform(get("/logout").session(admin)).andExpect(redirectedUrl("/dashboard"));
        assertFalse(admin.isInvalid());

        mvc.perform(post("/logout").session(admin).param("_csrf", token(admin)))
                .andExpect(redirectedUrl("/login"));
        assertTrue(admin.isInvalid());
    }
}
