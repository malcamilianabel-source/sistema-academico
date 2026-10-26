package com.academic.config;

import com.academic.model.Usuario;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("Sistema");
        admin.setEmail("admin@demo.com");
        admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        admin.setRol("ADMIN");
        usuarioRepository.save(admin);
    }
}