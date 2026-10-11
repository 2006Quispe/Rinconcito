package com.rinconcito.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("titulo", "Ingresar");
        model.addAttribute("error", error != null);
        return "paginas/login";
    }

    @GetMapping("/personal/login")
    public String personalLogin(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("titulo", "Acceso del personal");
        model.addAttribute("error", error != null);
        return "paginas/personal-login";
    }
}
