package com.rica.investigadores_service.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Taller 3 (DDD): la Entidad Investigador, raíz de su Agregado.
 * Taller 6 (Saga): el estado EN_ELIMINACION es parte del comportamiento del
 * dominio.
 */
class InvestigadorTest {

    private static Investigador ana() {
        return new Investigador(1L, "Ana Torres",
                new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT-UPTC");
    }

    @Test
    void unInvestigadorNuevoEstaActivo() {
        Investigador investigador = ana();

        assertEquals(EstadoInvestigador.ACTIVO, investigador.getEstado());
        assertFalse(investigador.estaEnEliminacion());
    }

    @Test
    void conservaSuCorreoComoValueObject() {
        Investigador investigador = ana();

        assertEquals(new CorreoInstitucional("ana.torres@uptc.edu.co"),
                investigador.getCorreoInstitucional());
        assertEquals("ana.torres@uptc.edu.co", investigador.getCorreoInstitucional().valor());
    }

    @Test
    void marcarEnEliminacionCambiaSuEstado() {
        Investigador investigador = ana();

        investigador.marcarEnEliminacion();

        assertEquals(EstadoInvestigador.EN_ELIMINACION, investigador.getEstado());
        assertTrue(investigador.estaEnEliminacion());
    }

    @Test
    void marcarEnEliminacionDosVecesNoFalla() {
        Investigador investigador = ana();

        investigador.marcarEnEliminacion();
        investigador.marcarEnEliminacion();

        assertTrue(investigador.estaEnEliminacion());
    }

}
