package com.academic.controller;

import com.academic.model.Usuario;
import com.academic.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("nuevoUsuario") Usuario usuario, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return vistaLista(model, "modalNuevo");
        }
        if (usuarioService.existeEmail(usuario.getEmail())) {
            ra.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }

        try {
            usuario.setId(null);
            usuario.setPassword(usuarioService.codificarPassword(usuario.getPassword()));
            usuarioService.guardar(usuario);
            ra.addFlashAttribute("mensaje", "Usuario registrado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Usuario editar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("usuarioEditar") Usuario form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        // Al editar, la contraseña vacía significa "no cambiarla": su @NotBlank no cuenta como error
        boolean soloFaltaPassword = form.getPassword() == null
                && result.getErrorCount() == result.getFieldErrorCount("password");
        if (result.hasErrors() && !soloFaltaPassword) {
            return vistaLista(model, "modalEditar");
        }

        Usuario usuario = usuarioService.buscarPorId(form.getId()).orElseThrow();

        var existenteEmail = usuarioService.buscarPorEmail(form.getEmail());
        if (existenteEmail.isPresent() && !existenteEmail.get().getId().equals(usuario.getId())) {
            ra.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }

        try {
            usuario.setNombre(form.getNombre());
            usuario.setApellido(form.getApellido());
            usuario.setEmail(form.getEmail());
            if (form.getPassword() != null) {
                usuario.setPassword(usuarioService.codificarPassword(form.getPassword()));
            }
            usuario.setRol(form.getRol());
            usuarioService.guardar(usuario);
            ra.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        usuarioService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        return "redirect:/usuarios";
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
