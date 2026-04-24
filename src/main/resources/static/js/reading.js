/* Funcionalidades relacionadas con biblioteca y lecturas en progreso */

function formHeaders() {
    const headers = { "Content-Type": "application/x-www-form-urlencoded" };
    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function readJsonResponse(response) {
    return response.text().then(text => {
        let data = {};

        try {
            data = text ? JSON.parse(text) : {};
        } catch (error) {
            data = {};
        }

        if (!response.ok) {
            data.success = false;
            data.message = data.message || `Error HTTP ${response.status}`;
        }

        return data;
    });
}

function isHomeView() {
    return !!document.querySelector(".cols__container");
}

function showActionFeedback(title, text, icon, reloadExplora = false) {
    const onComplete = () => {
        if (reloadExplora && window.location.pathname === "/explora") {
            window.location.reload();
        }
    };

    if (typeof Swal !== "undefined" && !isHomeView()) {
        Swal.fire({
            title,
            text,
            icon,
            confirmButtonText: "OK",
            customClass: {
                popup: "malva-popup",
                confirmButton: "malva-confirm-button"
            }
        }).then(onComplete);
        return;
    }

    onComplete();
}

function submitLibraryReview(title, review, rating) {
    return fetch("/addToLibraryWithReview", {
        method: "POST",
        headers: formHeaders(),
        body: new URLSearchParams({
            title,
            resenia: review,
            puntuacion: String(rating)
        })
    }).then(readJsonResponse);
}

function promptAddToLibrary(title) {
    if (typeof Swal === "undefined") {
        return;
    }

    Swal.fire({
        title: "Anadir a libros leidos",
        html: `
            <label class="swal2-input-label" for="swalLibraryRating">Puntuacion</label>
            <select id="swalLibraryRating" class="swal2-select">
                <option value="">Selecciona una puntuacion</option>
                <option value="5">5 estrellas</option>
                <option value="4">4 estrellas</option>
                <option value="3">3 estrellas</option>
                <option value="2">2 estrellas</option>
                <option value="1">1 estrella</option>
            </select>
            <label class="swal2-input-label" for="swalLibraryReview">Resena</label>
            <textarea id="swalLibraryReview" class="swal2-textarea" placeholder="Escribe tu resena..."></textarea>
        `,
        showCancelButton: true,
        confirmButtonText: "Guardar",
        cancelButtonText: "Cancelar",
        focusConfirm: false,
        customClass: {
            popup: "malva-popup",
            confirmButton: "malva-confirm-button"
        },
        preConfirm: () => {
            const rating = document.getElementById("swalLibraryRating")?.value || "";
            const review = (document.getElementById("swalLibraryReview")?.value || "").trim();

            if (!rating) {
                Swal.showValidationMessage("Selecciona una puntuacion.");
                return false;
            }

            if (!review) {
                Swal.showValidationMessage("Escribe una resena antes de guardar.");
                return false;
            }

            return {
                rating: parseInt(rating, 10),
                review
            };
        }
    }).then(result => {
        if (!result.isConfirmed || !result.value) {
            return;
        }

        submitLibraryReview(title, result.value.review, result.value.rating)
            .then(data => {
                if (!data.success) {
                    showActionFeedback("Aviso", data.message || "No se pudo guardar el libro.", "warning");
                    return;
                }

                refreshHomeSections();
                showActionFeedback("Guardado", "El libro se ha anadido a tus libros leidos.", "success", true);
            })
            .catch(error => {
                console.error("Error al guardar el libro en leidos:", error);
                showActionFeedback("Error", "No se pudo guardar el libro.", "error");
            });
    });
}

function addToLibrary(title) {
    fetch("/checkIfBookExists?title=" + encodeURIComponent(title), { method: "GET" })
        .then(response => response.json())
        .then(data => {
            if (data.exists) {
                Swal.fire({
                    title: "Atencion",
                    text: "El libro ya esta en tu biblioteca.",
                    icon: "warning",
                    confirmButtonText: "OK",
                    customClass: {
                        popup: "malva-popup",
                        confirmButton: "malva-confirm-button"
                    }
                });
                return;
            }

            if (typeof closePopup === "function") closePopup();

            const modal = document.getElementById("finishReadingModal");
            const overlay = document.getElementById("overlayModal");
            if (!modal || !overlay) {
                promptAddToLibrary(title);
                return;
            }

            modal.dataset.bookTitle = title;
            delete modal.dataset.bookId;
            overlay.style.display = "block";
            modal.style.display = "flex";

            const reviewText = document.getElementById("reviewText");
            if (reviewText) reviewText.value = "";
            if (typeof selectedRating !== "undefined") {
                selectedRating = 0;
            }
            document.querySelectorAll(".stars span").forEach(star => star.classList.remove("selected"));

            const searchBook = document.getElementById("searchBook");
            if (searchBook) searchBook.value = "";
        })
        .catch(error => console.error("Error al verificar si el libro existe:", error));
}

function addToUpcomingReads(title, coverImage) {
    fetch("/addToUpcomingReads", {
        method: "POST",
        headers: formHeaders(),
        body: `title=${encodeURIComponent(title)}`
    })
        .then(readJsonResponse)
        .then(data => {
            if (!data.success) {
                Swal.fire({
                    title: "Aviso",
                    text: data.message || "No se pudo anadir a proximas lecturas.",
                    icon: "warning",
                    confirmButtonText: "OK",
                    customClass: {
                        popup: "malva-popup",
                        confirmButton: "malva-confirm-button"
                    }
                });
                return;
            }

            const upcomingContainer = document.querySelector(".proxLecturas");
            if (upcomingContainer) {
                const img = document.createElement("img");
                img.src = coverImage;
                img.alt = title;
                img.classList.add("upcoming-book");
                upcomingContainer.appendChild(img);
            }

            if (typeof closePopup === "function") closePopup();
            const searchBook = document.getElementById("searchBook");
            const bookList = document.getElementById("bookList");
            if (searchBook) searchBook.value = "";
            if (bookList) bookList.style.display = "none";

            loadUpcomingReads();
            showActionFeedback("Guardado", "El libro se ha anadido a proximas lecturas.", "success");
        })
        .catch(error => console.error("Error al anadir el libro:", error));
}

function refreshHomeSections() {
    if (typeof loadLibrary === "function") loadLibrary();
    if (typeof loadUpcomingReads === "function") loadUpcomingReads();
    if (typeof loadCurrentReading === "function") loadCurrentReading();
    if (typeof loadReviews === "function") loadReviews();
    if (typeof loadFavorites === "function") loadFavorites();
    if (typeof updateProgressBar === "function") updateProgressBar();
}

function startReading() {
    const bookTitle = document.getElementById("bookTitle")?.textContent?.trim();
    if (bookTitle) {
        startReadingByTitle(bookTitle);
    }
}

function startReadingByTitle(bookTitle) {
    fetch("/startReading?title=" + encodeURIComponent(bookTitle), {
        method: "POST",
        headers: formHeaders()
    })
        .then(readJsonResponse)
        .then(data => {
            if (!data.success) {
                Swal.fire({
                    title: "Aviso",
                    text: data.message || "No se pudo comenzar esta lectura.",
                    icon: "warning",
                    confirmButtonText: "OK",
                    customClass: {
                        popup: "malva-popup",
                        confirmButton: "malva-confirm-button"
                    }
                });
                return;
            }

            if (typeof closePopup === "function") closePopup();
            const searchBook = document.getElementById("searchBook");
            if (searchBook) searchBook.value = "";

            refreshHomeSections();
            showActionFeedback("Listo", "El libro ya esta en tus lecturas actuales.", "success", true);
        })
        .catch(error => console.error("Error al comenzar a leer:", error));
}

function deleteReadingEntry(idLibro, estadoLectura) {
    return fetch("/deleteReadingEntry", {
        method: "POST",
        headers: formHeaders(),
        body: `idLibro=${encodeURIComponent(idLibro)}&estadoLectura=${encodeURIComponent(estadoLectura)}`
    })
        .then(readJsonResponse)
        .then(data => {
            if (!data.success) {
                throw new Error(data.message || "No se pudo eliminar el libro.");
            }
            refreshHomeSections();
        });
}

function createDeleteButton(label, onClick) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn-delete-entry";
    button.setAttribute("aria-label", label);
    button.title = label;
    button.innerHTML = '<i class="fa-solid fa-trash"></i>';
    button.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        onClick();
    });
    return button;
}

