package es.prw.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Contacto {
    @GetMapping("/contacto")
    public String mostrarFormularioContacto() {
        return "contacto"; // Aquí se asume que contacto.html está en src/main/resources/templates
    }
}
