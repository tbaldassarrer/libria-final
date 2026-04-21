/* Gestión de reseñas y puntuaciones */

// //////////////////////// MODAL PUNTUAR Y RESEÑA AL FINALIZAR UN LIBRO //////////////////////
let selectedRating = 0; // Variable para almacenar la puntuación seleccionada

// Abre el modal al hacer clic en "Finalizar lectura"
document.addEventListener("click", function (event) {
    if (event.target && event.target.id === "finishReading") {
        document.getElementById("overlayModal").style.display = "block";
        document.getElementById("finishReadingModal").style.display = "block";
    }
});

// Cierra el modal
function closeFinishReadingModal() {
    const overlay = document.getElementById("overlayModal");
    const modal = document.getElementById("finishReadingModal");

    if (overlay) overlay.style.display = "none";
    if (modal) modal.style.display = "none";

    // 🔥 Limpiar la reseña y las estrellas seleccionadas
    document.getElementById("reviewText").value = "";
    selectedRating = 0;
    document.querySelectorAll(".stars span").forEach(star => star.classList.remove("selected"));
}


// ------------------------------ SISTEMA DE ESTRELLAS MEJORADO ------------------------------
document.addEventListener("DOMContentLoaded", function () {
    const stars = document.querySelectorAll(".stars span");

    stars.forEach((star) => {
        const ratingValue = parseInt(star.getAttribute("data-value"));

        // Evento: Hover (rellenar temporalmente)
        star.addEventListener("mouseover", function () {
            highlightStars(ratingValue);
        });

        // Evento: Salir del hover (restaurar estado real)
        star.addEventListener("mouseout", function () {
            highlightStars(selectedRating);
        });

        // Evento: Clic (guardar selección)
        star.addEventListener("click", function () {
            selectedRating = ratingValue; // Guarda la puntuación seleccionada
            highlightStars(selectedRating); // Asegura que se quede seleccionada
        });
    });

    function highlightStars(rating) {
        stars.forEach((star) => {
            const starValue = parseInt(star.getAttribute("data-value"));
            if (starValue <= rating) {
                star.classList.add("selected"); // Estrella dorada
            } else {
                star.classList.remove("selected"); // Estrella gris
            }
        });
    }
});

// ------------------------------ ENVÍO DE RESEÑA ------------------------------

function submitReview() {
    const review = document.getElementById("reviewText").value.trim();
    const bookId = document.getElementById("finishReading").getAttribute("data-book-id");
    const bookTitle = document.getElementById("finishReadingModal").dataset.bookTitle; // ✅ Título si viene de "Libros Leídos"
    const rating = selectedRating;

    console.log("📌 Enviando reseña...");
    console.log("📖 Libro ID:", bookId);
    console.log("📖 Libro Título:", bookTitle);

    if (!review || rating === 0) {
        Swal.fire({
            title: 'Atención',
            text: '⚠️ Por favor, escribe una reseña y selecciona una puntuación.',
            icon: 'warning',
            confirmButtonText: 'OK',
            customClass: {
              popup: 'malva-popup',
              confirmButton: 'malva-confirm-button'
            }
          });
          
        return;
    }

    let requestBody = "";
    let endpoint = "";

    if (bookId && bookId !== "") {
        // ✅ Enviar reseña para un libro en progreso (lectura actual)
        requestBody = `idLibro=${encodeURIComponent(bookId)}&rating=${rating}&review=${encodeURIComponent(review)}`;
        endpoint = "/finishReading";
    } else if (bookTitle && bookTitle !== "") {
        // ✅ Enviar reseña para un libro añadido a "Libros Leídos" (nueva ruta que maneja ambos en un solo request)
        requestBody = `title=${encodeURIComponent(bookTitle)}&resenia=${encodeURIComponent(review)}&puntuacion=${rating}`;
        endpoint = "/addToLibraryWithReview"; 
    } else {
        Swal.fire({
            title: 'Error',
            text: '❌ Error: No se encontró un libro válido para guardar la reseña.',
            icon: 'error',
            confirmButtonText: 'OK',
            customClass: {
              popup: 'malva-popup',
              confirmButton: 'malva-confirm-button'
            }
          });
                  return;
    }

    fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: requestBody,
    })
    .then(response => response.json())
    .then(data => {
        console.log("📌 Respuesta del backend:", data);

        if (data.success) {
            if (bookId && bookId !== "") {
                // ✅ Si la reseña es para un libro en progreso, actualiza la interfaz
                const currentReading = document.getElementById("currentReading");
                const coverImage = document.getElementById("coverImage");
                const readingStartDate = document.getElementById("readingStartDate");
                const finishButton = document.getElementById("finishReading");
                const actualReading = document.getElementById("reading-text");

                if (currentReading) {
                    currentReading.textContent = "Buscando un libro nuevo... 🔎";
                    currentReading.removeAttribute("data-book-id");
                }
                if (finishButton) finishButton.setAttribute("data-book-id", "");
                if (coverImage) coverImage.src = "/images/LIBRiA.png";
                if (readingStartDate) readingStartDate.textContent = "--";

                // 🔥 Ocultar elementos de forma segura
                setTimeout(() => {
                    if (coverImage) coverImage.style.display = "none";
                    if (readingStartDate) readingStartDate.style.display = "none";
                    if (finishButton) finishButton.style.display = "none";
                    if (actualReading) actualReading.style.display = "none";
                }, 100);
            } else if (bookTitle && bookTitle !== "") {
                // ✅ Si la reseña es para un libro nuevo en "Libros Leídos", añadirlo a la UI
                const leidos = document.querySelector(".leidos");
                const img = document.createElement("img");
                img.src = data.cover_image;
                img.alt = bookTitle;
                img.classList.add("book-thumbnail");
                leidos.appendChild(img);
            }

            closeFinishReadingModal();

            // ✅ Actualizar vistas
            loadLibrary();
            loadReviews();
            loadCurrentReading();
            if (rating === 5) {
                loadFavorites();
            }
            updateProgressBar();

            Swal.fire({
                title: 'Éxito',
                text: '✅ Reseña y libro añadidos correctamente.',
                icon: 'success',
                confirmButtonText: 'OK',
                customClass: {
                  popup: 'malva-popup',
                  confirmButton: 'malva-confirm-button'
                }
              });
              
            
        } else {
            Swal.fire({
                title: 'Error',
                text: '❌ Error al guardar la reseña: ' + data.message,
                icon: 'error',
                confirmButtonText: 'OK',
                customClass: {
                  popup: 'malva-popup',
                  confirmButton: 'malva-confirm-button'
                }
              });
                          
        }
    })
    .catch(error => {
        Swal.fire({
            title: 'Error',
            text: '❌ Error en la conexión con el servidor. Intenta de nuevo. ',
            icon: 'error',
            confirmButtonText: 'OK',
            customClass: {
              popup: 'malva-popup',
              confirmButton: 'malva-confirm-button'
            }
          });
          
    });
}