function confirmDeleteEntry(idLibro, estadoLectura, title) {
    Swal.fire({
        title: "Eliminar libro",
        text: `Quieres eliminar "${title}"?`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Eliminar",
        cancelButtonText: "Cancelar",
        customClass: {
            popup: "malva-popup",
            confirmButton: "malva-confirm-button"
        }
    }).then(result => {
        if (!result.isConfirmed) return;
        deleteReadingEntry(idLibro, estadoLectura)
            .catch(error => Swal.fire("Error", error.message, "error"));
    });
}

function loadLibrary() {
    fetch("/getLibraryBooks")
        .then(response => response.json())
        .then(data => {
            const libraryContainer = document.querySelector(".leidos");
            if (!libraryContainer) return;

            libraryContainer.innerHTML = "";

            if (!Array.isArray(data) || data.length === 0) {
                libraryContainer.innerHTML = "<p>Aun no has completado ningun libro.</p>";
                return;
            }

            data.forEach(book => {
                const tile = document.createElement("div");
                tile.className = "book-tile";

                const img = document.createElement("img");
                img.src = book.cover_image;
                img.alt = book.titulo;
                img.classList.add("library-book");
                img.onclick = function () {
                    openPopup2(
                        book.titulo,
                        book.autor,
                        book.genero,
                        book.anioEdicion,
                        book.puntuacion,
                        book.cover_image
                    );
                };

                tile.appendChild(img);
                tile.appendChild(createDeleteButton(`Eliminar ${book.titulo}`, () => {
                    confirmDeleteEntry(book.idLibro, "Completado", book.titulo);
                }));
                libraryContainer.appendChild(tile);
            });
        })
        .catch(error => console.error("Error al cargar los libros leidos:", error));
}

