// Cargar libros favoritos
function loadFavorites() {
    fetch("/getFavorites")
    .then(response => response.json())
    .then(data => {
        const favoritesContainer = document.querySelector(".favoritos");
        favoritesContainer.innerHTML = "";
 
        if (data.length === 0) {
            favoritesContainer.innerHTML = "<p>No tienes libros favoritos 😔</p>";
            return;
        }
 
        data.forEach(book => {
            const img = document.createElement("img");
            img.src = book.cover_image;
            img.alt = book.titulo;
            img.classList.add("favorite-book");
            favoritesContainer.appendChild(img);
        });
    })
    .catch(error => console.error("❌ Error al obtener los libros favoritos:", error));
}