package ec.restaurante.memento;

import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class HistorialPedido {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Deque<MementoPedido> pilaDeshacer = new ArrayDeque<>();
    private final Deque<MementoPedido> pilaRehacer = new ArrayDeque<>();

    public void guardar(PedidoMesa pedido, String descripcion) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio");
        pilaDeshacer.push(pedido.guardar(descripcion));
        pilaRehacer.clear();
    }

    public boolean deshacer(PedidoMesa pedido) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio");
        if (pilaDeshacer.isEmpty()) {
            return false;
        }
        pilaRehacer.push(pedido.guardar("Estado posterior al cambio"));
        pedido.restaurar(pilaDeshacer.pop());
        return true;
    }

    public boolean rehacer(PedidoMesa pedido) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio");
        if (pilaRehacer.isEmpty()) {
            return false;
        }
        pilaDeshacer.push(pedido.guardar("Estado previo al rehacer"));
        pedido.restaurar(pilaRehacer.pop());
        return true;
    }

    public List<String> listarVersiones() {
        List<String> versiones = new ArrayList<>();
        agregarVersiones(versiones, "Deshacer", pilaDeshacer);
        agregarVersiones(versiones, "Rehacer", pilaRehacer);
        return List.copyOf(versiones);
    }

    private static void agregarVersiones(List<String> versiones, String tipo, Deque<MementoPedido> pila) {
        for (MementoPedido memento : pila) {
            versiones.add(tipo + ": " + memento.getDescripcion() + " ("
                    + memento.getFechaHora().format(FORMATO_FECHA) + ")");
        }
    }
}