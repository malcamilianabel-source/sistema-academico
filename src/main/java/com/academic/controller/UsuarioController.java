package com.academic.controller;

import com.academic.model.Usuario;
import com.academic.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        return vistaLista(model, null);
    }

    // Cualquier error (formato, email repetido, contraseña corta) vuelve al modal con lo escrito.
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("nuevoUsuario") Usuario usuario, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                usuarioService.registrar(usuario);
                ra.addFlashAttribute("mensaje", "Usuario registrado correctamente.");
                return "redirect:/usuarios";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        return vistaLista(model, "modalNuevo");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Usuario editar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id).orElse(null);
    }

    /**
     * Sin @Valid: al editar, la contraseña vacía significa "no cambiarla", así que su @NotBlank no aplica.
     * El servicio valida nombre, apellido, email, rol y, si se escribió, la contraseña.
     */
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute("usuarioEditar") Usuario form, BindingResult result,
                             HttpSession session, Model model, RedirectAttributes ra) {
        if (form.getId() == null || usuarioService.buscarPorId(form.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "El usuario no existe.");
            return "redirect:/usuarios";
        }
        if (esElMismo(session, form.getId()) && !"ADMIN".equals(form.getRol())) {
            result.rejectValue("rol", "propio", "No puedes quitarte el rol de administrador a ti mismo.");
        }
        if (!result.hasErrors()) {
            try {
                usuarioService.actualizar(form);
                ra.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
                return "redirect:/usuarios";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        form.setPassword(null);
        return vistaLista(model, "modalEditar");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (esElMismo(session, id)) {
            ra.addFlashAttribute("error", "No puedes eliminar tu propio usuario mientras tienes la sesión abierta.");
            return "redirect:/usuarios";
        }
        usuarioService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        return "redirect:/usuarios";
    }

    private boolean esElMismo(HttpSession session, Long id) {
        Usuario actual = (Usuario) session.getAttribute("usuario");
        return actual != null && actual.getId() != null && actual.getId().equals(id);
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        if (!model.containsAttribute("nuevoUsuario")) model.addAttribute("nuevoUsuario", new Usuario());
        if (!model.containsAttribute("usuarioEditar")) model.addAttribute("usuarioEditar", new Usuario());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "usuarios");
        return "usuarios/lista";
    }
}
