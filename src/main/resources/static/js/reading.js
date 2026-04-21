/* Funcionalidades relacionadas con la biblioteca y lecturas en progreso */

// ------------------------------ AÑADIR A LIBROS LEIDOS ------------------------------
function addToLibrary(title) {
    console.log("📌 Intentando añadir a Libros Leídos:", title);

    // 🔹 Primero verificamos si el libro ya está en la biblioteca
    fetch("/checkIfBookExists?title=" + encodeURIComponent(title), { method: "GET" })
    .then(response => response.json())
    .then(data => {
        if (data.exists) {
            // 📌 El libro ya está en la biblioteca, mostramos solo un mensaje de alerta
            Swal.fire({
                title: 'Atención',
                text: '❌ El libro ya está en tu biblioteca.',
                icon: 'warning',
                confirmButtonText: 'OK',
                customClass: {
                    popup: 'malva-popup',
                    confirmButton: 'malva-confirm-button'
                }
            });
        } else {
            closePopup(); 
            // 📌 El libro NO está en la biblioteca, solo mostramos el modal para puntuar y reseñar
            document.getElementById("finishReadingModal").dataset.bookTitle = title;
            document.getElementById("overlayModal").style.display = "block";
            document.getElementById("finishReadingModal").style.display = "flex";

            // ✅ Limpiar la reseña anterior y las estrellas seleccionadas
            document.getElementById("reviewText").value = "";
            selectedRating = 0;
            document.querySelectorAll(".stars span").forEach(star => star.classList.remove("selected"));
             // 🔥 Limpiar la barra de búsqueda después de cerrar el modal
             document.getElementById("searchBook").value = ""; 
        }
    })
    .catch(error => console.error("❌ Error al verificar si el libro existe:", error));
}



// ------------------------------ AÑADIR A PRÓXIMAS LECTURAS ------------------------------
function addToUpcomingReads(title, coverImage) {
    const searchInput = document.getElementById("searchBook");
    fetch("/addToUpcomingReads", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        },
        body: `title=${encodeURIComponent(title)}`,
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Agregar el libro visualmente a la sección de Próximas Lecturas
            const upcomingContainer = document.querySelector(".proxLecturas");

            const img = document.createElement("img");
            img.src = coverImage;
            img.alt = title;
            img.classList.add("upcoming-book");

            upcomingContainer.appendChild(img);
            closePopup(); // 🔥 Cierra el modal después de añadir
            // 🔥 Limpiar la barra de búsqueda después de cerrar el modal
            document.getElementById("searchBook").value = ""; 
            document.getElementById("bookList").style.display = "none"; // 🔥 Oculta el desplegable
        }
        else {
            // Si no es exitoso, mostramos el mensaje (por ejemplo, "Libro leído")
            Swal.fire({
                title: 'Aviso',
                text: '⚠️ Ya has leído este libro',
                icon: 'warning',
                confirmButtonText: 'OK',
                customClass: {
                    popup: 'malva-popup',
                    confirmButton: 'malva-confirm-button'
                }
            });
        }
    })
    .catch(error => console.error("❌ Error al añadir el libro:", error));
}

// ------------------------------ CARGAR LIBROS LEIDOS Y PRÓXIMAS LECTURAS ------------------------------
function loadLibrary() {
    
    fetch("/getLibraryBooks")
    .then(response => response.json())
    .then(data => {
        const libraryContainer = document.querySelector(".leidos");
        libraryContainer.innerHTML = ""; // Limpiar el contenedor antes de añadir libros

        if (data.length === 0) {
            libraryContainer.innerHTML = "<p>Aún no has completado ningún libro 📚.</p>";
            return;
        }

        data.forEach(book => {
            const img = document.createElement("img");
            img.src = book.cover_image;
            img.alt = book.titulo;
            img.classList.add("library-book");

            // Agregar evento para abrir la ventana emergente con detalles
            img.onclick = function() {
                openPopup2(
                    book.titulo,
                    book.autor,
                    book.genero,
                    book.anioEdicion,
                    book.puntuacion,
                    book.cover_image
                );
            };

            libraryContainer.appendChild(img);
        });
    })
    .catch(error => console.error("❌ Error al cargar los libros leídos:", error));
    
}

function loadUpcomingReads() {
    fetch("/getUpcomingReads")
      .then(response => response.json())
      .then(data => {
        const upcomingContainer = document.querySelector(".proxLecturas");
        upcomingContainer.innerHTML = "";
  
        data.forEach(book => {
          const img = document.createElement("img");
          img.src = book.cover_image;
          img.alt = book.titulo;
          img.classList.add("upcoming-book");
  
          // Guardamos título y portada en atributos 'data-' para usarlos luego
          img.dataset.title = book.titulo;
          img.dataset.cover = book.cover_image;
  
          // Al hacer clic en la portada, abrimos un modal para "Comenzar a leer"
          img.addEventListener("click", function() {
            openUpcomingModal(this.dataset.title, this.dataset.cover);
          });
  
          upcomingContainer.appendChild(img);
        });
      })
      .catch(error => console.error("❌ Error al obtener los libros de Próximas Lecturas:", error));
  }
  

