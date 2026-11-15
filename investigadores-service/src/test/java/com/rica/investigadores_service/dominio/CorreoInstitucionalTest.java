package com.rica.investigadores_service.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Taller 3 (DDD): el Value Object CorreoInstitucional hace imposible
 * construir un correo que no pertenezca al dominio @uptc.edu.co.
 */
class CorreoInstitucionalTest {

    @Test
    void aceptaUnCorreoDelDominioInstitucional() {
        CorreoInstitucional correo = new CorreoInstitucional("ana.torres@uptc.edu.co");

        assertEquals("ana.torres@uptc.edu.co", correo.valor());
    }

    @Test
    void rechazaUnCorreoDeOtroDominio() {
        assertThrows(IllegalArgumentException.class,
                () -> new CorreoInstitucional("carlos@gmail.com"));
    }

    @Test
    void rechazaUnCorreoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new CorreoInstitucional(null));
    }

    @Test
    void rechazaUnDominioParecidoPeroDistinto() {
        assertThrows(IllegalArgumentException.class,
                () -> new CorreoInstitucional("ana.torres@uptc.edu.co.falso.com"));
    }

    @Test
    void dosCorreosConElMismoValorSonIguales() {
        CorreoInstitucional uno = new CorreoInstitucional("ana.torres@uptc.edu.co");
        CorreoInstitucional otro = new CorreoInstitucional("ana.torres@uptc.edu.co");

        // Un Value Object se compara por su valor, no por su identidad.
        assertEquals(uno, otro);
        assertEquals(uno.hashCode(), otro.hashCode());
    }

    @Test
    void dosCorreosConValorDistintoNoSonIguales() {
        assertNotEquals(new CorreoInstitucional("ana.torres@uptc.edu.co"),
                new CorreoInstitucional("luis.mora@uptc.edu.co"));
    }

}