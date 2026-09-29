package com.academic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccesoController {
    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "errores/403";
    }
}
