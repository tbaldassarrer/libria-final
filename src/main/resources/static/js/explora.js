const GOOGLE_BOOKS_API_KEY = 'AIzaSyCMkSGGM48kzSDDy4GSxm9YXdYFx4u7fqw';
const genres = ['fantasy', 'romance', 'science_fiction', 'mystery', 'history'];
const DEFAULT_BOOK_COVER = '/images/portadaLibro.jpg';
const BOOKS_PER_GENRE = 5;
const GOOGLE_MAX_RESULTS = 40;

const genreConfig = {
  fantasy: {
    queries: ['mejores novelas de fantasia', 'fantasia epica libros', 'libros fantasia mejor valorados'],
    seedTitles: ['El hobbit', 'La comunidad del anillo', 'Harry Potter y la piedra filosofal', 'Juego de tronos', 'El nombre del viento', 'El camino de los reyes'],
    keywords: ['fantasy', 'fantasia', 'magic', 'magia', 'epic', 'epica', 'dragon', 'wizard', 'reino']
  },
  romance: {
    queries: ['mejores novelas romanticas', 'novela romantica contemporanea', 'libros de amor mejor valorados'],
    seedTitles: ['Orgullo y prejuicio', 'Forastera', 'El cuaderno de Noah', 'Yo antes de ti', 'Rojo blanco y sangre azul', 'Cariño cuanto te odio'],
    keywords: ['romance', 'romantica', 'love story', 'love stories', 'amor', 'ficcion', 'novela'],
    exclude: ['grammar', 'linguistic', 'language', 'library of congress', 'subject headings', 'prose fiction', 'calibration', 'manual']
  },
  science_fiction: {
    queries: ['mejores libros de ciencia ficcion', 'novelas ciencia ficcion mejor valoradas', 'ciencia ficcion popular libros'],
    seedTitles: ['Dune', 'Proyecto Hail Mary', 'El marciano', 'El juego de Ender', 'Fundacion', 'Neuromante'],
    keywords: ['science fiction', 'sci-fi', 'ciencia ficcion', 'espacio', 'futuro', 'robot', 'distopia', 'planeta']
  },
  mystery: {
    queries: ['mejores novelas de misterio', 'thriller misterio libros mejor valorados', 'novela negra popular'],
    seedTitles: ['Perdida', 'La paciente silenciosa', 'Los hombres que no amaban a las mujeres', 'Diez negritos', 'El codigo Da Vinci', 'El club del crimen de los jueves'],
    keywords: ['mystery', 'misterio', 'thriller', 'detective', 'crime', 'crimen', 'suspense', 'asesinato']
  },
  history: {
    queries: ['mejores libros de historia', 'libros historia mejor valorados', 'ensayo historico popular'],
    seedTitles: ['Sapiens', 'Diario de Ana Frank', 'Los cañones de agosto', 'Equipo de rivales', 'Los hermanos Wright', 'SPQR'],
    keywords: ['history', 'historia', 'historical', 'historico', 'biography', 'biografia', 'guerra', 'mundo']
  }
};

