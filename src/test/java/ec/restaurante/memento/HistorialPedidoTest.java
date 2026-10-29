package ec.restaurante.memento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HistorialPedidoTest {
    private final Plato sopa = new Plato("Sopa", 4.50, true);

    @Test
    void deshaceYRehaceElEstadoCompletoDelPedido() {
        PedidoMesa pedido = new PedidoMesa(4);
        HistorialPedido historial = new HistorialPedido();

        historial.guardar(pedido, "Pedido inicial");
        pedido.agregarPlato(sopa, 2);
        pedido.cambiarNotas("Sin cebolla");

        assertTrue(historial.deshacer(pedido));
        MementoPedido estadoDeshecho = pedido.guardar("Estado deshecho");
        assertTrue(estadoDeshecho.getLineas().isEmpty());
        assertEquals("", estadoDeshecho.getNotas());

        assertTrue(historial.rehacer(pedido));
        MementoPedido estadoRehecho = pedido.guardar("Estado rehecho");
        assertEquals(2, estadoRehecho.getLineas().get(0).getCantidad());
        assertEquals("Sin cebolla", estadoRehecho.getNotas());
    }

    @Test
    void terminalPuedeDeshacerYRehacerVariosPasos() {
        PedidoMesa pedido = new PedidoMesa(4);
        TerminalMesero terminal = new TerminalMesero(pedido, new HistorialPedido());

        terminal.agregarPlato(sopa, 1);
        terminal.cambiarNotas("Sin cebolla");
        terminal.cambiarCantidad("Sopa", 2);

        terminal.deshacer();
        MementoPedido primerDeshacer = pedido.guardar("Primer deshacer");
        assertEquals(1, primerDeshacer.getLineas().get(0).getCantidad());
        assertEquals("Sin cebolla", primerDeshacer.getNotas());

        terminal.deshacer();
        MementoPedido segundoDeshacer = pedido.guardar("Segundo deshacer");
        assertEquals(1, segundoDeshacer.getLineas().get(0).getCantidad());
        assertEquals("", segundoDeshacer.getNotas());

        terminal.rehacer();
        MementoPedido primerRehacer = pedido.guardar("Primer rehacer");
        assertEquals("Sin cebolla", primerRehacer.getNotas());

        terminal.rehacer();
        MementoPedido segundoRehacer = pedido.guardar("Segundo rehacer");
        assertEquals(2, segundoRehacer.getLineas().get(0).getCantidad());
    }

    @Test
    void nuevaModificacionDescartaElHistorialDeRehacer() {
        PedidoMesa pedido = new PedidoMesa(4);
        HistorialPedido historial = new HistorialPedido();

        historial.guardar(pedido, "Pedido inicial");
        pedido.agregarPlato(sopa, 1);
        assertTrue(historial.deshacer(pedido));

        historial.guardar(pedido, "Nueva rama");
        pedido.cambiarNotas("Sin sal");

        assertFalse(historial.rehacer(pedido));
        assertEquals("Sin sal", pedido.guardar("Estado actual").getNotas());
    }

    @Test
    void mementoExponeUnaListaInmodificable() {
        PedidoMesa pedido = new PedidoMesa(4);
        pedido.agregarPlato(sopa, 1);
        MementoPedido memento = pedido.guardar("Pedido con sopa");

        assertThrows(UnsupportedOperationException.class, () -> memento.getLineas().clear());
        assertEquals("Pedido con sopa", memento.getDescripcion());
        assertEquals(1, memento.getLineas().size());
    }
}