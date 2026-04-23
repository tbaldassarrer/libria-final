/* Funcionalidades relacionadas con busqueda y detalles de libros */

document.addEventListener("DOMContentLoaded", function () {
    const overlay = document.querySelector('.overlay');
    const searchInput = document.getElementById("searchBook");
    const bookList = document.getElementById("bookList");
    const searchButton = document.getElementById("searchButton");
    const popupWindow = document.getElementById("popupWindow");
    const closePopupButton = document.querySelector(".close-popup");

    if (!searchInput || !bookList || !searchButton) {
      return;
    }

    let selectedBook = "";
    let selectedGoogleId = "";
    let searchAbortController = null;
    let searchDebounceTimer = null;
    let isSearching = false;
    let currentSuggestions = [];

    setSearchButtonState(false);

    searchInput.addEventListener("input", function () {
      const query = this.value.trim();

      if (searchDebounceTimer) {
        clearTimeout(searchDebounceTimer);
      }

      if (query.length === 0) {
        cancelPendingSearch();
        bookList.style.display = "none";
        bookList.innerHTML = "";
        selectedBook = "";
        selectedGoogleId = "";
        currentSuggestions = [];
        setSearchButtonState(false);
        return;
      }

      selectedBook = "";
      selectedGoogleId = "";

      if (query.length < 2) {
        cancelPendingSearch();
        currentSuggestions = [];
        showSearchMessage("Escribe al menos 2 letras para buscar.");
        setSearchButtonState(false);
        return;
      }

      searchDebounceTimer = setTimeout(() => {
        fetchSuggestions(query);
      }, 250);
    });

    searchInput.addEventListener("keydown", function (event) {
      if (event.key !== "Enter") {
        return;
      }

      event.preventDefault();

      if (isSearching) {
        return;
      }

      if (selectedBook || selectedGoogleId) {
        searchBookDetails(selectedBook || searchInput.value.trim(), selectedGoogleId);
        selectedBook = "";
        selectedGoogleId = "";
        return;
      }

      if (currentSuggestions.length > 0) {
        const firstSuggestion = currentSuggestions[0];
        searchInput.value = firstSuggestion.title;
        selectedBook = firstSuggestion.title;
        selectedGoogleId = firstSuggestion.googleId || "";
        searchBookDetails(firstSuggestion.title, selectedGoogleId);
      }
    });

    searchButton.addEventListener("click", function (event) {
      event.preventDefault();

      const query = searchInput.value.trim();

      if (query === "") {
        showAlert('Atencion', 'Por favor, escribe un titulo o un autor', 'warning');
        return;
      }

      if (isSearching) {
        return;
      }

      if (selectedBook || selectedGoogleId) {
        searchBookDetails(selectedBook || query, selectedGoogleId);
        selectedBook = "";
        selectedGoogleId = "";
        return;
      }

      if (currentSuggestions.length > 0) {
        const firstSuggestion = currentSuggestions[0];
        searchInput.value = firstSuggestion.title;
        selectedBook = firstSuggestion.title;
        selectedGoogleId = firstSuggestion.googleId || "";
        searchBookDetails(firstSuggestion.title, selectedGoogleId);
        selectedBook = "";
        selectedGoogleId = "";
        return;
      }

      showSearchMessage("Sigue escribiendo para ver sugerencias.");
    });

    function fetchSuggestions(query) {
      cancelPendingSearch();
      searchAbortController = new AbortController();
      isSearching = true;
      setSearchButtonState(true);
      showSearchLoading();

      fetch(`/searchBooks?query=${encodeURIComponent(query)}`, {
        signal: searchAbortController.signal
      })
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          return response.json();
        })
        .then(data => {
          isSearching = false;
          setSearchButtonState(false);
          bookList.innerHTML = "";

          if (!Array.isArray(data) || data.length === 0) {
            currentSuggestions = [];
            bookList.style.display = "none";
            return;
          }

          currentSuggestions = data.map(normalizeBookSuggestion);

          currentSuggestions.forEach(suggestion => {
            let li = document.createElement("li");
            li.className = "list-group-item";
            li.innerHTML = `
              <img class="book-suggestion-cover" src="${suggestion.cover}" alt="">
              <span class="book-suggestion-text">
                <strong>${escapeHtml(suggestion.title)}</strong>
                <small>${escapeHtml(suggestion.author)}${suggestion.year ? " · " + escapeHtml(suggestion.year) : ""}</small>
              </span>
              <span class="book-suggestion-source">${escapeHtml(suggestion.source)}</span>
            `;
            li.tabIndex = 0;
            li.onclick = function () {
              searchInput.value = suggestion.title;
              selectedBook = suggestion.title;
              selectedGoogleId = suggestion.googleId || "";
              bookList.style.display = "none";
              searchBookDetails(suggestion.title, selectedGoogleId);
            };
            li.onkeydown = function (event) {
              if (event.key === "Enter") {
                li.click();
              }
            };
            bookList.appendChild(li);
          });

          bookList.style.display = "block";
        })
        .catch(error => {
          if (error.name === "AbortError") {
            return;
          }

          isSearching = false;
          setSearchButtonState(false);
          currentSuggestions = [];
          showSearchMessage("No se pudieron cargar sugerencias. Intenta de nuevo.");
          console.error("Error al buscar libros:", error);
        });
    }

    function searchBookDetails(bookTitle, googleId = "") {
      bookTitle = (bookTitle || "").trim();

      if (bookTitle === "" && !googleId) {
        showAlert('Atencion', 'Por favor, ingresa o selecciona un libro', 'warning');
        return;
      }

      showSearchMessage("Cargando detalles del libro...");

      const params = new URLSearchParams();
      if (bookTitle) params.set("title", bookTitle);
      if (googleId) params.set("googleId", googleId);

      fetch(`/getBookDetails?${params.toString()}`)
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          return response.json();
        })
        .then(data => {
          if (!data || Object.keys(data).length === 0) {
            showSearchMessage("No se encontraron detalles para este libro.");
            return;
          }

          bookList.style.display = "none";
          bookList.innerHTML = "";

          if (!popupWindow) {
            window.location.href = `/home?bookTitle=${encodeURIComponent(data.titulo || bookTitle)}`;
            return;
          }

          document.getElementById("bookTitle").textContent = data.titulo;
          document.getElementById("bookAuthor").textContent = data.autor;
          document.getElementById("bookGenre").textContent = data.genero;
          document.getElementById("bookYear").textContent = data.anioEdicion;
          document.getElementById("bookRating").textContent = data.puntuacion;
          document.getElementById("bookSynopsis").textContent = data.sinopsis;
          document.getElementById("bookCover").src = data.cover_image || "/images/portadaLibro.jpg";

          popupWindow.style.display = "block";
          if (overlay) {
            overlay.classList.add('active');
          }
        })
        .catch(error => {
          showSearchMessage("No se pudieron cargar los detalles. Intenta de nuevo.");
          console.error("Error al obtener detalles del libro:", error);
        });
    }

    if (closePopupButton && popupWindow) {
      closePopupButton.addEventListener("click", function () {
        popupWindow.style.display = "none";
        if (overlay) {
          overlay.classList.remove('active');
        }
        searchInput.value = "";
      });
    }

    const initialBookTitle = new URLSearchParams(window.location.search).get("bookTitle");
    if (initialBookTitle) {
      searchInput.value = initialBookTitle;
      searchBookDetails(initialBookTitle);
      window.history.replaceState(null, "", window.location.pathname);
    }

    function showSearchLoading() {
      showSearchMessage("Buscando en Google Books...");
    }

    function showSearchMessage(message) {
      bookList.innerHTML = `<li class="list-group-item search-status">${escapeHtml(message)}</li>`;
      bookList.style.display = "block";
    }

    function cancelPendingSearch() {
      if (searchAbortController) {
        searchAbortController.abort();
        searchAbortController = null;
      }
      isSearching = false;
    }

    function setSearchButtonState(loading) {
      searchButton.disabled = loading;
      searchButton.setAttribute("aria-disabled", String(loading));
      searchButton.title = loading ? "Buscando libros..." : "Buscar";
    }
});

function showAlert(title, text, icon) {
  if (typeof Swal !== "undefined") {
    Swal.fire({
      title,
      text,
      icon,
      confirmButtonText: 'OK',
      customClass: {
        popup: 'malva-popup',
        confirmButton: 'malva-confirm-button'
      }
    });
    return;
  }

  alert(text);
}

function normalizeBookSuggestion(book) {
  if (typeof book === "string") {
    return {
      title: book,
      author: "Autor desconocido",
      year: "",
      cover: "/images/portadaLibro.jpg",
      source: "Libria"
    };
  }

  return {
    title: book?.titulo || "Titulo desconocido",
    author: book?.autor || "Autor desconocido",
    year: book?.anioEdicion || "",
    cover: book?.cover_image || "/images/portadaLibro.jpg",
    source: book?.source || "Google Books",
    googleId: book?.googleId || ""
  };
}

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value || "";
  return div.innerHTML;
}

function closePopup() {
    const popupWindow = document.getElementById("popupWindow");
    const overlay = document.querySelector(".overlay");

    if (popupWindow) {
      popupWindow.style.display = "none";
    }

    if (overlay) {
      overlay.classList.remove("active");
    }
}
