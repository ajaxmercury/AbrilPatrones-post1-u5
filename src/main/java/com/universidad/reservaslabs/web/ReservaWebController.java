package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservas")
public class ReservaWebController {

    private final ReservaService reservaService;
    private final LaboratorioRepository laboratorioRepository;

    public ReservaWebController(ReservaService reservaService, LaboratorioRepository laboratorioRepository) {
        this.reservaService = reservaService;
        this.laboratorioRepository = laboratorioRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reservas", reservaService.findAll());
        return "reservas/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("reserva", new Reserva());
        model.addAttribute("laboratorios", laboratorioRepository.findAll());
        return "reservas/nueva";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("reserva") Reserva reserva, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("laboratorios", laboratorioRepository.findAll());
            return "reservas/nueva";
        }
        reservaService.crear(reserva);
        redirectAttributes.addFlashAttribute("mensaje", "Reserva creada correctamente");
        return "redirect:/reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reservaService.cancelar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Reserva cancelada correctamente");
        return "redirect:/reservas";
    }
}