const fallbackBooksByGenre = {
  fantasy: [
    ['El hobbit', 'J. R. R. Tolkien', '1937', 'Una aventura clasica de fantasia sobre Bilbo Bolson, un viaje inesperado y un tesoro custodiado por un dragon.', 5],
    ['La comunidad del anillo', 'J. R. R. Tolkien', '1954', 'El inicio de una de las sagas fantasticas mas influyentes, con la mision de destruir el Anillo Unico.', 5],
    ['Harry Potter y la piedra filosofal', 'J. K. Rowling', '1997', 'Un joven descubre que es mago y entra en Hogwarts, donde empieza una aventura llena de misterio y magia.', 5],
    ['El nombre del viento', 'Patrick Rothfuss', '2007', 'Kvothe narra su vida: musica, magia, universidad y leyenda en una novela de fantasia muy querida.', 5],
    ['El camino de los reyes', 'Brandon Sanderson', '2010', 'Fantasia epica de gran escala con guerras, ordenes antiguas y personajes marcados por el destino.', 5]
  ],
  romance: [
    ['Orgullo y prejuicio', 'Jane Austen', '1813', 'Una historia brillante sobre amor, orgullo, prejuicios sociales y segundas oportunidades.', 5],
    ['Yo antes de ti', 'Jojo Moyes', '2012', 'Una novela romantica emotiva sobre Lou, Will y una relacion capaz de cambiar sus vidas.', 4],
    ['El cuaderno de Noah', 'Nicholas Sparks', '1996', 'Una historia de amor intensa y persistente marcada por el recuerdo, el tiempo y la fidelidad emocional.', 4],
    ['Forastera', 'Diana Gabaldon', '1991', 'Romance historico, viajes en el tiempo y aventura en la Escocia del siglo XVIII.', 4],
    ['Rojo, blanco y sangre azul', 'Casey McQuiston', '2019', 'Comedia romantica contemporanea sobre una relacion inesperada entre el hijo de la presidenta y un principe britanico.', 4]
  ],
  science_fiction: [
    ['Dune', 'Frank Herbert', '1965', 'Politica, religion, ecologia y poder en Arrakis, el planeta mas codiciado del imperio.', 5],
    ['Proyecto Hail Mary', 'Andy Weir', '2021', 'Un astronauta despierta solo en una mision desesperada para salvar a la humanidad.', 5],
    ['El marciano', 'Andy Weir', '2011', 'Un astronauta queda atrapado en Marte y debe sobrevivir usando ciencia, ingenio y humor.', 5],
    ['El juego de Ender', 'Orson Scott Card', '1985', 'Un joven estratega es entrenado para una guerra decisiva contra una amenaza alienigena.', 4],
    ['Fundacion', 'Isaac Asimov', '1951', 'Una saga clave de ciencia ficcion sobre la caida de un imperio galactico y la ciencia de predecir el futuro.', 4]
  ],
  mystery: [
    ['La paciente silenciosa', 'Alex Michaelides', '2019', 'Un thriller psicologico sobre una mujer que deja de hablar tras un crimen y el terapeuta que intenta descubrir la verdad.', 5],
    ['Perdida', 'Gillian Flynn', '2012', 'Un matrimonio toxico, una desaparicion y una investigacion llena de giros.', 5],
    ['Los hombres que no amaban a las mujeres', 'Stieg Larsson', '2005', 'Misterio, investigacion periodistica y secretos familiares con Lisbeth Salander y Mikael Blomkvist.', 5],
    ['Diez negritos', 'Agatha Christie', '1939', 'Diez desconocidos son invitados a una isla donde empiezan a morir uno a uno.', 5],
    ['El codigo Da Vinci', 'Dan Brown', '2003', 'Un thriller de simbolos, arte, sociedades secretas y una carrera contrarreloj por Europa.', 4]
  ],
  history: [
    ['Sapiens', 'Yuval Noah Harari', '2011', 'Un recorrido divulgativo por la historia de la humanidad, desde los primeros sapiens hasta el mundo moderno.', 5],
    ['Diario de Ana Frank', 'Ana Frank', '1947', 'Testimonio imprescindible de una adolescente judia escondida durante la ocupacion nazi.', 5],
    ['SPQR', 'Mary Beard', '2015', 'Una mirada clara y apasionante a la historia de la antigua Roma.', 5],
    ['Los cañones de agosto', 'Barbara W. Tuchman', '1962', 'Relato historico sobre las primeras semanas de la Primera Guerra Mundial.', 4],
    ['Los hermanos Wright', 'David McCullough', '2015', 'La historia de los pioneros que hicieron posible el vuelo moderno.', 4]
  ]
};

async function fetchBooksByGenre(genre) {
  const fixedBooks = (fallbackBooksByGenre[genre] || [])
    .slice(0, BOOKS_PER_GENRE)
    .map(book => createFallbackBook(book, genre));

  return Promise.all(fixedBooks.map(enrichBookFromGoogle));
}