function loadUpcomingReads() {
    fetch("/getUpcomingReads")
        .then(response => response.json())
        .then(data => {
            const upcomingContainer = document.querySelector(".proxLecturas");
            if (!upcomingContainer) return;

            upcomingContainer.innerHTML = "";

            if (!Array.isArray(data) || data.length === 0) {
                upcomingContainer.innerHTML = "<p>No tienes proximas lecturas.</p>";
                return;
            }

            data.forEach(book => {
                const tile = document.createElement("div");
                tile.className = "book-tile";

                const img = document.createElement("img");
                img.src = book.cover_image;
                img.alt = book.titulo;
                img.classList.add("upcoming-book");
                img.dataset.title = book.titulo;
                img.dataset.cover = book.cover_image;
                img.addEventListener("click", function () {
                    openUpcomingModal(this.dataset.title, this.dataset.cover);
                });

                tile.appendChild(img);
                tile.appendChild(createDeleteButton(`Eliminar ${book.titulo}`, () => {
                    confirmDeleteEntry(book.idLibro, "Próximas Lecturas", book.titulo);
                }));
                upcomingContainer.appendChild(tile);
            });
        })
        .catch(error => console.error("Error al obtener los libros de proximas lecturas:", error));
}

function openUpcomingModal(title, coverImage) {
    const upcomingModal = document.getElementById("upcomingModal");
    const upcomingTitle = document.getElementById("upcomingTitle");
    const startReadingBtn = document.getElementById("startReadingBtn");

    if (!upcomingModal || !upcomingTitle || !startReadingBtn) return;

    upcomingTitle.textContent = title;
    startReadingBtn.onclick = function () {
        startReadingByTitle(title, coverImage);
        closeUpcomingModal();
    };

    upcomingModal.style.display = "block";
}

function closeUpcomingModal() {
    const upcomingModal = document.getElementById("upcomingModal");
    if (upcomingModal) {
        upcomingModal.style.display = "none";
    }
}

function getReadingGoalValue() {
    const targetElement = document.getElementById("targetDisplay");
    const value = parseInt(targetElement?.value, 10);
    return Number.isInteger(value) && value > 0 ? Math.min(value, 999) : 10;
}

function setReadingGoalValue(goal) {
    const targetElement = document.getElementById("targetDisplay");
    if (!targetElement) return;
    targetElement.value = Math.max(1, Math.min(parseInt(goal, 10) || 10, 999));
}

let readingGoalSaveTimer = null;
let readingGoalLastSaved = null;

function saveReadingGoal() {
    const goal = getReadingGoalValue();
    setReadingGoalValue(goal);

    if (readingGoalLastSaved === goal) {
        updateProgressBar();
        return Promise.resolve({ success: true, goal });
    }

    return fetch("/readingGoal", {
        method: "POST",
        headers: formHeaders(),
        body: new URLSearchParams({ goal })
    })
        .then(readJsonResponse)
        .then(data => {
            if (data.success && data.goal) {
                setReadingGoalValue(data.goal);
                readingGoalLastSaved = parseInt(data.goal, 10);
            }
            updateProgressBar();
            return data;
        })
        .catch(error => {
            console.error("Error al guardar el desafio de lectura:", error);
            updateProgressBar();
        });
}

function queueReadingGoalSave() {
    window.clearTimeout(readingGoalSaveTimer);
    updateProgressBar();
    readingGoalSaveTimer = window.setTimeout(saveReadingGoal, 350);
}

function loadReadingGoal() {
    return fetch("/readingGoal")
        .then(response => response.json())
        .then(data => {
            setReadingGoalValue(data.goal || getReadingGoalValue());
            readingGoalLastSaved = getReadingGoalValue();
            updateProgressBar();
        })
        .catch(error => {
            console.error("Error al cargar el desafio de lectura:", error);
            updateProgressBar();
        });
}

function updateProgressBar() {
    fetch("/getLibraryBooks")
        .then(response => response.json())
        .then(data => {
            const completedLabel = document.getElementById("completedLabel");
            const progressBar = document.getElementById("progressBar");
            const count = Array.isArray(data) ? data.length : 0;
            const target = getReadingGoalValue();
            const percentage = Math.min((count / target) * 100, 100);

            if (completedLabel) completedLabel.textContent = count;
            if (progressBar) progressBar.style.width = percentage + "%";
        })
        .catch(error => console.error("Error al obtener libros completados:", error));
}

