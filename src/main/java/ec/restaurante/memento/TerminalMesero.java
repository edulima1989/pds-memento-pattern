package ec.restaurante.memento;

import java.util.Objects;

public final class TerminalMesero {
    private final PedidoMesa pedido;
    private final HistorialPedido historial;

    public TerminalMesero(int numeroMesa) {
        this(new PedidoMesa(numeroMesa), new HistorialPedido());
    }

    public TerminalMesero(PedidoMesa pedido, HistorialPedido historial) {
        this.pedido = Objects.requireNonNull(pedido, "El pedido es obligatorio");
        this.historial = Objects.requireNonNull(historial, "El historial es obligatorio");
    }

    public void agregarPlato(Plato plato, int cantidad) {
        historial.guardar(pedido, "Agregar " + plato.getNombre());
        pedido.agregarPlato(plato, cantidad);
    }

    public void quitarPlato(String nombrePlato) {
        historial.guardar(pedido, "Quitar " + nombrePlato);
        pedido.quitarPlato(nombrePlato);
    }

    public void cambiarCantidad(String nombrePlato, int cantidad) {
        historial.guardar(pedido, "Cambiar cantidad de " + nombrePlato);
        pedido.cambiarCantidad(nombrePlato, cantidad);
    }

    public void cambiarNotas(String notas) {
        historial.guardar(pedido, "Cambiar notas");
        pedido.cambiarNotas(notas);
    }

    public void deshacer() {
        historial.deshacer(pedido);
    }

    public void rehacer() {
        historial.rehacer(pedido);
    }
}