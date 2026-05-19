document.addEventListener("DOMContentLoaded", () => {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
  const createForm = document.getElementById("clubCreateForm");
  const bookSelect = document.getElementById("clubBook");
  const clubList = document.getElementById("clubList");
  const clubCount = document.getElementById("clubCount");
  const modal = document.getElementById("clubDiscussionModal");
  const modalHeader = document.getElementById("clubDiscussionHeader");
  const commentsContainer = document.getElementById("clubComments");
  const commentForm = document.getElementById("clubCommentForm");
  const closeModalButton = document.getElementById("closeClubModal");

  let clubs = [];
  let selectedClub = null;

  function headers() {
    const result = { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" };
    if (csrfToken && csrfHeader) {
      result[csrfHeader] = csrfToken;
    }
    return result;
  }

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function showToast(text, icon = "success") {
    if (typeof Swal === "undefined") {
      return;
    }

    Swal.fire({
      toast: true,
      position: "top-end",
      icon,
      title: text,
      showConfirmButton: false,
      timer: 1700,
      heightAuto: false
    });
  }

  async function requestJson(url, options = {}) {
    const response = await fetch(url, options);
    const data = await response.json();
    if (!response.ok || data.success === false) {
      throw new Error(data.message || "No se pudo completar la accion.");
    }
    return data;
  }

  function renderBooks(books) {
    if (!books.length) {
      bookSelect.innerHTML = '<option value="">No hay libros disponibles</option>';
      return;
    }

    bookSelect.innerHTML = '<option value="">Selecciona un libro</option>' + books.map((book) => (
      `<option value="${book.idLibro}">${escapeHtml(book.titulo)} · ${escapeHtml(book.autor || "Autor pendiente")}</option>`
    )).join("");
  }

  function renderClubs(nextClubs) {
    clubs = nextClubs || [];
    clubCount.textContent = String(clubs.length);

    if (!clubs.length) {
      clubList.innerHTML = '<p class="club-empty">Todavia no hay clubes activos. Crea el primero para empezar una lectura compartida.</p>';
      return;
    }

    clubList.innerHTML = clubs.map((club) => `
      <article class="club-card" data-club-id="${club.idClub}">
        <img class="club-card__cover" src="${escapeHtml(club.coverImage)}" alt="Portada de ${escapeHtml(club.titulo)}" />
        <div class="club-card__content">
          <div class="club-card__top">
            <div>
              <h3>${escapeHtml(club.name)}</h3>
              <p>${escapeHtml(club.titulo)} · ${escapeHtml(club.autor || "Autor pendiente")}</p>
            </div>
            <span class="club-card__chapter">Cap. ${club.currentChapter || 1}</span>
          </div>
          <p class="club-card__description">${escapeHtml(club.description || "Club abierto para comentar esta lectura por capitulos.")}</p>
          <div class="club-card__meta">
            <span><i class="fa-solid fa-user-group"></i> ${club.memberCount} miembros</span>
            <span><i class="fa-solid fa-message"></i> ${club.commentCount} comentarios</span>
            <span><i class="fa-solid fa-crown"></i> ${escapeHtml(club.ownerUsername || "Lector")}</span>
          </div>
          <div class="club-card__actions">
            <button type="button" class="club-secondary-button" data-discuss="${club.idClub}">
              <i class="fa-solid fa-comments"></i> Comentarios
            </button>
            ${club.joined
              ? `<button type="button" class="club-muted-button" data-leave="${club.idClub}">Salir</button>`
              : `<button type="button" class="club-primary-button" data-join="${club.idClub}">Unirme</button>`}
          </div>
        </div>
      </article>
    `).join("");
  }

  function renderModalHeader(club) {
    modalHeader.innerHTML = `
      <img class="club-discussion-header__cover" src="${escapeHtml(club.coverImage)}" alt="Portada de ${escapeHtml(club.titulo)}" />
      <div>
        <p class="club-eyebrow">Comentarios por capitulos</p>
        <h2 id="clubDiscussionTitle">${escapeHtml(club.name)}</h2>
        <p>${escapeHtml(club.titulo)} · ${escapeHtml(club.autor || "Autor pendiente")}</p>
      </div>
    `;
  }

  function renderComments(comments) {
    if (!comments.length) {
      commentsContainer.innerHTML = '<p class="club-empty">Aun no hay comentarios. Puedes abrir el primer hilo de este club.</p>';
      return;
    }

    commentsContainer.innerHTML = comments.map((comment) => `
      <article class="club-comment">
        <div class="club-comment__header">
          <strong>Capitulo ${comment.chapterNumber}</strong>
          <span>${escapeHtml(comment.username || "Lector")}</span>
        </div>
        <p>${escapeHtml(comment.commentText)}</p>
      </article>
    `).join("");
  }

  async function loadData() {
    try {
      const data = await requestJson("/readingClubs/data");
      renderBooks(data.books || []);
      renderClubs(data.clubs || []);
    } catch (error) {
      clubList.innerHTML = `<p class="club-empty">${escapeHtml(error.message)}</p>`;
    }
  }

  async function openDiscussion(idClub) {
    selectedClub = clubs.find((club) => Number(club.idClub) === Number(idClub));
    if (!selectedClub) {
      return;
    }

    renderModalHeader(selectedClub);
    document.getElementById("commentClubId").value = selectedClub.idClub;
    document.getElementById("commentChapter").value = selectedClub.currentChapter || 1;
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";

    commentsContainer.innerHTML = '<p class="club-empty">Cargando comentarios...</p>';
    try {
      const data = await requestJson(`/readingClubs/comments?idClub=${encodeURIComponent(idClub)}`);
      renderComments(data.comments || []);
    } catch (error) {
      commentsContainer.innerHTML = `<p class="club-empty">${escapeHtml(error.message)}</p>`;
    }
  }

  function closeDiscussion() {
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    selectedClub = null;
  }

  createForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const body = new URLSearchParams();
    body.set("idLibro", bookSelect.value);
    body.set("name", document.getElementById("clubName").value.trim());
    body.set("currentChapter", document.getElementById("clubChapter").value || "1");
    body.set("description", document.getElementById("clubDescription").value.trim());

    try {
      const data = await requestJson("/readingClubs/create", {
        method: "POST",
        headers: headers(),
        body: body.toString()
      });
      renderClubs(data.clubs || []);
      createForm.reset();
      document.getElementById("clubChapter").value = "1";
      showToast("Club creado");
    } catch (error) {
      showToast(error.message, "warning");
    }
  });

  clubList.addEventListener("click", async (event) => {
    const joinButton = event.target.closest("[data-join]");
    const leaveButton = event.target.closest("[data-leave]");
    const discussButton = event.target.closest("[data-discuss]");

    if (discussButton) {
      openDiscussion(discussButton.dataset.discuss);
      return;
    }

    const actionButton = joinButton || leaveButton;
    if (!actionButton) {
      return;
    }

    const body = new URLSearchParams();
    body.set("idClub", joinButton ? joinButton.dataset.join : leaveButton.dataset.leave);

    try {
      const data = await requestJson(joinButton ? "/readingClubs/join" : "/readingClubs/leave", {
        method: "POST",
        headers: headers(),
        body: body.toString()
      });
      renderClubs(data.clubs || []);
      showToast(joinButton ? "Te has unido al club" : "Has salido del club");
    } catch (error) {
      showToast(error.message, "warning");
    }
  });

  commentForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const body = new URLSearchParams();
    body.set("idClub", document.getElementById("commentClubId").value);
    body.set("chapterNumber", document.getElementById("commentChapter").value || "1");
    body.set("commentText", document.getElementById("commentText").value.trim());

    try {
      const data = await requestJson("/readingClubs/comments", {
        method: "POST",
        headers: headers(),
        body: body.toString()
      });
      renderComments(data.comments || []);
      renderClubs(data.clubs || clubs);
      document.getElementById("commentText").value = "";
      showToast("Comentario publicado");
    } catch (error) {
      showToast(error.message, "warning");
    }
  });

  closeModalButton.addEventListener("click", closeDiscussion);
  modal.addEventListener("click", (event) => {
    if (event.target.matches("[data-close-club-modal]")) {
      closeDiscussion();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && modal.classList.contains("is-open")) {
      closeDiscussion();
    }
  });

  loadData();
});
