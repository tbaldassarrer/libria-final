/* Funcionalidades relacionadas con búsqueda y detalles de libros */

document.addEventListener("DOMContentLoaded", function () {
    const overlay = document.querySelector('.overlay');
    const searchInput = document.getElementById("searchBook");
    const bookList = document.getElementById("bookList");
    const searchButton = document.getElementById("searchButton");
    const popupWindow = document.getElementById("popupWindow");
    const closePopup = document.querySelector(".close-popup");
 
    let selectedBook = "";
 
    // Evento para buscar mientras se escribe
    searchInput.addEventListener("keyup", function () {
      let query = this.value.trim();
      if (query.length === 0) {
        bookList.style.display = "none";
        selectedBook = ""; // Reinicia el valor de selectedBook cuando el input está vacío
        return;
      }
 
      fetch(`/searchBooks?query=${encodeURIComponent(query)}`)
        .then(response => response.json())
        .then(data => {
          bookList.innerHTML = "";
          if (data.length === 0) {
            bookList.style.display = "none";
            return;
          }
 
          data.forEach(book => {
            let li = document.createElement("li");
            li.className = "list-group-item";
            li.textContent = book;
            li.onclick = function () {
              searchInput.value = book;
              selectedBook = book;
              bookList.style.display = "none";
              searchBookDetails(book); // Buscar detalles automáticamente
            };
            bookList.appendChild(li);
          });
 
          bookList.style.display = "block";
        })
        .catch(error => console.error("Error al buscar libros:", error));
    });
 
    // Evento para el botón de búsqueda
    searchButton.addEventListener("click", function (event) {
      event.preventDefault();
 
      if (searchInput.value.trim() === "") {
        Swal.fire({
          title: 'Atención',
          text: 'Por favor, ingesa o selecciona un libro',
          icon: 'warning',
          confirmButtonText: 'OK'
        });
        
        return;
      }
 
      searchBookDetails(selectedBook);
      selectedBook = ""; // Reinicia el valor después de buscar
    });
 
    // Función para buscar detalles del libro y mostrar el modal
    function searchBookDetails(bookTitle) {
      fetch(`/getBookDetails?title=${encodeURIComponent(bookTitle)}`)
        .then(response => response.json())
        .then(data => {
          if (!data || Object.keys(data).length === 0) {
                        Swal.fire({
              title: 'Error',
              text: 'No se encontraron detalles para este libro',
              icon: 'error',
              confirmButtonText: 'OK',
              customClass: {
                  popup: 'malva-popup',
                  confirmButton: 'malva-confirm-button'
              }
          });
            return;
          }
 
          document.getElementById("bookTitle").textContent = data.titulo;
          document.getElementById("bookAuthor").textContent = data.autor;
          document.getElementById("bookGenre").textContent = data.genero;
          document.getElementById("bookYear").textContent = data.anioEdicion;
          document.getElementById("bookRating").textContent = data.puntuacion;
          document.getElementById("bookSynopsis").textContent = data.sinopsis;
          document.getElementById("bookCover").src = data.cover_image || "default-cover.jpg";
 
          popupWindow.style.display = "block";
          overlay.classList.add('active');
        })
        .catch(error => console.error("Error al obtener detalles del libro:", error));
    }
 
    // Evento para cerrar la ventana emergente
    closePopup.addEventListener("click", function () {
      popupWindow.style.display = "none";
      overlay.classList.remove('active');
      searchInput.value = "";
    });
});
 
// función para cerrar el modal de 3 botones //
function closePopup() {
    document.getElementById("popupWindow").style.display = "none";
    document.querySelector(".overlay").classList.remove("active");
}
