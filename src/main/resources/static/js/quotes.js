document.addEventListener("DOMContentLoaded", function () {
  const previewContainer = document.getElementById("favoriteQuotesPreview");
  const openModalButton = document.getElementById("openQuotesModal");
  const addQuoteButton = document.getElementById("addFavoriteQuoteButton");
  const modal = document.getElementById("favoriteQuotesModal");
  const modalOverlay = document.getElementById("favoriteQuotesOverlay");
  const closeButton = document.getElementById("closeQuotesModal");
  const listContainer = document.getElementById("favoriteQuotesList");
  const form = document.getElementById("favoriteQuoteForm");
  const quoteIdInput = document.getElementById("favoriteQuoteId");
  const quoteTextInput = document.getElementById("favoriteQuoteText");
  const quoteWorkInput = document.getElementById("favoriteQuoteWork");
  const submitButton = document.getElementById("favoriteQuoteSubmit");
  const cancelButton = document.getElementById("favoriteQuoteCancel");
  const modalTitle = modal.querySelector(".favorite-quotes-modal__header h3");

  if (!previewContainer || !modal || !form || !listContainer) {
    return;
  }

  let quotes = [];
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";

  loadQuotes();

  openModalButton?.addEventListener("click", function () {
    openQuotesModal("list");
  });

  addQuoteButton?.addEventListener("click", function () {
    resetQuoteForm();
    openQuotesModal("add");
  });

  closeButton?.addEventListener("click", closeQuotesModal);
  modalOverlay?.addEventListener("click", closeQuotesModal);
  cancelButton?.addEventListener("click", function () {
    resetQuoteForm();
    if (modal.style.display === "block") {
      setModalMode("list");
    }
  });

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    saveQuote();
  });

  function loadQuotes() {
    fetch("/favoriteQuotes")
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        quotes = Array.isArray(data.quotes) ? data.quotes : [];
        renderPreview();
        renderQuotesList();
      })
      .catch(error => {
        console.error("No se pudieron cargar las citas favoritas:", error);
      });
  }

  function renderPreview() {
    previewContainer.innerHTML = "";
    const previewQuotes = quotes.slice(0, 2);

    if (previewQuotes.length === 0) {
      previewContainer.innerHTML = '<p class="favorite-quotes-empty">Aun no tienes citas favoritas guardadas.</p>';
      return;
    }

    previewQuotes.forEach(quote => {
      const article = document.createElement("article");
      article.className = "favorite-quote-preview";
      article.innerHTML = `
        <p class="favorite-quote-preview__text"><em>"${escapeHtml(quote.texto)}"</em></p>
        <p class="favorite-quote-preview__work">- ${escapeHtml(quote.obra)}</p>
      `;
      previewContainer.appendChild(article);
    });
  }

  function renderQuotesList() {
    listContainer.innerHTML = "";

    if (quotes.length === 0) {
      listContainer.innerHTML = '<p class="favorite-quotes-empty">No hay citas guardadas todavia.</p>';
      return;
    }

    quotes.forEach(quote => {
      const item = document.createElement("article");
      item.className = "favorite-quote-card";
      item.innerHTML = `
        <p class="favorite-quote-card__text">"${escapeHtml(quote.texto)}"</p>
        <p class="favorite-quote-card__work">- ${escapeHtml(quote.obra)}</p>
        <div class="favorite-quote-card__actions">
          <button type="button" class="favorite-quote-card__button" data-action="edit">Editar</button>
          <button type="button" class="favorite-quote-card__button favorite-quote-card__button--danger" data-action="delete">Eliminar</button>
        </div>
      `;

      item.querySelector('[data-action="edit"]').addEventListener("click", function () {
        quoteIdInput.value = quote.idCita;
        quoteTextInput.value = quote.texto;
        quoteWorkInput.value = quote.obra;
        submitButton.textContent = "Guardar cambios";
        openQuotesModal("edit");
        quoteTextInput.focus();
      });

      item.querySelector('[data-action="delete"]').addEventListener("click", function () {
        deleteQuote(quote.idCita);
      });

      listContainer.appendChild(item);
    });
  }

  function saveQuote() {
    const texto = quoteTextInput.value.trim();
    const obra = quoteWorkInput.value.trim();

    if (!texto || !obra) {
      showQuotesAlert("Atencion", "Escribe la cita y la obra.", "warning");
      return;
    }

    const isEditing = Boolean(quoteIdInput.value);
    const endpoint = isEditing ? "/favoriteQuotes/update" : "/favoriteQuotes";
    const body = new URLSearchParams();
    body.set("texto", texto);
    body.set("obra", obra);
    if (isEditing) {
      body.set("idCita", quoteIdInput.value);
    }

    fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        [csrfHeader]: csrfToken
      },
      body: body.toString()
    })
      .then(async response => {
        let data = {};
        try {
          data = await response.json();
        } catch (error) {
          data = {};
        }

        if (!response.ok) {
          throw new Error(data.message || `HTTP ${response.status}`);
        }

        return data;
      })
      .then(data => {
        if (!data.success) {
          throw new Error(data.message || "No se pudo guardar la cita.");
        }

        quotes = Array.isArray(data.quotes) ? data.quotes : [];
        renderPreview();
        renderQuotesList();
        resetQuoteForm();
        showQuotesAlert("Hecho", isEditing ? "Cita actualizada correctamente." : "Cita añadida correctamente.", "success");
        setModalMode(isEditing ? "list" : "add");
      })
      .catch(error => {
        showQuotesAlert("Error", error.message || "No se pudo guardar la cita.", "error");
      });
  }

  function deleteQuote(idCita) {
    const body = new URLSearchParams();
    body.set("idCita", idCita);

    fetch("/favoriteQuotes/delete", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        [csrfHeader]: csrfToken
      },
      body: body.toString()
    })
      .then(async response => {
        let data = {};
        try {
          data = await response.json();
        } catch (error) {
          data = {};
        }

        if (!response.ok) {
          throw new Error(data.message || `HTTP ${response.status}`);
        }

        return data;
      })
      .then(data => {
        if (!data.success) {
          throw new Error(data.message || "No se pudo eliminar la cita.");
        }

        quotes = Array.isArray(data.quotes) ? data.quotes : [];
        renderPreview();
        renderQuotesList();
        resetQuoteForm();
        setModalMode("list");
      })
      .catch(error => {
        showQuotesAlert("Error", error.message || "No se pudo eliminar la cita.", "error");
      });
  }

  function resetQuoteForm() {
    quoteIdInput.value = "";
    quoteTextInput.value = "";
    quoteWorkInput.value = "";
    submitButton.textContent = "Agregar cita";
  }

  function openQuotesModal(mode) {
    modal.style.display = "block";
    modalOverlay.style.display = "block";
    setModalMode(mode);
    if (mode !== "list") {
      quoteTextInput.focus();
    }
  }

  function closeQuotesModal() {
    modal.style.display = "none";
    modalOverlay.style.display = "none";
    setModalMode("list");
    resetQuoteForm();
  }

  function setModalMode(mode) {
    modal.classList.toggle("favorite-quotes-modal--add", mode === "add");
    modal.classList.toggle("favorite-quotes-modal--list", mode === "list");
    modal.classList.toggle("favorite-quotes-modal--edit", mode === "edit");

    if (modalTitle) {
      modalTitle.textContent =
        mode === "add"
          ? "Añadir cita favorita"
          : mode === "edit"
            ? "Editar cita favorita"
            : "Mis citas favoritas";
    }
  }

  function showQuotesAlert(title, text, icon) {
    if (typeof Swal !== "undefined") {
      Swal.fire({
        title,
        text,
        icon,
        confirmButtonText: "OK",
        heightAuto: false,
        customClass: {
          popup: "malva-popup",
          confirmButton: "malva-confirm-button"
        }
      });
      return;
    }

    alert(text);
  }

  function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value || "";
    return div.innerHTML;
  }
});