function formatReadingStartDate(fechaInicio) {
    if (!fechaInicio || fechaInicio.trim() === "" || fechaInicio === "undefined") {
        return "No disponible";
    }

    const normalizedDate = fechaInicio.split("T")[0];
    const fecha = new Date(`${normalizedDate}T00:00:00`);
    if (isNaN(fecha.getTime())) {
        return "No disponible";
    }

    return fecha.toLocaleDateString("es-ES", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric"
    });
}

function renderCurrentReadings(data) {
    const readingContainer = document.querySelector(".reading-container");
    const list = document.getElementById("currentReadingsList");

    if (!readingContainer || !list) return;

    const books = Array.isArray(data?.books)
        ? data.books
        : (data?.idLibro ? [data] : []);

    if (books.length === 0) {
        readingContainer.style.display = "none";
        list.innerHTML = '<p class="empty-current-reading">No tienes lecturas en progreso.</p>';
        return;
    }

    readingContainer.style.display = "flex";
    list.innerHTML = "";

    books.forEach(book => {
        const card = document.createElement("article");
        card.className = "current-reading-card";

        const cover = document.createElement("img");
        cover.className = "book-cover2";
        cover.src = book.cover_image || "/images/LIBRiA.png";
        cover.alt = book.titulo || "Portada del libro";

        const info = document.createElement("div");
        info.className = "book-info2";

        const title = document.createElement("a");
        title.className = "current-reading-title";
        title.textContent = `${book.titulo || "Sin titulo"}${book.autor ? " - " + book.autor : ""}`;
        title.dataset.bookId = book.idLibro || "";

        const date = document.createElement("p");
        date.className = "reading-start-date";
        date.textContent = `Fecha de inicio: ${formatReadingStartDate(book.fechaInicio)}`;

        const actions = document.createElement("div");
        actions.className = "current-reading-actions";

        const finishButton = document.createElement("button");
        finishButton.type = "button";
        finishButton.className = "btn-finish finish-reading-btn";
        finishButton.textContent = "Finalizar lectura";
        finishButton.dataset.bookId = book.idLibro || "";

        actions.appendChild(finishButton);
        actions.appendChild(createDeleteButton(`Eliminar ${book.titulo}`, () => {
            confirmDeleteEntry(book.idLibro, "En progreso", book.titulo);
        }));

        info.append(title, date, actions);
        card.append(cover, info);
        list.appendChild(card);
    });
}

function loadCurrentReading() {
    fetch("/getCurrentReading")
        .then(response => response.json())
        .then(renderCurrentReadings)
        .catch(error => console.error("Error al obtener el estado de lectura:", error));
}

document.addEventListener("DOMContentLoaded", function () {
    const targetDisplay = document.getElementById("targetDisplay");
    const stepButtons = document.querySelectorAll("[data-goal-step]");
    const popupWindow = document.getElementById("popupWindow");

    loadCurrentReading();
    loadLibrary();
    loadUpcomingReads();
    updateProgressBar();

    if (popupWindow) {
        popupWindow.addEventListener("click", function (event) {
            const actionButton = event.target.closest("[data-book-action]");
            if (!actionButton) return;

            const title = document.getElementById("bookTitle")?.textContent?.trim();
            const coverImage = document.getElementById("bookCover")?.getAttribute("src") || "";

            if (!title) return;

            const action = actionButton.dataset.bookAction;
            if (action === "library") {
                addToLibrary(title);
            } else if (action === "reading") {
                startReadingByTitle(title);
            } else if (action === "upcoming") {
                addToUpcomingReads(title, coverImage);
            }
        });
    }

    if (!targetDisplay) return;

    loadReadingGoal();

    targetDisplay.addEventListener("input", queueReadingGoalSave);
    targetDisplay.addEventListener("change", saveReadingGoal);
    targetDisplay.addEventListener("blur", saveReadingGoal);
    targetDisplay.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            e.preventDefault();
            this.blur();
        }
    });

    stepButtons.forEach(button => {
        button.addEventListener("click", function () {
            const step = parseInt(this.dataset.goalStep, 10) || 0;
            setReadingGoalValue(getReadingGoalValue() + step);
            saveReadingGoal();
        });
    });

    window.addEventListener("beforeunload", function () {
        if (readingGoalSaveTimer) {
            window.clearTimeout(readingGoalSaveTimer);
            const goal = getReadingGoalValue();
            if (readingGoalLastSaved !== goal) {
                fetch("/readingGoal", {
                    method: "POST",
                    headers: formHeaders(),
                    body: new URLSearchParams({ goal }),
                    keepalive: true
                });
            }
        }
    });
});
