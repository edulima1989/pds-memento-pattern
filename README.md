# Patrón Memento: pedidos de restaurante

Implementación del patrón **Memento** para permitir que la terminal de un mesero deshaga y rehaga cambios en el pedido de una mesa sin que la terminal gestione directamente sus líneas ni sus notas.

## Requisitos

- Java Development Kit (JDK) 21
- Apache Maven 3.8 o posterior

Comprueba las versiones instaladas con:

```bash
java --version
mvn --version
```

## Implementación

Las clases se encuentran en `ec.restaurante.memento` y corresponden a estos participantes:

| Clase | Responsabilidad |
| --- | --- |
| `PedidoMesa` | Originator: administra el número de mesa, las líneas y las notas. Crea mementos con `guardar()` y restaura el pedido con `restaurar()`. |
| `MementoPedido` | Memento: conserva una instantánea de las líneas y las notas, junto con su descripción y fecha. La lista se copia con `List.copyOf`, y sus elementos (`LineaPedido` y `Plato`) son inmutables. |
| `HistorialPedido` | Caretaker: mantiene pilas separadas para deshacer y rehacer. No modifica directamente el estado del pedido. |
| `TerminalMesero` | Client: guarda antes de cada cambio y delega las órdenes de deshacer y rehacer al historial. |
| `LineaPedido` | Representa un plato y su cantidad; `conCantidad()` devuelve una nueva línea. |
| `Plato` | Representa un plato del menú con nombre, precio e indicador vegetariano. |

Al agregar un plato que ya existe en el pedido, se acumula su cantidad. Las cantidades deben ser mayores que cero. `calcularTotal()` suma los subtotales de las líneas.

Los mementos exponen getters públicos de estado según el UML. La colección de líneas devuelta es inmodificable, y el pedido activo no ofrece getters públicos para sus líneas ni sus notas.

## Probar

Desde la raíz del proyecto, ejecuta:

```bash
mvn clean test
```

Maven compila el proyecto para Java 21 y ejecuta las pruebas con JUnit 5. La suite comprueba:

- Restaurar las líneas y notas al deshacer y rehacer.
- Deshacer y rehacer varios cambios consecutivos desde `TerminalMesero`.
- Descartar el historial de rehacer al guardar una nueva modificación.
- Impedir la modificación de la lista de líneas de un memento.

## Ejecutar la interfaz web

Las pruebas se ejecutan por separado con `mvn clean test`. Para iniciar solamente la interfaz web, desde la raíz del proyecto ejecuta:

```bash
mvn compile
mvn exec:java -Dexec.mainClass=ec.restaurante.memento.Main
```

Abre [http://localhost:8080](http://localhost:8080). La página se conecta al modelo Java: puedes agregar y quitar platos, cambiar cantidades y notas, deshacer, rehacer o reiniciar el ejercicio. El diagrama interactivo resalta en secuencia los participantes que intervienen en cada operación; también puedes seleccionar un participante para consultar su responsabilidad. El panel muestra las dos pilas del historial.

Para usar otro puerto, define la variable `PORT` antes de iniciar Maven. En Windows CMD, por ejemplo:

```cmd
set PORT=8081 && mvn exec:java -Dexec.mainClass=ec.restaurante.memento.Main
```

## Estructura

```text
src/
├── main/java/ec/restaurante/memento/  # Dominio Memento y servidor web
├── main/resources/static/             # Interfaz web
└── test/java/ec/restaurante/memento/  # Pruebas unitarias
```