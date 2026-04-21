const API_KEY = 'AIzaSyCMkSGGM48kzSDDy4GSxm9YXdYFx4u7fqw';
const genres = ['fantasy', 'romance', 'science_fiction', 'mystery', 'history']; // Nuevas categorías añadidas

// Obtener libros desde la API de Google Books por género
async function fetchBooksByGenre(genre) {
  const url = `https://www.googleapis.com/books/v1/volumes?q=subject:${genre}&langRestrict=es&orderBy=relevance&maxResults=5&key=${API_KEY}`;
  try {
    const response = await fetch(url);
    const data = await response.json();
    return data.items || [];
  } catch (error) {
    console.error('Error fetching books:', error);
  }
}

// Renderizar los libros dinámicamente
// Ruta de la imagen por defecto
const DEFAULT_BOOK_COVER = '/images/portadaLibro.jpg';


function renderBooks(genre, books) {
  const section = document.getElementById(`${genre}-books`);
  section.innerHTML = ''; // Limpiar resultados anteriores

  books.forEach(book => {
    const info = book.volumeInfo;
    const bookCard = document.createElement('div');
    bookCard.classList.add('book-card');

    // Usar imagen por defecto si no existe la portada
    const bookImage = info.imageLinks?.thumbnail || DEFAULT_BOOK_COVER;

    bookCard.innerHTML = `
      <img src="${bookImage}" alt="${info.title}" />
      <h3>${info.title}</h3>
      <p><strong>Autor:</strong> ${info.authors?.join(', ') || 'Desconocido'}</p>
      <p><strong>Año de Edición:</strong> ${info.publishedDate?.split('-')[0] || 'Desconocido'}</p>
      <p><strong>Puntuación:</strong> ${info.averageRating || 'N/A'} ⭐</p>
      <p><strong>Sinopsis:</strong> ${info.description ? info.description.slice(0, 100) + '...' : 'Sin descripción disponible.'}</p>
    `;

    section.appendChild(bookCard);
  });
}


// Cargar los libros al iniciar la página
document.addEventListener('DOMContentLoaded', async () => {
  for (let genre of genres) {
    const books = await fetchBooksByGenre(genre);
    renderBooks(genre, books);
  }
});
