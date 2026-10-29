package ec.restaurante.memento;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebServer {
    private static final String PREFIJO_DESHACER = "Deshacer: ";
    private static final String PREFIJO_REHACER = "Rehacer: ";

    private final HttpServer server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Plato> menu = new LinkedHashMap<>();

    private PedidoMesa pedido;
    private HistorialPedido historial;
    private TerminalMesero terminal;
    private String explicacion = "Agrega un plato para observar el flujo de una accion.";
    private List<String> flujo = List.of();

    public WebServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        cargarMenu();
        reiniciarPedido();
        registrarRutas();
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdown();
        }));
        System.out.println("Interfaz web disponible en http://localhost:" + server.getAddress().getPort());
    }

    private void cargarMenu() {
        menu.put("Sopa", new Plato("Sopa", 4.50, true));
        menu.put("Ensalada", new Plato("Ensalada", 6.25, true));
        menu.put("Filete", new Plato("Filete", 12.00, false));
    }

    private void reiniciarPedido() {
        pedido = new PedidoMesa(8);
        historial = new HistorialPedido();
        terminal = new TerminalMesero(pedido, historial);
    }

    private void registrarRutas() {
        server.createContext("/api/state", this::manejarEstado);
        server.createContext("/api/add", exchange -> manejarCambio(exchange, form -> agregar(form)));
        server.createContext("/api/remove", exchange -> manejarCambio(exchange, form -> quitar(form)));
        server.createContext("/api/quantity", exchange -> manejarCambio(exchange, form -> cambiarCantidad(form)));
        server.createContext("/api/notes", exchange -> manejarCambio(exchange, form -> cambiarNotas(form)));
        server.createContext("/api/undo", exchange -> manejarCambio(exchange, form -> deshacer()));
        server.createContext("/api/redo", exchange -> manejarCambio(exchange, form -> rehacer()));
        server.createContext("/api/reset", exchange -> manejarCambio(exchange, form -> reiniciar()));
        server.createContext("/", this::servirArchivo);
    }

    private void manejarEstado(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no permitido"));
            return;
        }
        responderJson(exchange, 200, crearEstado());
    }

    private void manejarCambio(HttpExchange exchange, Cambio cambio) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no permitido"));
            return;
        }
        try {
            cambio.ejecutar(leerFormulario(exchange));
            responderJson(exchange, 200, crearEstado());
        } catch (IllegalArgumentException error) {
            responderJson(exchange, 400, Map.of("error", error.getMessage()));
        } catch (Exception error) {
            responderJson(exchange, 500, Map.of("error", "No se pudo actualizar el pedido"));
        }
    }

    private void agregar(Map<String, String> form) {
        Plato plato = obtenerPlato(form);
        int cantidad = enteroRequerido(form, "cantidad");
        terminal.agregarPlato(plato, cantidad);
        registrarEvento("Se guarda el estado anterior antes de agregar " + plato.getNombre() + ".",
            "terminal", "historial", "pedido", "memento", "historial", "pedido", "linea", "plato");
    }

    private void quitar(Map<String, String> form) {
        String nombre = textoRequerido(form, "plato");
        obtenerPlato(form);
        validarPlatoEnPedido(nombre);
        terminal.quitarPlato(nombre);
        registrarEvento("Se guarda una instantanea antes de quitar " + nombre + " del pedido.",
            "terminal", "historial", "pedido", "memento", "historial", "pedido", "linea", "plato");
    }

    private void cambiarCantidad(Map<String, String> form) {
        String nombre = textoRequerido(form, "plato");
        obtenerPlato(form);
        validarPlatoEnPedido(nombre);
        int cantidad = enteroRequerido(form, "cantidad");
        terminal.cambiarCantidad(nombre, cantidad);
        registrarEvento("Se conserva la cantidad anterior antes de cambiar " + nombre + ".",
            "terminal", "historial", "pedido", "memento", "historial", "pedido", "linea", "plato");
    }

    private void cambiarNotas(Map<String, String> form) {
        String notas = form.getOrDefault("notas", "");
        if (notas.length() > 500) {
            throw new IllegalArgumentException("Las notas no pueden superar 500 caracteres");
        }
        terminal.cambiarNotas(notas);
        registrarEvento("El memento guarda las notas junto con las lineas del pedido.",
            "terminal", "historial", "pedido", "memento", "historial", "pedido");
    }

    private void deshacer() {
        terminal.deshacer();
        registrarEvento("El estado actual pasa a rehacer y PedidoMesa restaura la instantanea anterior.",
            "terminal", "historial", "pedido", "memento", "historial", "pedido");
    }

    private void rehacer() {
        terminal.rehacer();
        registrarEvento("HistorialPedido recupera una instantanea y PedidoMesa restaura ese estado.",
            "terminal", "historial", "pedido", "memento", "historial", "pedido");
    }

    private void reiniciar() {
        reiniciarPedido();
        explicacion = "Se creo un pedido nuevo y se vaciaron las dos pilas del historial.";
        flujo = List.of();
    }

    private void registrarEvento(String descripcion, String... participantes) {
        explicacion = descripcion;
        flujo = List.of(participantes);
    }

    private Plato obtenerPlato(Map<String, String> form) {
        String nombre = textoRequerido(form, "plato");
        Plato plato = menu.get(nombre);
        if (plato == null) {
            throw new IllegalArgumentException("Selecciona un plato valido del menu");
        }
        return plato;
    }

    private void validarPlatoEnPedido(String nombre) {
        boolean existe = pedido.guardar("Consulta").getLineas().stream()
                .anyMatch(linea -> linea.getPlato().getNombre().equals(nombre));
        if (!existe) {
            throw new IllegalArgumentException("Ese plato no esta en el pedido");
        }
    }

    private static String textoRequerido(Map<String, String> form, String campo) {
        String valor = form.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo " + campo);
        }
        return valor;
    }

    private static int enteroRequerido(Map<String, String> form, String campo) {
        try {
            int valor = Integer.parseInt(textoRequerido(form, campo));
            if (valor <= 0 || valor > 99) {
                throw new IllegalArgumentException("La cantidad debe estar entre 1 y 99");
            }
            return valor;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cantidad debe ser un numero entero");
        }
    }

    private Map<String, Object> crearEstado() {
        MementoPedido instantanea = pedido.guardar("Vista actual");
        List<LineaVista> lineas = instantanea.getLineas().stream()
                .map(linea -> new LineaVista(linea.getPlato().getNombre(), linea.getCantidad(),
                        linea.getPlato().getPrecio(), linea.calcularSubtotal()))
                .toList();
        List<String> versiones = historial.listarVersiones();
        List<String> deshacer = extraerVersiones(versiones, PREFIJO_DESHACER);
        List<String> rehacer = extraerVersiones(versiones, PREFIJO_REHACER);
        List<PlatoVista> platos = menu.values().stream()
                .map(plato -> new PlatoVista(plato.getNombre(), plato.getPrecio(), plato.isVegetariano()))
                .toList();

        return Map.of(
                "mesa", pedido.getNumeroMesa(),
                "lineas", lineas,
                "notas", instantanea.getNotas(),
                "total", pedido.calcularTotal(),
                "deshacer", deshacer,
                "rehacer", rehacer,
                "menu", platos,
                "explicacion", explicacion,
                "flujo", flujo);
    }

    private static List<String> extraerVersiones(List<String> versiones, String prefijo) {
        List<String> resultado = new ArrayList<>();
        for (String version : versiones) {
            if (version.startsWith(prefijo)) {
                resultado.add(version.substring(prefijo.length()));
            }
        }
        return resultado;
    }

    private static Map<String, String> leerFormulario(HttpExchange exchange) throws IOException {
        String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> valores = new LinkedHashMap<>();
        if (cuerpo.isBlank()) {
            return valores;
        }
        for (String parametro : cuerpo.split("&")) {
            String[] partes = parametro.split("=", 2);
            String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            valores.put(clave, valor);
        }
        return valores;
    }

    private void servirArchivo(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            responderTexto(exchange, 405, "Metodo no permitido", "text/plain; charset=utf-8");
            return;
        }
        String ruta = exchange.getRequestURI().getPath();
        if (ruta.contains("..")) {
            responderTexto(exchange, 400, "Ruta no valida", "text/plain; charset=utf-8");
            return;
        }
        String recurso = "/static" + ("/".equals(ruta) ? "/index.html" : ruta);
        try (InputStream contenido = WebServer.class.getResourceAsStream(recurso)) {
            if (contenido == null) {
                responderTexto(exchange, 404, "No encontrado", "text/plain; charset=utf-8");
                return;
            }
            String tipo = switch (ruta.substring(ruta.lastIndexOf('/') + 1)) {
                case "app.css" -> "text/css; charset=utf-8";
                case "app.js" -> "text/javascript; charset=utf-8";
                default -> "text/html; charset=utf-8";
            };
            responder(exchange, 200, tipo, contenido.readAllBytes());
        }
    }

    private void responderJson(HttpExchange exchange, int codigo, Object respuesta) throws IOException {
        responder(exchange, codigo, "application/json; charset=utf-8", objectMapper.writeValueAsBytes(respuesta));
    }

    private static void responderTexto(HttpExchange exchange, int codigo, String texto, String tipo) throws IOException {
        responder(exchange, codigo, tipo, texto.getBytes(StandardCharsets.UTF_8));
    }

    private static void responder(HttpExchange exchange, int codigo, String tipo, byte[] contenido) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(codigo, contenido.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(contenido);
        }
    }

    @FunctionalInterface
    private interface Cambio {
        void ejecutar(Map<String, String> formulario) throws Exception;
    }

    private record LineaVista(String nombre, int cantidad, double precio, double subtotal) {
    }

    private record PlatoVista(String nombre, double precio, boolean vegetariano) {
    }
}