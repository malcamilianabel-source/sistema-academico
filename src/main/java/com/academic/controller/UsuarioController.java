package com.academic.controller;

import com.academic.model.Usuario;
import com.academic.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("paginaActual", "usuarios");
        return "usuarios/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String nombre,
                          @RequestParam String apellido,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String rol,
                          RedirectAttributes ra) {
        if (usuarioService.existeEmail(email)) {
            ra.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setPassword(password);
        usuario.setRol(rol);
        usuarioService.guardar(usuario);
        ra.addFlashAttribute("mensaje", "Usuario registrado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Usuario editar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long id,
                             @RequestParam String nombre,
                             @RequestParam String apellido,
                             @RequestParam String email,
                             @RequestParam(required = false) String password,
                             @RequestParam String rol,
                             RedirectAttributes ra) {
        Usuario usuario = usuarioService.buscarPorId(id).orElseThrow();

        var existenteEmail = usuarioService.buscarPorEmail(email);
        if (existenteEmail.isPresent() && !existenteEmail.get().getId().equals(id)) {
            ra.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }

        try {
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setEmail(email);
            if (password != null && !password.isBlank()) {
                usuario.setPassword(password);
            }
            usuario.setRol(rol);
            usuarioService.guardar(usuario);
            ra.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        usuarioService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        return "redirect:/usuarios";
    }
}
