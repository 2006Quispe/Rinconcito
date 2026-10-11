package com.rinconcito.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PersonalStubController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Panel administrador"
        );

        model.addAttribute(
                "rol",
                "ADMINISTRADOR"
        );

        return "paginas/admin/dashboard";
    }

}