// ------------------------------ COMENZAR A LEER ------------------------------
document.addEventListener("DOMContentLoaded", function () {
    // Aseguramos que el botón se asigna correctamente
    document.getElementById("popupWindow").addEventListener("click", function (event) {
        if (event.target && event.target.classList.contains("btn-action")) {
            // Verificamos que se haya hecho clic en el botón "Comenzar a leer"
            if (event.target.textContent.includes("Comenzar a leer")) {
                startReading();
            }
        }
    });
});

// ------------------------------ FUNCIÓN COMENZAR A LEER ------------------------------
function startReading() {
    const bookTitle = document.getElementById("bookTitle").textContent;
    const coverImage = document.getElementById("bookCover").src;

    fetch("/getCurrentReading")
        .then(response => response.json())
        .then(data => {
            if (data.idLibro) {
                // Hay un libro en progreso, lo eliminamos
                return removeCurrentReadingBook(data.idLibro);
            }
        })
        .then(() => {
            // Ahora iniciamos el nuevo libro
            return fetch("/startReading?title=" + encodeURIComponent(bookTitle), {
                method: "POST"
            });
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const currentReading = document.getElementById("currentReading");
                const coverImageElement = document.getElementById("coverImage");
                const readingStartDate = document.getElementById("readingStartDate");
                const finishButton = document.getElementById("finishReading");

                // Título y autor
                currentReading.textContent = data.reading;
                currentReading.dataset.bookId = data.idLibro;
                finishButton.setAttribute("data-book-id", data.idLibro);

                // Portada
                coverImageElement.src = coverImage;
                coverImageElement.style.display = "block";

                // Fecha de inicio -> convertir y formatear
                if (data.fechaInicio && data.fechaInicio.trim() !== "") {
                    // Ojo: si tu backend ya devuelve algo como "2025-02-21T00:00:00"
                    // simplemente parseas con new Date(data.fechaInicio).
                    const fecha = new Date(data.fechaInicio); 
                    if (!isNaN(fecha)) {
                        const opcionesFormato = { 
                            weekday: 'long', 
                            day: 'numeric', 
                            month: 'long', 
                            year: 'numeric'
                        };
                        const fechaFormateada = fecha.toLocaleDateString("es-ES", opcionesFormato);
                        readingStartDate.textContent = `📅 Fecha de inicio: ${fechaFormateada}`;
                    } else {
                        readingStartDate.textContent = `📅 Fecha de inicio: Fecha inválida`;
                    }
                } else {
                    readingStartDate.textContent = `📅 Fecha de inicio: No disponible`;
                }

                // Mostramos sección
                readingStartDate.style.display = "block";
                finishButton.style.display = "block";

                // Cerrar el pop-up y limpiar búsqueda
                closePopup();
                document.getElementById("searchBook").value = "";
            } else {
                Swal.fire({
                    title: 'Aviso',
                    text: '⚠️ Ya has leído este libro',
                    icon: 'warning',
                    confirmButtonText: 'OK',
                    customClass: {
                        popup: 'malva-popup',
                        confirmButton: 'malva-confirm-button'
                    }
                });
            }
        })
        .catch(error => console.error("❌ Error al comenzar a leer:", error));
}


function removeCurrentReadingBook(bookId) {
    return fetch("/removeCurrentReadingBook?idLibro=" + bookId, {
        method: "POST"
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log("✅ Libro eliminado correctamente:", bookId);
            
            // 🔥 Actualizar interfaz eliminando el libro de todas las secciones
            loadLibrary();  
            loadUpcomingReads();
            loadReviews();
            loadFavorites();
            updateProgressBar();
        } else {
            console.error("❌ Error al eliminar el libro en progreso:", data.message);
        }
    })
    .catch(error => console.error("❌ Error en la eliminación del libro:", error));
}





// ------------------------------ CARGAR LIBRO QUE SE ESTÁ LEYENDO ------------------------------

