package com.rica.investigadores_service.aplicacion;

import org.springframework.stereotype.Component;

import com.rica.investigadores_service.dominio.CorreoDuplicadoException;
import com.rica.investigadores_service.dominio.CorreoInstitucional;
import com.rica.investigadores_service.dominio.Investigador;

@Component 
public class InvestigadorFactory {

    private final RepositorioInvestigadores investigadorRepository;

    public InvestigadorFactory(RepositorioInvestigadores investigadorRepository) {
        this.investigadorRepository = investigadorRepository;
    }

    public Investigador crear(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        CorreoInstitucional correo = new CorreoInstitucional(correoInstitucional);

        if (investigadorRepository.existeCorreo(correo.valor())) {
        throw new CorreoDuplicadoException(
            "Ya existe un investigador registrado con el correo " + correo.valor());
        }

        return new Investigador(null, nombreCompleto, correo, grupoInvestigacion);
    }
    
}