// función para añadir reseña y luego añadir el libro a "Libros leídos" //
function submitReviewAndAddBook(title) {
    const review = document.getElementById("reviewText").value.trim();
    const rating = selectedRating;

    if (!review || rating === 0) {
        Swal.fire({
            title: 'Atención',
            text: '⚠️ Por favor, escribe una reseña y selecciona una puntuación.',
            icon: 'warning',
            confirmButtonText: 'OK',
            customClass: {
              popup: 'malva-popup',
              confirmButton: 'malva-confirm-button'
            }
        });
        return;
    }

    fetch("/addToLibraryWithReview", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            title: title,
            resenia: review,
            puntuacion: rating
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Actualizamos la interfaz sin recargar la página
            const leidos = document.querySelector(".leidos");
            const img = document.createElement("img");
            img.src = data.cover_image;
            img.alt = title;
            img.classList.add("book-thumbnail");
            leidos.appendChild(img);

            closeFinishReadingModal();
            updateProgressBar();
            loadLibrary();
            loadReviews();

            if (rating === 5) {
                const favoritosContainer = document.querySelector(".favoritos");
                const favImg = document.createElement("img");
                favImg.src = data.cover_image;
                favImg.alt = title;
                favImg.classList.add("favorite-book");
                favoritosContainer.appendChild(favImg);
            }
            closeFinishReadingModal();
        } else {
            // Si el mensaje indica que el libro ya está en la biblioteca
            if (data.message.includes("El libro ya está en tu biblioteca")) {
                closeFinishReadingModal();
                Swal.fire({
                    title: 'Información',
                    text: data.message,
                    icon: 'info',
                    confirmButtonText: 'OK',
                    customClass: {
                        popup: 'malva-popup',
                        confirmButton: 'malva-confirm-button'
                    }
                });
            } else {
                Swal.fire({
                    title: 'Error',
                    text: '❌ Error al guardar la reseña: ' + data.message,
                    icon: 'error',
                    confirmButtonText: 'OK',
                    customClass: {
                        popup: 'malva-popup',
                        confirmButton: 'malva-confirm-button'
                    }
                });
            }
        }
    })
    .catch(error => console.error("❌ Error al añadir libro:", error));
}



// función para centralizar el envío de datos //

