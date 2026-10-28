package com.academic.controller;

import com.academic.config.ProteccionCsrf;
import com.academic.model.Usuario;
import com.academic.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String expirada,
                               HttpSession session, Model model) {
        if (session.getAttribute("usuario") != null) {
            return "redirect:/dashboard";
        }
        if (expirada != null) {
            model.addAttribute("error", "Tu sesión expiró o el formulario ya no era válido. Ingresa de nuevo.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email,
                                @RequestParam String password,
                                HttpServletRequest request,
                                Model model) {
        Optional<Usuario> usuarioOpt = usuarioService.login(email, password);
        if (usuarioOpt.isPresent()) {
            // Id de sesión nuevo al ingresar: un id conocido antes del login deja de servir
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("usuario", datosDeSesion(usuarioOpt.get()));
            ProteccionCsrf.renovar(session);
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Correo o contraseña incorrectos.");
        return "login";
    }

    /** Cerrar sesión solo por POST (con token), para que otra página no pueda cerrarla con un enlace. */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /** Un enlace antiguo a /logout ya no cierra la sesión: vuelve al inicio. */
    @GetMapping("/logout")
    public String logoutPorEnlace() {
        return "redirect:/dashboard";
    }

    /** Copia para la sesión con lo que usan las pantallas; nunca el hash de la contraseña. */
    private static Usuario datosDeSesion(Usuario usuario) {
        Usuario copia = new Usuario();
        copia.setId(usuario.getId());
        copia.setNombre(usuario.getNombre());
        copia.setApellido(usuario.getApellido());
        copia.setEmail(usuario.getEmail());
        copia.setRol(usuario.getRol());
        return copia;
    }
}