function buildGoogleBooksUrl(query, withApiKey, maxResults = GOOGLE_MAX_RESULTS) {
  const params = new URLSearchParams({
    q: query,
    langRestrict: 'es',
    orderBy: 'relevance',
    maxResults: String(maxResults),
    printType: 'books'
  });

  if (withApiKey) {
    params.set('key', GOOGLE_BOOKS_API_KEY);
  }

  return `https://www.googleapis.com/books/v1/volumes?${params.toString()}`;
}

async function enrichBookFromGoogle(book) {
  const info = book.volumeInfo || {};
  const title = info.title || '';
  const author = info.authors?.[0] || '';
  const attempts = [
    buildGoogleBooksUrl(`intitle:${title} inauthor:${author}`, true, 5),
    buildGoogleBooksUrl(`intitle:${title}`, true, 5),
    buildGoogleBooksUrl(`intitle:${title} inauthor:${author}`, false, 5)
  ];

  for (const url of attempts) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Google Books HTTP ${response.status}`);
      }

      const data = await response.json();
      const match = findBestGoogleMatch(book, data.items || []);

      if (match) {
        return mergeBookData(book, match);
      }
    } catch (error) {
      console.error('Error enriching book from Google Books:', error);
    }
  }

  return book;
}

function findBestGoogleMatch(baseBook, candidates) {
  const baseInfo = baseBook.volumeInfo || {};
  const baseTitle = normalizeText(baseInfo.title);
  const baseAuthor = normalizeText(baseInfo.authors?.[0]);

  return candidates.find(candidate => {
    const info = candidate.volumeInfo || {};
    const candidateTitle = normalizeText(info.title);
    const candidateAuthors = normalizeText((info.authors || []).join(' '));
    const titleMatches = candidateTitle.includes(baseTitle) || baseTitle.includes(candidateTitle);
    const authorMatches = !baseAuthor || candidateAuthors.includes(baseAuthor.split(' ')[0]);

    return isSpanishBook(candidate) && titleMatches && authorMatches;
  }) || candidates.find(candidate => isSpanishBook(candidate) && candidate.volumeInfo?.imageLinks?.thumbnail);
}

function mergeBookData(baseBook, googleBook) {
  const baseInfo = baseBook.volumeInfo || {};
  const googleInfo = googleBook.volumeInfo || {};

  return {
    ...baseBook,
    id: googleBook.id || baseBook.id,
    volumeInfo: {
      ...baseInfo,
      imageLinks: googleInfo.imageLinks?.thumbnail ? googleInfo.imageLinks : baseInfo.imageLinks,
      publishedDate: googleInfo.publishedDate || baseInfo.publishedDate,
      averageRating: googleInfo.averageRating || baseInfo.averageRating,
      ratingsCount: googleInfo.ratingsCount || baseInfo.ratingsCount,
      categories: googleInfo.categories || baseInfo.categories,
      language: 'es'
    }
  };
}

function rankBooksForGenre(books, config) {
  const seen = new Set();
  const uniqueBooks = [];

  books.forEach(book => {
    const info = book.volumeInfo || {};
    const key = book.id || normalizeText(`${info.title || ''}-${info.authors?.join(',') || ''}`);

    if (!key || seen.has(key)) {
      return;
    }

    seen.add(key);
    uniqueBooks.push(book);
  });

  const relevantBooks = uniqueBooks
    .filter(book => isRelevantForGenre(book, config))
    .filter(isSpanishBook)
    .filter(hasEnoughBookQuality);

  const pool = relevantBooks.length >= BOOKS_PER_GENRE
    ? relevantBooks
    : uniqueBooks.filter(isSpanishBook).filter(hasEnoughBookQuality);

  return pool
    .sort((a, b) => getBookScore(b, config) - getBookScore(a, config))
    .slice(0, BOOKS_PER_GENRE);
}

function isRelevantForGenre(book, config) {
  const info = book.volumeInfo || {};
  const searchableText = normalizeText([
    info.title,
    info.subtitle,
    info.description,
    ...(info.categories || [])
  ].filter(Boolean).join(' '));

  const hasGenreKeyword = config.keywords.some(keyword => searchableText.includes(normalizeText(keyword)));
  const hasExcludedKeyword = (config.exclude || []).some(keyword => searchableText.includes(normalizeText(keyword)));

  return hasGenreKeyword && !hasExcludedKeyword;
}

function hasEnoughBookQuality(book) {
  const info = book.volumeInfo || {};
  const year = Number(info.publishedDate?.split('-')[0]) || 0;

  return Boolean(
    info.title
    && Array.isArray(info.authors)
    && info.authors.length > 0
    && info.imageLinks?.thumbnail
    && info.description
    && year >= 1900
  );
}

function isSpanishBook(book) {
  return (book.volumeInfo?.language || '').toLowerCase() === 'es';
}

function getBookScore(book, config) {
  const info = book.volumeInfo || {};
  const text = normalizeText([
    info.title,
    info.subtitle,
    info.description,
    ...(info.categories || [])
  ].filter(Boolean).join(' '));
  const rating = Number(info.averageRating) || 0;
  const ratingsCount = Number(info.ratingsCount) || 0;
  const year = Number(info.publishedDate?.split('-')[0]) || 0;
  const keywordMatches = config.keywords.filter(keyword => text.includes(normalizeText(keyword))).length;
  const recentBonus = year >= 2000 ? 0.4 : 0;
  const spanishBonus = isSpanishBook(book) ? 6 : 0;
  const popularityBonus = Math.min(Math.log10(ratingsCount + 1), 3);

  return (rating * 12) + (popularityBonus * 3) + (keywordMatches * 2) + recentBonus + spanishBonus;
}

function normalizeText(text) {
  return String(text || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

function fillWithFallbackBooks(books, genre) {
  const results = [...books];
  const seenTitles = new Set(results.map(book => normalizeText(book.volumeInfo?.title)));

  for (const fallbackBook of fallbackBooksByGenre[genre] || []) {
    if (results.length >= BOOKS_PER_GENRE) {
      break;
    }

    const book = createFallbackBook(fallbackBook, genre);
    const titleKey = normalizeText(book.volumeInfo.title);

    if (!seenTitles.has(titleKey)) {
      results.push(book);
      seenTitles.add(titleKey);
    }
  }

  return results.slice(0, BOOKS_PER_GENRE);
}

function createFallbackBook([title, author, year, description, rating], genre) {
  return {
    id: `fallback-${genre}-${normalizeText(title).replace(/\s+/g, '-')}`,
    volumeInfo: {
      title,
      authors: [author],
      publishedDate: year,
      averageRating: rating,
      ratingsCount: 1000,
      description,
      categories: [getGenreName(genre)],
      language: 'es',
      imageLinks: {
        thumbnail: DEFAULT_BOOK_COVER
      }
    }
  };
}

function getGenreName(genre) {
  const names = {
    fantasy: 'Fantasia',
    romance: 'Romance',
    science_fiction: 'Ciencia Ficcion',
    mystery: 'Misterio',
    history: 'Historia'
  };

  return names[genre] || 'Libro';
}

function renderBooks(genre, books) {
  const section = document.getElementById(`${genre}-books`);
  if (!section) {
    return;
  }

  section.innerHTML = '';
  if (section.nextElementSibling?.classList.contains('load-more-books')) {
    section.nextElementSibling.remove();
  }

  if (!books.length) {
    section.innerHTML = '<p>No se pudieron cargar libros desde Google Books ahora mismo.</p>';
    return;
  }

  books.forEach((book, index) => {
    const info = book.volumeInfo || {};
    const title = info.title || 'Título desconocido';
    const bookCard = document.createElement('div');
    bookCard.classList.add('book-card');
    bookCard.setAttribute('role', 'button');
    bookCard.setAttribute('tabindex', '0');

    const bookImage = (info.imageLinks?.thumbnail || DEFAULT_BOOK_COVER).replace('http://', 'https://');

    bookCard.innerHTML = `
      <img src="${bookImage}" alt="${title}" />
      <h3>${title}</h3>
      <p><strong>Autor:</strong> ${info.authors?.join(', ') || 'Desconocido'}</p>
      <p><strong>Año de Edición:</strong> ${info.publishedDate?.split('-')[0] || 'Desconocido'}</p>
      <p><strong>Puntuación:</strong> ${info.averageRating || 'N/A'}</p>
      <p><strong>Sinopsis:</strong> ${info.description ? info.description.slice(0, 100) + '...' : 'Sin descripción disponible.'}</p>
    `;

    const openBook = () => openExploraBookModal(book);

    bookCard.addEventListener('click', openBook);
    bookCard.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openBook();
      }
    });

    if (window.matchMedia('(max-width: 520px)').matches && index > 0) {
      bookCard.classList.add('is-hidden-mobile');
    }

    section.appendChild(bookCard);
  });

  addLoadMoreButton(section);
}

document.addEventListener('DOMContentLoaded', async () => {
  setupExploraModal();

  await Promise.all(genres.map(async genre => {
    const books = await fetchBooksByGenre(genre);
    renderBooks(genre, books);
  }));
});

function addLoadMoreButton(section) {
  const hiddenCards = () => Array.from(section.querySelectorAll('.book-card.is-hidden-mobile'));

  if (!window.matchMedia('(max-width: 520px)').matches || hiddenCards().length === 0) {
    return;
  }

  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'load-more-books';
  button.textContent = 'Ver más';

  button.addEventListener('click', () => {
    hiddenCards().slice(0, 4).forEach(card => card.classList.remove('is-hidden-mobile'));

    if (hiddenCards().length === 0) {
      button.remove();
    }
  });

  section.after(button);
}

function setupExploraModal() {
  const modal = document.getElementById('exploraBookModal');
  const closeButton = document.getElementById('closeExploraModal');

  if (!modal || !closeButton) {
    return;
  }

  closeButton.addEventListener('click', closeExploraBookModal);
  modal.addEventListener('click', event => {
    if (event.target === modal) {
      closeExploraBookModal();
    }
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && modal.classList.contains('is-open')) {
      closeExploraBookModal();
    }
  });
}

function openExploraBookModal(book) {
  const info = book.volumeInfo || {};
  const modal = document.getElementById('exploraBookModal');
  const cover = document.getElementById('exploraModalCover');

  if (!modal || !cover) {
    return;
  }

  const title = info.title || 'Título desconocido';
  const image = (info.imageLinks?.thumbnail || DEFAULT_BOOK_COVER).replace('http://', 'https://');
  const categories = Array.isArray(info.categories) && info.categories.length > 0
    ? info.categories.join(', ')
    : 'Desconocido';
  const year = info.publishedDate?.split('-')[0] || 'Desconocido';
  const rating = info.averageRating ? `${info.averageRating} / 5` : 'N/A';

  cover.src = image;
  cover.alt = title;
  document.getElementById('exploraModalTitle').textContent = title;
  document.getElementById('exploraModalAuthor').textContent = info.authors?.join(', ') || 'Desconocido';
  document.getElementById('exploraModalGenre').textContent = categories;
  document.getElementById('exploraModalYear').textContent = year;
  document.getElementById('exploraModalRating').textContent = rating;
  document.getElementById('exploraModalDescription').textContent = info.description || 'Sin descripción disponible.';

  modal.classList.add('is-open');
  modal.setAttribute('aria-hidden', 'false');
  document.body.classList.add('modal-open');
}

function closeExploraBookModal() {
  const modal = document.getElementById('exploraBookModal');
  if (!modal) {
    return;
  }

  modal.classList.remove('is-open');
  modal.setAttribute('aria-hidden', 'true');
  document.body.classList.remove('modal-open');
}