function sendReviewToServer(endpoint, requestBody) {
    fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: requestBody
    })
    .then(response => response.json())
    .then(data => {
        console.log("📌 Respuesta del backend:", data);

        if (data.success) {
            Swal.fire({
                title: 'Éxito',
                text: '✅ Reseña guardada correctamente.',
                icon: 'success',
                confirmButtonText: 'OK',
                customClass: {
                  popup: 'malva-popup',
                  confirmButton: 'malva-confirm-button'
                }
              });
                          closeFinishReadingModal();
            
            // ✅ Actualizar vistas
            loadLibrary();
            loadReviews();
            loadCurrentReading();
            if (selectedRating === 5) {
                loadFavorites();
            }
            updateProgressBar();
        } else {
            Swal.fire({
                title: 'Error',
                text: '❌ Error al guardar la reseña: ' + data.message,
                icon: 'error',
                confirmButtonText: 'OK',
                customClass: {
                  popup: 'malva-popup',
                  confirmButton: 'malva-confirm-button'
                }
              });
                      }
    })
    .catch(error => {
        console.error("❌ Error al enviar la reseña:", error);
        Swal.fire({
            title: 'Error',
            text: '❌ Error en la conexión con el servidor. Intenta de nuevo.',
            icon: 'error',
            confirmButtonText: 'OK',
            customClass: {
              popup: 'malva-popup',
              confirmButton: 'malva-confirm-button'
            }
          });
              });
}


// 👉 Función para añadir la reseña a la página sin recargar
function addReviewToPage(reviewText, rating) {
    const reviewsContainer = document.querySelector(".resenias");

    // Crear el contenedor de la reseña
    const reviewCard = document.createElement("div");
    reviewCard.classList.add("resenia-card");

    // Agregar las estrellas según la puntuación
    let starsHTML = "";
    for (let i = 0; i < rating; i++) {
        starsHTML += "⭐";
    }

    // Crear el contenido de la reseña
    reviewCard.innerHTML = `
        <p>"${reviewText}"</p>
        <div class="resenia-autor"> ${starsHTML}</div>
    `;

    // Agregar la nueva reseña al contenedor
    reviewsContainer.prepend(reviewCard);
}

// 🔄 Cargar reseñas guardadas desde el backend al iniciar la página
function loadReviews() {
    fetch("/getReviews")
    .then(response => response.json())
    .then(data => {
        const reviewsContainer = document.querySelector(".resenias");
        reviewsContainer.innerHTML = ""; // Limpiar contenido anterior

        data.forEach(review => {
            const reviewCard = document.createElement("div");
            reviewCard.classList.add("resenia-card");

            // Generar las estrellas según la puntuación
            let starsHTML = "";
            for (let i = 0; i < parseInt(review.puntuacion); i++) {
                starsHTML += "⭐";
            }

            reviewCard.innerHTML = `
                <p>"${review.resenia}"</p>
                <div class="resenia-autor">${review.titulo} - ${starsHTML}</div>
            `;

            reviewsContainer.appendChild(reviewCard);
        });
    })
    .catch(error => console.error("❌ Error al cargar reseñas:", error));
}

// 🔄 Cargar la sección "Lectura Actual" después de completar un libro
function loadCurrentReading() {
    fetch("/getCurrentReading")
    .then(response => response.json())
    .then(data => {
        console.log("📌 Estado de lectura actual:", data);

        const currentReading = document.getElementById("currentReading");
        const coverImage = document.getElementById("coverImage");
        const readingStartDate = document.getElementById("readingStartDate");
        const finishButton = document.getElementById("finishReading");
        const actualReading = document.getElementById("reading-text");

        if (data.idLibro && data.idLibro !== "") {
            if (currentReading) {
                currentReading.textContent = `${data.titulo} - ${data.autor}`;
                currentReading.dataset.bookId = data.idLibro;
            }
            if (finishButton) finishButton.setAttribute("data-book-id", data.idLibro);
            if (coverImage) coverImage.src = data.cover_image;
            if (readingStartDate) readingStartDate.textContent = `📅 Fecha de inicio: ${data.fechaInicio}`;

            if (coverImage) coverImage.style.display = "block";
            if (readingStartDate) readingStartDate.style.display = "block";
            if (finishButton) finishButton.style.display = "block";
        } else {
            if (currentReading) {
                currentReading.textContent = "Buscando un libro nuevo... 🔎";
                currentReading.removeAttribute("data-book-id");
            }
            if (finishButton) finishButton.setAttribute("data-book-id", "");

            if (readingStartDate) readingStartDate.textContent = "--";

            if (coverImage) coverImage.style.display = "none";
            if (readingStartDate) readingStartDate.style.display = "none";
            if (finishButton) finishButton.style.display = "none";
            if (actualReading) actualReading.style.display = "none";  // 🔥 Evita el error aquí
        }
    })
    .catch(error => console.error("❌ Error al obtener el estado de lectura:", error));
}

// Asegurar que se ejecuta cuando el DOM está listo
document.addEventListener("DOMContentLoaded", loadCurrentReading);
