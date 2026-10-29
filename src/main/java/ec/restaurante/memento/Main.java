package ec.restaurante.memento;

import java.util.Locale;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Plato sopa = new Plato("Sopa", 4.50, true);
        Plato filete = new Plato("Filete", 12.00, false);
        PedidoMesa pedido = new PedidoMesa(8);
        HistorialPedido historial = new HistorialPedido();
        TerminalMesero terminal = new TerminalMesero(pedido, historial);

        mostrarPedido("Pedido inicial", pedido);

        terminal.agregarPlato(sopa, 2);
        terminal.cambiarNotas("Sin cebolla");
        mostrarPedido("Despues de agregar sopa y notas", pedido);

        terminal.agregarPlato(filete, 1);
        mostrarPedido("Despues de agregar filete", pedido);

        terminal.deshacer();
        mostrarPedido("Deshacer: quitar el filete", pedido);

        terminal.deshacer();
        mostrarPedido("Deshacer: quitar las notas", pedido);

        terminal.deshacer();
        mostrarPedido("Deshacer: volver al pedido vacio", pedido);

        terminal.rehacer();
        mostrarPedido("Rehacer: recuperar la sopa", pedido);

        terminal.rehacer();
        mostrarPedido("Rehacer: recuperar las notas", pedido);

        terminal.rehacer();
        mostrarPedido("Rehacer: recuperar el filete", pedido);

        System.out.println("\nVersiones disponibles:");
        historial.listarVersiones().forEach(version -> System.out.println("- " + version));
    }

    private static void mostrarPedido(String titulo, PedidoMesa pedido) {
        MementoPedido estado = pedido.guardar("Estado mostrado en la demostracion");
        System.out.printf("%n%s | Mesa %d | Total: $%.2f%n",
                titulo, pedido.getNumeroMesa(), pedido.calcularTotal());

        if (estado.getLineas().isEmpty()) {
            System.out.println("  (sin platos)");
        } else {
            estado.getLineas().forEach(linea -> System.out.printf(Locale.ROOT,
                    "  - %s x%d: $%.2f%n",
                    linea.getPlato().getNombre(), linea.getCantidad(), linea.calcularSubtotal()));
        }

        String notas = estado.getNotas().isBlank() ? "(ninguna)" : estado.getNotas();
        System.out.println("  Notas: " + notas);
    }
}