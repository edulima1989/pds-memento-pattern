const dishSelect = document.querySelector("#dish-select");
const addQuantity = document.querySelector("#add-quantity");
const orderLines = document.querySelector("#order-lines");
const notesInput = document.querySelector("#order-notes");
const feedback = document.querySelector("#feedback");
let notesDirty = false;
let flowTimer;
let flowRun = 0;

const participantDetails = {
  terminal: {
    name: "TerminalMesero · Client",
    detail: "Solicita guardar antes de cambiar el pedido. No necesita conocer las líneas ni las notas internas."
  },
  historial: {
    name: "HistorialPedido · Caretaker",
    detail: "Conserva las instantáneas y decide cuál se restaura. No lee ni modifica los datos guardados."
  },
  pedido: {
    name: "PedidoMesa · Originator",
    detail: "Es dueño del estado: crea el memento y sabe restaurarse a partir de él."
  },
  memento: {
    name: "MementoPedido · Memento",
    detail: "Transporta una copia de líneas y notas, junto con la descripción y la fecha del cambio."
  },
  linea: {
    name: "LineaPedido · Clase de apoyo",
    detail: "Representa un plato y su cantidad. Es inmutable: cambiar la cantidad produce una línea nueva."
  },
  plato: {
    name: "Plato · Clase de apoyo",
    detail: "Aporta nombre y precio a la línea del pedido; sus datos no cambian durante la operación."
  }
};

async function request(path, values) {
  const options = values
    ? { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: new URLSearchParams(values) }
    : { method: "GET" };
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "No se pudo completar la operacion.");
  return result;
}

function money(value) {
  return new Intl.NumberFormat("es-EC", { style: "currency", currency: "USD" }).format(value);
}

function createButton(text, className, action, title) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = className;
  button.textContent = text;
  if (title) button.setAttribute("aria-label", title);
  button.addEventListener("click", action);
  return button;
}

function renderMenu(menu) {
  const selected = dishSelect.value;
  dishSelect.replaceChildren();
  for (const dish of menu) {
    const option = document.createElement("option");
    option.value = dish.nombre;
    option.textContent = `${dish.nombre} · ${money(dish.precio)}${dish.vegetariano ? " · vegetariano" : ""}`;
    dishSelect.append(option);
  }
  if (menu.some((dish) => dish.nombre === selected)) dishSelect.value = selected;
}

function renderLines(lines) {
  orderLines.replaceChildren();
  if (lines.length === 0) {
    const empty = document.createElement("p");
    empty.className = "empty-state";
    empty.textContent = "El pedido está vacío. Agrega un plato para comenzar.";
    orderLines.append(empty);
    return;
  }

  for (const line of lines) {
    const row = document.createElement("article");
    row.className = "order-line";

    const description = document.createElement("div");
    description.className = "line-description";
    const name = document.createElement("strong");
    name.textContent = line.nombre;
    const unitPrice = document.createElement("span");
    unitPrice.textContent = `${money(line.precio)} por unidad`;
    description.append(name, unitPrice);

    const actions = document.createElement("div");
    actions.className = "line-actions";
    const quantity = document.createElement("input");
    quantity.className = "line-quantity";
    quantity.type = "number";
    quantity.min = "1";
    quantity.max = "99";
    quantity.value = line.cantidad;
    quantity.setAttribute("aria-label", `Cantidad de ${line.nombre}`);
    const update = createButton("Aplicar", "button", () => {
      perform(`/api/quantity`, { plato: line.nombre, cantidad: quantity.value });
    });
    const subtotal = document.createElement("span");
    subtotal.className = "line-subtotal";
    subtotal.textContent = money(line.subtotal);
    const remove = createButton("Quitar", "button line-remove", () => {
      perform("/api/remove", { plato: line.nombre });
    }, `Quitar ${line.nombre} del pedido`);

    actions.append(quantity, update, subtotal, remove);
    row.append(description, actions);
    orderLines.append(row);
  }
}

function renderStack(target, versions) {
  target.replaceChildren();
  if (versions.length === 0) {
    const empty = document.createElement("li");
    empty.className = "stack-empty";
    empty.textContent = "Sin versiones";
    target.append(empty);
    return;
  }

  versions.forEach((version, index) => {
    const item = document.createElement("li");
    item.className = "stack-item";
    item.textContent = `${index === 0 ? "Siguiente · " : ""}${version}`;
    target.append(item);
  });
}

