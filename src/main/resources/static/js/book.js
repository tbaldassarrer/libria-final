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

    searchInput.addEventListener("input", function () {
      let query = this.value.trim();
      if (query.length === 0) {
        bookList.style.display = "none";
        bookList.innerHTML = "";
        selectedBook = "";
        return;
      }

      fetch(`/searchBooks?query=${encodeURIComponent(query)}`)
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          return response.json();
        })
        .then(data => {
          bookList.innerHTML = "";
          if (!Array.isArray(data) || data.length === 0) {
            bookList.style.display = "none";
            return;
          }

          data.forEach(book => {
            let li = document.createElement("li");
            li.className = "list-group-item";
            li.textContent = book;
            li.tabIndex = 0;
            li.onclick = function () {
              searchInput.value = book;
              selectedBook = book;
              bookList.style.display = "none";
              searchBookDetails(book);
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
        .catch(error => console.error("Error al buscar libros:", error));
    });

    searchButton.addEventListener("click", function (event) {
      event.preventDefault();

      if (searchInput.value.trim() === "") {
        showAlert('Atencion', 'Por favor, ingresa o selecciona un libro', 'warning');
        return;
      }

      const bookTitle = selectedBook || searchInput.value.trim();
      searchBookDetails(bookTitle);
      selectedBook = "";
    });

    function searchBookDetails(bookTitle) {
      bookTitle = (bookTitle || "").trim();

      if (bookTitle === "") {
        showAlert('Atencion', 'Por favor, ingresa o selecciona un libro', 'warning');
        return;
      }

      fetch(`/getBookDetails?title=${encodeURIComponent(bookTitle)}`)
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }
          return response.json();
        })
        .then(data => {
          if (!data || Object.keys(data).length === 0) {
            showAlert('Error', 'No se encontraron detalles para este libro', 'error');
            return;
          }

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
        .catch(error => console.error("Error al obtener detalles del libro:", error));
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