function loadCurrentReading() {
    fetch("/getCurrentReading")
    .then(response => response.json())
    .then(data => {
        console.log("📌 Estado de lectura actual:", data); 

        const readingContainer = document.querySelector(".reading-container");
        const currentReading = document.getElementById("currentReading");
        const coverImage = document.getElementById("coverImage");
        const readingStartDate = document.getElementById("readingStartDate");
        const finishButton = document.getElementById("finishReading");

        if (data.idLibro && data.idLibro !== "") {
            currentReading.textContent = `${data.titulo} - ${data.autor}`;
            currentReading.dataset.bookId = data.idLibro;
            finishButton.setAttribute("data-book-id", data.idLibro);
            coverImage.src = data.cover_image;

            // 🔍 Verificar si fechaInicio tiene un valor correcto
            console.log("🕵️‍♂️ data.fechaInicio recibido:", data.fechaInicio);

            if (data.fechaInicio && data.fechaInicio.trim() !== "" && data.fechaInicio !== "undefined") {
                const fechaInicio = new Date(data.fechaInicio + "T00:00:00"); // Aseguramos formato de fecha completo
            
                // Verificar si la fecha es válida
                if (!isNaN(fechaInicio.getTime())) {
                    const opcionesFormato = {
                        weekday: 'long',  // Día de la semana
                        day: 'numeric',   // Día del mes
                        month: 'long',    // Nombre completo del mes
                        year: 'numeric'   // Año completo
                    };
            
                    // Convertir la fecha a formato largo en español
                    const fechaFormateada = fechaInicio.toLocaleDateString("es-ES", opcionesFormato);
                    readingStartDate.textContent = `📅 Fecha de inicio: ${fechaFormateada}`;
                } else {
                    console.warn("⚠️ Fecha no válida:", data.fechaInicio);
                    readingStartDate.textContent = `📅 Fecha de inicio: Fecha inválida`;
                }
            } else {
                console.warn("⚠️ data.fechaInicio está vacío o undefined");
                readingStartDate.textContent = `📅 Fecha de inicio: No disponible`;
            }
            

            // 🔥 Mostrar la sección si hay un libro en progreso
            readingContainer.style.display = "block";
            coverImage.style.display = "block";
            readingStartDate.style.display = "block";
            finishButton.style.display = "block";
        } else {
            readingContainer.style.display = "none";
        }
    })
    .catch(error => console.error("❌ Error al obtener el estado de lectura:", error));
}

document.addEventListener("DOMContentLoaded", function () {
    loadCurrentReading();
});



function openUpcomingModal(title, coverImage) {
    const upcomingModal = document.getElementById("upcomingModal");
    const upcomingTitle = document.getElementById("upcomingTitle");
    const startReadingBtn = document.getElementById("startReadingBtn");
  
    // Mostramos el título en el modal
    upcomingTitle.textContent = title;
  
    // Asignamos la acción de "Comenzar a leer"
    startReadingBtn.onclick = function() {
      // Llamamos a la función que hace el fetch al backend
      startReadingByTitle(title, coverImage);
  
      // Cerramos el modal
      closeUpcomingModal();
    };
  
    // Mostramos el modal
    upcomingModal.style.display = "block";
  }
  
  function closeUpcomingModal() {
    const upcomingModal = document.getElementById("upcomingModal");
    upcomingModal.style.display = "none";
  }
  

  // Llamaremos a esta función para recalcular y pintar la barra de progreso
function updateProgressBar() {
    fetch("/getLibraryBooks")  // O la ruta que tengas para obtener la lista de libros leídos
      .then(response => response.json())
      .then(data => {
        const count = data.length; // Número de libros completados
        // Lee el objetivo actual del span (o del localStorage)
        const targetElement = document.getElementById("targetDisplay");
        let target = parseInt(targetElement.textContent, 10) || 1;
  
        // Calcula el porcentaje
        const percentage = Math.min((count / target) * 100, 100);
  
        // Actualiza la UI
        document.getElementById("completedLabel").textContent = count;
        document.getElementById("progressBar").style.width = percentage + "%";
      })
      .catch(error => console.error("Error al obtener libros completados:", error));
  }
  
  // Al cargar la página
  document.addEventListener("DOMContentLoaded", function () {
    const targetDisplay = document.getElementById("targetDisplay");
  
    // 1) Cargar desde localStorage si existe
    const savedGoal = localStorage.getItem("readingGoal");
    if (savedGoal) {
      targetDisplay.textContent = savedGoal;
    }
  
    // 2) Actualizamos la barra al iniciar
    updateProgressBar();
  
    // 3) Cuando el usuario edite y pierda el foco (blur), guardamos y actualizamos
    targetDisplay.addEventListener("blur", function() {
      // Guardar en localStorage
      localStorage.setItem("readingGoal", this.textContent);
      // Recalcular la barra
      updateProgressBar();
    });
  
    // 4) Si el usuario presiona Enter, se finaliza la edición
    targetDisplay.addEventListener("keydown", function(e) {
      if (e.key === "Enter") {
        e.preventDefault(); // Evita saltos de línea
        this.blur();        // Dispara el evento 'blur'
      }
    });
  });
  




 