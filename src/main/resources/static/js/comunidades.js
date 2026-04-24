document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("readingJournalForm");
  if (!form) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
  const saveStatus = document.getElementById("journalSaveStatus");
  const entriesList = document.getElementById("journalEntriesList");
  const entriesCount = document.getElementById("journalEntriesCount");
  const coverInput = document.getElementById("journalCover");
  const coverPreview = document.getElementById("coverPreview");
  const saveButton = document.getElementById("saveJournalEntry");
  const openModalButton = document.getElementById("openJournalReviewsModal");
  const reviewModal = document.getElementById("journalReviewModal");
  const modalList = document.getElementById("journalModalList");
  const modalDetail = document.getElementById("journalModalDetail");
  const closeModalButton = document.getElementById("closeJournalModal");

  const ratingInputs = {
    general: document.getElementById("generalRating"),
    romance: document.getElementById("romanceRating"),
    spice: document.getElementById("spiceRating"),
    sadness: document.getElementById("sadnessRating"),
    plot: document.getElementById("plotRating"),
    characters: document.getElementById("charactersRating"),
    style: document.getElementById("styleRating"),
    ending: document.getElementById("endingRating")
  };

  let currentCoverImage = "";
  let currentEntryId = null;
  let entriesCache = [];
  let modalSelectedEntryId = null;

  function setSaveStatus(text) {
    if (saveStatus) {
      saveStatus.textContent = text;
    }
  }

  function getHeaders() {
    const headers = {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
    };

    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    return headers;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function setCoverPreview(imageValue) {
    const hasImage = Boolean(imageValue);
    coverPreview.style.backgroundImage = hasImage
      ? `linear-gradient(rgba(0,0,0,0.05), rgba(0,0,0,0.05)), url("${imageValue}")`
      : "";
    coverPreview.classList.toggle("has-image", hasImage);
  }

  function paintRating(groupName, value) {
    const group = document.querySelector(`[data-rating-group="${groupName}"]`);
    if (!group) {
      return;
    }

    group.querySelectorAll("button").forEach((button) => {
      button.classList.toggle("is-active", Number(button.dataset.value) <= value);
    });
  }

  function paintAllRatings() {
    Object.entries(ratingInputs).forEach(([groupName, input]) => {
      paintRating(groupName, Number(input.value || 0));
    });
  }

  function resetForm(statusText = "Ficha vacía") {
    form.reset();
    currentEntryId = null;
    document.getElementById("journalId").value = "";
    currentCoverImage = "";
    setCoverPreview("");

    Object.values(ratingInputs).forEach((input) => {
      input.value = "0";
    });

    const defaultFormat = document.querySelector('input[name="formato"][value="Papel"]');
    if (defaultFormat) {
      defaultFormat.checked = true;
    }

    paintAllRatings();
    setActiveEntryCard(null);
    setSaveStatus(statusText);
  }

  function serializeForm() {
    const params = new URLSearchParams();
    params.set("idJournal", document.getElementById("journalId").value || "");
    params.set("titulo", document.getElementById("titulo").value.trim());
    params.set("autor", document.getElementById("autor").value.trim());
    params.set("paginas", document.getElementById("paginas").value || "");
    params.set("formato", document.querySelector('input[name="formato"]:checked')?.value || "");
    params.set("fechaInicio", document.getElementById("fechaInicio").value || "");
    params.set("fechaFin", document.getElementById("fechaFin").value || "");
    params.set("feelingOption", document.querySelector('input[name="feelingOption"]:checked')?.value || "");
    params.set("generalRating", ratingInputs.general.value || "0");
    params.set("romanceRating", ratingInputs.romance.value || "0");
    params.set("spiceRating", ratingInputs.spice.value || "0");
    params.set("sadnessRating", ratingInputs.sadness.value || "0");
    params.set("plotRating", ratingInputs.plot.value || "0");
    params.set("charactersRating", ratingInputs.characters.value || "0");
    params.set("styleRating", ratingInputs.style.value || "0");
    params.set("endingRating", ratingInputs.ending.value || "0");
    params.set("bestCharacter", document.getElementById("bestCharacter").value.trim());
    params.set("worstCharacter", document.getElementById("worstCharacter").value.trim());
    params.set("reflexionesFinales", document.getElementById("reflexionesFinales").value.trim());
    params.set("coverImage", currentCoverImage || "");
    return params;
  }

  function hasContent() {
    const data = serializeForm();
    return Array.from(data.entries()).some(([key, value]) => {
      if (key === "idJournal" || key === "formato") {
        return false;
      }
      return value.trim() !== "" && value !== "0";
    });
  }

  function fillForm(entry) {
    resetForm("Reseña cargada");
    currentEntryId = entry.idJournal || null;
    document.getElementById("journalId").value = currentEntryId || "";
    document.getElementById("titulo").value = entry.titulo || "";
    document.getElementById("autor").value = entry.autor || "";
    document.getElementById("paginas").value = entry.paginas || "";
    document.getElementById("fechaInicio").value = entry.fechaInicio || "";
    document.getElementById("fechaFin").value = entry.fechaFin || "";
    document.getElementById("bestCharacter").value = entry.bestCharacter || "";
    document.getElementById("worstCharacter").value = entry.worstCharacter || "";
    document.getElementById("reflexionesFinales").value = entry.reflexionesFinales || "";
    currentCoverImage = entry.coverImage || "";
    setCoverPreview(currentCoverImage);

    const formatValue = entry.formato || "Papel";
    const formatRadio = Array.from(document.querySelectorAll('input[name="formato"]'))
      .find((input) => input.value === formatValue);
    if (formatRadio) {
      formatRadio.checked = true;
    }

    const feelingRadio = Array.from(document.querySelectorAll('input[name="feelingOption"]'))
      .find((input) => input.value === (entry.feelingOption || ""));
    if (feelingRadio) {
      feelingRadio.checked = true;
    }

    ratingInputs.general.value = entry.generalRating || 0;
    ratingInputs.romance.value = entry.romanceRating || 0;
    ratingInputs.spice.value = entry.spiceRating || 0;
    ratingInputs.sadness.value = entry.sadnessRating || 0;
    ratingInputs.plot.value = entry.plotRating || 0;
    ratingInputs.characters.value = entry.charactersRating || 0;
    ratingInputs.style.value = entry.styleRating || 0;
    ratingInputs.ending.value = entry.endingRating || 0;

    paintAllRatings();
    setActiveEntryCard(currentEntryId);
    setSaveStatus("Reseña lista para modificar");
  }

  function buildSymbolMarkup(type, value) {
    const total = 5;
    const active = Number(value || 0);
    let symbol = "&#9733;";
    let inactiveSymbol = "&#9734;";

    if (type === "romance") {
      symbol = "&#9829;";
      inactiveSymbol = "&#9825;";
    } else if (type === "spice") {
      symbol = "&#127798;";
      inactiveSymbol = "&#127798;";
    } else if (type === "sadness") {
      symbol = '<i class="fa-solid fa-droplet"></i>';
      inactiveSymbol = '<i class="fa-solid fa-droplet"></i>';
    }

    let markup = "";
    for (let i = 1; i <= total; i += 1) {
      const isActive = i <= active;
      const content = isActive ? symbol : inactiveSymbol;
      markup += `<span class="journal-modal-detail__symbol${isActive ? "" : " is-inactive"}">${content}</span>`;
    }
    return markup;
  }

  function renderModalDetail(entry) {
    if (!modalDetail) {
      return;
    }

    if (!entry) {
      modalDetail.innerHTML = '<p class="journal-empty-state">Selecciona una reseña para ver todos sus detalles.</p>';
      return;
    }

    modalDetail.innerHTML = `
      <div class="journal-modal-detail__top">
        <div class="journal-modal-detail__cover" style="${entry.coverImage ? `background-image: linear-gradient(rgba(0,0,0,0.05), rgba(0,0,0,0.05)), url('${escapeHtml(entry.coverImage)}');` : ""}"></div>
        <div>
          <h3 class="journal-modal-detail__title">${escapeHtml(entry.titulo || "Sin título")}</h3>
          <p class="journal-modal-detail__author">${escapeHtml(entry.autor || "Autora pendiente")}</p>
          <div class="journal-modal-detail__meta">
            <div class="journal-modal-detail__chip"><strong>Formato</strong>${escapeHtml(entry.formato || "Sin indicar")}</div>
            <div class="journal-modal-detail__chip"><strong>Páginas</strong>${escapeHtml(entry.paginas || "Sin indicar")}</div>
            <div class="journal-modal-detail__chip"><strong>Inicio</strong>${escapeHtml(entry.fechaInicio || "Sin indicar")}</div>
            <div class="journal-modal-detail__chip"><strong>Fin</strong>${escapeHtml(entry.fechaFin || "Sin indicar")}</div>
          </div>
          <div class="journal-modal-detail__section">
            <strong>Cómo me hizo sentir</strong>
            <p>${escapeHtml(entry.feelingOption || "No se seleccionó ninguna opción.")}</p>
          </div>
        </div>
      </div>

      <div class="journal-modal-detail__ratings">
        <div class="journal-modal-detail__rating"><strong>Valoración general</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("general", entry.generalRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Romance</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("romance", entry.romanceRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Spice</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("spice", entry.spiceRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Tristeza</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("sadness", entry.sadnessRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Trama</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("general", entry.plotRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Personajes</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("general", entry.charactersRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Estilo</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("general", entry.styleRating)}</div></div>
        <div class="journal-modal-detail__rating"><strong>Final</strong><div class="journal-modal-detail__symbols">${buildSymbolMarkup("general", entry.endingRating)}</div></div>
      </div>

      <div class="journal-modal-detail__section">
        <strong>Personajes destacados</strong>
        <p><strong>Mejor personaje:</strong> ${escapeHtml(entry.bestCharacter || "Sin indicar")}</p>
        <p><strong>Peor personaje:</strong> ${escapeHtml(entry.worstCharacter || "Sin indicar")}</p>
      </div>

      <div class="journal-modal-detail__section">
        <strong>Reflexiones finales</strong>
        <p>${escapeHtml(entry.reflexionesFinales || "Sin reflexiones guardadas.")}</p>
      </div>
    `;
  }

  function renderReviewsModal() {
    if (!modalList || !modalDetail) {
      return;
    }

    if (!entriesCache.length) {
      modalList.innerHTML = '<p class="journal-empty-state">Todavía no has guardado ninguna reseña.</p>';
      modalDetail.innerHTML = '<p class="journal-empty-state">Selecciona una reseña para ver todos sus detalles.</p>';
      return;
    }

    const selectedEntry = entriesCache.find((entry) => Number(entry.idJournal) === Number(modalSelectedEntryId))
      || entriesCache[0];

    modalSelectedEntryId = selectedEntry.idJournal;
    modalList.innerHTML = entriesCache.map((entry) => `
      <button type="button" class="journal-modal-entry${Number(entry.idJournal) === Number(modalSelectedEntryId) ? " is-active" : ""}" data-modal-entry-id="${entry.idJournal}">
        <p class="journal-modal-entry__title">${escapeHtml(entry.titulo || "Sin título")}</p>
        <p class="journal-modal-entry__meta">${escapeHtml(entry.autor || "Autor pendiente")} · ${escapeHtml(entry.formato || "Formato libre")}</p>
      </button>
    `).join("");

    renderModalDetail(selectedEntry);
  }

  function openReviewsModal(entryId = null) {
    if (!reviewModal) {
      return;
    }

    modalSelectedEntryId = entryId || entriesCache[0]?.idJournal || null;
    renderReviewsModal();
    reviewModal.classList.add("is-open");
    reviewModal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
  }

  function closeReviewsModal() {
    if (!reviewModal) {
      return;
    }

    reviewModal.classList.remove("is-open");
    reviewModal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }

  function renderEntries(entries) {
    entriesCache = entries;
    entriesCount.textContent = String(entries.length);

    if (!entries.length) {
      entriesList.innerHTML = '<p class="journal-empty-state">Todavía no has guardado ninguna reseña.</p>';
      renderReviewsModal();
      return;
    }

    entriesList.innerHTML = entries.map((entry) => `
      <article class="journal-entry-card${entry.idJournal === currentEntryId ? " is-active" : ""}" data-entry-id="${entry.idJournal}">
        <h3 class="journal-entry-card__title">${escapeHtml(entry.titulo || "Sin título")}</h3>
        <p class="journal-entry-card__meta">${escapeHtml(entry.autor || "Autor pendiente")} · ${escapeHtml(entry.formato || "Formato libre")}</p>
        <p class="journal-entry-card__excerpt">${escapeHtml((entry.reflexionesFinales || entry.feelingOption || "Reseña guardada.").slice(0, 110))}</p>
        <div class="journal-entry-card__actions">
          <button type="button" class="journal-entry-card__edit" data-edit-id="${entry.idJournal}">Modificar</button>
          <button type="button" class="journal-entry-card__delete" data-delete-id="${entry.idJournal}">Eliminar</button>
        </div>
      </article>
    `).join("");

    renderReviewsModal();
  }

  function setActiveEntryCard(entryId) {
    document.querySelectorAll(".journal-entry-card").forEach((card) => {
      card.classList.toggle("is-active", Number(card.dataset.entryId) === Number(entryId));
    });
  }

  async function loadEntries(selectFirst = true) {
    try {
      const response = await fetch("/readingJournal/entries");
      const data = await response.json();
      const entries = Array.isArray(data.entries) ? data.entries : [];
      renderEntries(entries);

      if (currentEntryId) {
        const current = entries.find((entry) => Number(entry.idJournal) === Number(currentEntryId));
        if (current) {
          fillForm(current);
          return;
        }
      }

      if (selectFirst && entries.length) {
        fillForm(entries[0]);
      } else if (!entries.length) {
        resetForm();
      }
    } catch (error) {
      console.error("Error al cargar el Reading Journal:", error);
      setSaveStatus("Error al cargar");
    }
  }

  async function saveEntry() {
    if (!hasContent()) {
      setSaveStatus("No hay datos para guardar");
      if (typeof Swal !== "undefined") {
        await Swal.fire({
          icon: "warning",
          text: "Completa al menos un campo antes de guardar la reseña.",
          heightAuto: false
        });
      }
      return;
    }

    if (typeof Swal !== "undefined") {
      const result = await Swal.fire({
        icon: "question",
        text: "¿Seguro que quieres guardar esta reseña?",
        showCancelButton: true,
        confirmButtonText: "Guardar",
        cancelButtonText: "Cancelar",
        heightAuto: false
      });

      if (!result.isConfirmed) {
        return;
      }
    }

    setSaveStatus("Guardando...");
    try {
      const response = await fetch("/readingJournal/save", {
        method: "POST",
        headers: getHeaders(),
        body: serializeForm().toString()
      });
      const data = await response.json();

      if (!response.ok || !data.success) {
        setSaveStatus("No se pudo guardar");
        return;
      }

      renderEntries(Array.isArray(data.entries) ? data.entries : []);
      resetForm("Reseña guardada");

      if (typeof Swal !== "undefined") {
        await Swal.fire({
          icon: "success",
          text: "Reseña guardada correctamente.",
          confirmButtonText: "Aceptar",
          heightAuto: false
        });
      }
    } catch (error) {
      console.error("Error al guardar el Reading Journal:", error);
      setSaveStatus("Error al guardar");
    }
  }

  async function deleteEntry(entryId) {
    try {
      const body = new URLSearchParams();
      body.set("idJournal", entryId);

      const response = await fetch("/readingJournal/delete", {
        method: "POST",
        headers: getHeaders(),
        body: body.toString()
      });
      const data = await response.json();

      if (!response.ok || !data.success) {
        return;
      }

      if (Number(currentEntryId) === Number(entryId)) {
        resetForm();
      }

      if (Number(modalSelectedEntryId) === Number(entryId)) {
        modalSelectedEntryId = null;
      }

      renderEntries(Array.isArray(data.entries) ? data.entries : []);

      if (typeof Swal !== "undefined") {
        await Swal.fire({
          toast: true,
          position: "top-end",
          icon: "success",
          title: "Reseña eliminada",
          showConfirmButton: false,
          timer: 1600,
          heightAuto: false
        });
      }
    } catch (error) {
      console.error("Error al eliminar la reseña:", error);
    }
  }

  form.querySelectorAll("input:not([type='hidden']):not([type='file']), textarea").forEach((element) => {
    element.addEventListener("input", () => setSaveStatus("Cambios sin guardar"));
    element.addEventListener("change", () => setSaveStatus("Cambios sin guardar"));
  });

  document.querySelectorAll("[data-rating-group]").forEach((group) => {
    group.addEventListener("click", (event) => {
      const button = event.target.closest("button[data-value]");
      if (!button) {
        return;
      }

      const groupName = group.dataset.ratingGroup;
      ratingInputs[groupName].value = button.dataset.value;
      paintRating(groupName, Number(button.dataset.value));
      setSaveStatus("Cambios sin guardar");
    });
  });

  coverInput.addEventListener("change", () => {
    const [file] = coverInput.files || [];
    if (!file) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      currentCoverImage = typeof reader.result === "string" ? reader.result : "";
      setCoverPreview(currentCoverImage);
      setSaveStatus("Cambios sin guardar");
    };
    reader.readAsDataURL(file);
  });

  saveButton.addEventListener("click", () => {
    saveEntry();
  });

  if (openModalButton) {
    openModalButton.addEventListener("click", () => {
      openReviewsModal();
    });
  }

  if (closeModalButton) {
    closeModalButton.addEventListener("click", () => {
      closeReviewsModal();
    });
  }

  if (reviewModal) {
    reviewModal.addEventListener("click", (event) => {
      if (event.target.matches("[data-close-modal]")) {
        closeReviewsModal();
      }
    });
  }

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && reviewModal?.classList.contains("is-open")) {
      closeReviewsModal();
    }
  });

  entriesList.addEventListener("click", async (event) => {
    const deleteButton = event.target.closest("[data-delete-id]");
    if (deleteButton) {
      event.stopPropagation();
      const idToDelete = Number(deleteButton.dataset.deleteId);

      if (typeof Swal !== "undefined") {
        const result = await Swal.fire({
          text: "¿Quieres eliminar esta reseña del Reading Journal?",
          icon: "warning",
          showCancelButton: true,
          confirmButtonText: "Eliminar",
          cancelButtonText: "Cancelar",
          heightAuto: false
        });

        if (!result.isConfirmed) {
          return;
        }
      }

      deleteEntry(idToDelete);
      return;
    }

    const editButton = event.target.closest("[data-edit-id]");
    if (editButton) {
      event.stopPropagation();
      const entryId = Number(editButton.dataset.editId);
      const entry = entriesCache.find((item) => Number(item.idJournal) === entryId);
      if (entry) {
        fillForm(entry);
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
      return;
    }

    const card = event.target.closest(".journal-entry-card");
    if (!card) {
      return;
    }

    openReviewsModal(Number(card.dataset.entryId));
  });

  if (modalList) {
    modalList.addEventListener("click", (event) => {
      const modalEntry = event.target.closest("[data-modal-entry-id]");
      if (!modalEntry) {
        return;
      }

      modalSelectedEntryId = Number(modalEntry.dataset.modalEntryId);
      renderReviewsModal();
    });
  }

  loadEntries(true);
});
