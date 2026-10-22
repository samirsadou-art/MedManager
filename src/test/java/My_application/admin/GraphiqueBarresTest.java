package My_application.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.service.StatistiquesService.Barre;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import org.junit.Test;

public class GraphiqueBarresTest {

    @Test
    public void graduationsRondes() {
        assertEquals(1, GraphiqueBarres.graduation(3));
        assertEquals(2, GraphiqueBarres.graduation(7));
        assertEquals(5, GraphiqueBarres.graduation(17));
        assertEquals(10, GraphiqueBarres.graduation(31));
        assertEquals(50, GraphiqueBarres.graduation(180));
    }

    @Test
    public void barreSurvoleeEtInfoBulle() {
        GraphiqueBarres g = new GraphiqueBarres();
        g.setSize(244, 200);
        assertEquals(-1, g.indexA(100));
        g.setBarres(Arrays.asList(new Barre("1 oct.", "Jeudi 1 octobre 2026", 4),
                new Barre("2 oct.", "Vendredi 2 octobre 2026", 0)), "consultations");
        // Zone de tracé : de 44 à 236, deux colonnes de 96 px.
        assertEquals(0, g.indexA(50));
        assertEquals(1, g.indexA(200));
        assertEquals(-1, g.indexA(10));
        String bulle = g.getToolTipText(new MouseEvent(g, MouseEvent.MOUSE_MOVED, 0, 0, 60, 50, 0, false));
        assertTrue(bulle, bulle.contains("Jeudi 1 octobre 2026") && bulle.contains("4 consultations"));
        assertNull(g.getToolTipText(new MouseEvent(g, MouseEvent.MOUSE_MOVED, 0, 0, 5, 50, 0, false)));
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(244, 200,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        g.paint(im.getGraphics());
    }
}
