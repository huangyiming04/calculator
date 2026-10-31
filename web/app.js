// The page never computes anything itself: it builds a request, sends it to
// nginx with a relative path, and renders what the backend answers. A relative
// path keeps the page on its own origin, so no CORS setup is needed.

const expressionEl = document.getElementById("expression");
const resultEl = document.getElementById("result");
const statusEl = document.getElementById("status");
const statusText = document.getElementById("status-text");
const traceUrl = document.getElementById("trace-url");
const traceResponse = document.getElementById("trace-response");

// Two operands and one operator, e.g. 12+7, -5*3, 1.5/2.
const EXPRESSION = /^([+-]?(?:\d+\.?\d*|\.\d+))([+\-*/])([+-]?(?:\d+\.?\d*|\.\d+))$/;

let expression = "";
let busy = false;

function render() {
  expressionEl.textContent = expression || "0";
}

function showResult(value) {
  resultEl.textContent = "= " + value;
  resultEl.classList.remove("is-error");
  resultEl.classList.add("is-ok");
}

function showError(message) {
  resultEl.textContent = message;
  resultEl.classList.remove("is-ok");
  resultEl.classList.add("is-error");
}

function resetResult() {
  resultEl.textContent = "";
  resultEl.className = "result";
}

function clearAll() {
  expression = "";
  resetResult();
  render();
}

function backspace() {
  expression = expression.slice(0, -1);
  resetResult();
  render();
}

function append(value) {
  if (expression.length >= 24) {
    return;
  }
  expression += value;
  resetResult();
  render();
}

function formatNumber(value) {
  return Number.isInteger(value) ? String(value) : String(Number(value.toFixed(10)));
}

async function calculate() {
  const match = expression.match(EXPRESSION);
  if (!match) {
    showError("Type something like 12+7");
    return;
  }

  const [, a, op, b] = match;
  // encodeURIComponent is what turns + into %2B, * into %2A and / into %2F.
  // Without it the plus would reach the server as a space.
  const url = `/api/calc?a=${encodeURIComponent(a)}&op=${encodeURIComponent(op)}&b=${encodeURIComponent(b)}`;
  traceUrl.textContent = url;

  busy = true;
  try {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    const data = await response.json().catch(() => ({}));
    traceResponse.textContent = `${response.status} ${JSON.stringify(data)}`;

    if (!response.ok) {
      showError(data.error || `HTTP ${response.status}`);
      return;
    }
    showResult(formatNumber(data.result));
  } catch (error) {
    traceResponse.textContent = String(error);
    showError("Backend unreachable");
  } finally {
    busy = false;
  }
}

// Proves the whole chain works: browser -> nginx -> backend -> nginx -> browser.
async function checkHealth() {
  try {
    const response = await fetch("/api/health", { cache: "no-store" });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    statusEl.className = "status is-ok";
    statusText.textContent = `backend ${data.status}`;
  } catch {
    statusEl.className = "status is-bad";
    statusText.textContent = "backend unreachable";
  }
}

document.querySelector(".keys").addEventListener("click", (event) => {
  const key = event.target.closest("button");
  if (!key) {
    return;
  }
  if (key.dataset.digit !== undefined) {
    append(key.dataset.digit);
    return;
  }
  if (key.dataset.op !== undefined) {
    append(key.dataset.op);
    return;
  }
  switch (key.dataset.action) {
    case "clear":
      clearAll();
      break;
    case "back":
      backspace();
      break;
    case "equals":
      if (!busy) {
        calculate();
      }
      break;
    default:
      break;
  }
});

window.addEventListener("keydown", (event) => {
  const key = event.key;
  if (/^[0-9.]$/.test(key)) {
    append(key);
    event.preventDefault();
  } else if (["+", "-", "*", "/"].includes(key)) {
    append(key);
    event.preventDefault();
  } else if (key === "Enter" || key === "=") {
    if (!busy) {
      calculate();
    }
    event.preventDefault();
  } else if (key === "Backspace") {
    backspace();
    event.preventDefault();
  } else if (key === "Escape" || key.toLowerCase() === "c") {
    clearAll();
    event.preventDefault();
  }
});

render();
checkHealth();
setInterval(checkHealth, 15000);