function renderDiagram(flow) {
  const nodes = [...document.querySelectorAll(".diagram-node")];
  const edges = [...document.querySelectorAll(".uml-edge")];
  const progress = document.querySelector("#diagram-progress");
  const detail = document.querySelector("#diagram-detail");
  clearTimeout(flowTimer);
  const currentRun = ++flowRun;

  nodes.forEach((node) => {
    node.classList.remove("is-current", "is-complete");
    node.setAttribute("aria-pressed", "false");
  });
  edges.forEach((edge) => edge.classList.remove("is-current"));

  if (!flow || flow.length === 0) {
    progress.textContent = "Esperando una acción";
    return;
  }

  const completed = new Set();
  let step = 0;
  const showStep = () => {
    if (currentRun !== flowRun) return;
    const role = flow[step];
    const node = nodes.find((item) => item.dataset.role === role);
    if (!node || !participantDetails[role]) return;

    nodes.forEach((item) => {
      item.classList.toggle("is-complete", completed.has(item.dataset.role));
      item.classList.remove("is-current");
      item.setAttribute("aria-pressed", "false");
    });
    node.classList.add("is-current");
    node.setAttribute("aria-pressed", "true");
    progress.textContent = `Paso ${step + 1} de ${flow.length} · ${participantDetails[role].name.split(" · ")[0]}`;
    detail.textContent = participantDetails[role].detail;

    edges.forEach((edge) => edge.classList.remove("is-current"));
    const nextRole = flow[step + 1];
    const activeEdge = edges.find((edge) => edge.dataset.from === role && edge.dataset.to === nextRole);
    if (activeEdge) activeEdge.classList.add("is-current");
    completed.add(role);

    if (step < flow.length - 1) {
      flowTimer = setTimeout(() => {
        step += 1;
        showStep();
      }, 650);
    } else {
      flowTimer = setTimeout(() => {
        if (currentRun === flowRun) progress.textContent = "Recorrido completado";
      }, 650);
    }
  };
  showStep();
}

function render(state) {
  renderMenu(state.menu);
  renderLines(state.lineas);
  document.querySelector("#table-number").textContent = String(state.mesa).padStart(2, "0");
  document.querySelector("#order-total").textContent = money(state.total);
  document.querySelector("#line-count").textContent = `${state.lineas.length} ${state.lineas.length === 1 ? "plato" : "platos"}`;
  document.querySelector("#event-description").textContent = state.explicacion;
  renderDiagram(state.flujo);
  document.querySelector("#undo-count").textContent = state.deshacer.length;
  document.querySelector("#redo-count").textContent = state.rehacer.length;
  document.querySelector("#stack-count").textContent = `${state.deshacer.length} deshacer · ${state.rehacer.length} rehacer`;
  document.querySelector("#undo-button").disabled = state.deshacer.length === 0;
  document.querySelector("#redo-button").disabled = state.rehacer.length === 0;
  renderStack(document.querySelector("#undo-history"), state.deshacer);
  renderStack(document.querySelector("#redo-history"), state.rehacer);
  if (!notesDirty) notesInput.value = state.notas;
  feedback.textContent = "";
}

async function perform(path, values) {
  try {
    const state = await request(path, values);
    if (path === "/api/notes" || path === "/api/reset") {
      notesDirty = false;
      document.querySelector("#notes-hint").textContent = "Las notas también forman parte de la instantánea.";
    }
    render(state);
  } catch (error) {
    feedback.textContent = error.message;
  }
}

document.querySelector("#add-form").addEventListener("submit", (event) => {
  event.preventDefault();
  perform("/api/add", { plato: dishSelect.value, cantidad: addQuantity.value });
});

document.querySelector("#notes-form").addEventListener("submit", (event) => {
  event.preventDefault();
  perform("/api/notes", { notas: notesInput.value });
});

notesInput.addEventListener("input", () => {
  notesDirty = true;
  document.querySelector("#notes-hint").textContent = "Borrador sin guardar; no forma parte del pedido todavía.";
});
document.querySelector("#undo-button").addEventListener("click", () => perform("/api/undo", {}));
document.querySelector("#redo-button").addEventListener("click", () => perform("/api/redo", {}));
document.querySelector("#reset-button").addEventListener("click", () => perform("/api/reset", {}));

document.querySelectorAll(".diagram-node").forEach((node) => {
  node.addEventListener("click", () => {
    clearTimeout(flowTimer);
    flowRun += 1;
    document.querySelectorAll(".diagram-node").forEach((item) => {
      item.classList.remove("is-current");
      item.setAttribute("aria-pressed", "false");
    });
    document.querySelectorAll(".uml-edge").forEach((edge) => edge.classList.remove("is-current"));
    node.classList.add("is-current");
    node.setAttribute("aria-pressed", "true");
    document.querySelector("#diagram-progress").textContent = "Participante seleccionado";
    document.querySelector("#diagram-detail").textContent = participantDetails[node.dataset.role].detail;
  });
});

request("/api/state")
  .then(render)
  .catch((error) => { feedback.textContent = `No se pudo conectar con Java: ${error.message}`; });