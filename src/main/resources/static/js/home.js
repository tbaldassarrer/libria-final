/* Funcionalidades relacionadas con el home */


// 🔄 Cargar todo al iniciar la página
document.addEventListener("DOMContentLoaded", function () {
    loadReviews();
    loadCurrentReading();
    loadLibrary();
    loadFavorites();
});

  
document.addEventListener("DOMContentLoaded", function () {
    const editBtn = document.getElementById("edit-bg-btn");
    const bgOptions = document.getElementById("bg-options");
    const closeBtn = document.querySelector(".close-header");
 
    // Mostrar/ocultar opciones de fondo
    editBtn.addEventListener("click", function () {
        bgOptions.style.display = bgOptions.style.display === "block" ? "none" : "block";
    });
 
    // Cambiar el fondo al hacer clic en una imagen
    const bgImages = document.querySelectorAll(".bg-option");
    const header = document.querySelector("header");
 
    bgImages.forEach((img) => {
        img.addEventListener("click", function () {
            header.style.backgroundImage = `url(${img.src})`;
            bgOptions.style.display = "none"; // Ocultar opciones después de seleccionar
        });
    });
 
    // Cerrar el modal al hacer clic en la "x"
    closeBtn.addEventListener("click", closeHeaderModal);
 
    // Función para cerrar el modal
    function closeHeaderModal() {
      bgOptions.style.display = "none";
    }
 
    // Función para cambiar la sección visible
    function showSection(sectionId) {
        var sections = document.querySelectorAll(".content-section");
        sections.forEach(section => section.style.display = "none"); // Oculta todas las secciones
 
        var selectedSection = document.getElementById(sectionId);
        if (selectedSection) selectedSection.style.display = "block"; // Muestra la seleccionada
    }
    
 // ///////IMAGEN PERFIL////////////
document.addEventListener("DOMContentLoaded", function () {
    const editIcon = document.getElementById("editProfileIcon");
    const profileModal = document.getElementById("profileModal");
    const profileOptions = document.querySelectorAll(".profile-option");
    const profileImg = document.getElementById("profileImg");
 
    if (editIcon) {
      editIcon.addEventListener("click", function () {
        console.log("🖊️ Editar imagen de perfil");
        profileModal.style.display = "block";
      });
    }
 
    profileOptions.forEach(option => {
      option.addEventListener("click", function () {
        profileImg.src = this.src;
        closeProfileModal();
      });
    });
 
    window.closeProfileModal = function () {
      profileModal.style.display = "none";
    };
 
    window.addEventListener("click", function (event) {
      if (event.target === profileModal) {
        closeProfileModal();
      }
    });
});
 

  // Añadir eventos a los enlaces de navegación
  document.querySelectorAll(".right__col nav ul li a").forEach(link => {
    link.addEventListener("click", function (event) {
        event.preventDefault(); // Evita que el enlace recargue la página
        const sectionId = this.id.replace("-link", ""); // Extrae el ID de la sección
        showSection(sectionId);
    });
});

// Mostrar libros leídos por defecto
showSection("library");

// Añadir clase 'active' a los enlaces de navegación
document.querySelectorAll(".right__col nav ul li a").forEach(link => {
    link.addEventListener("click", function (event) {
        event.preventDefault(); // Evita salto de página
        document.querySelectorAll(".right__col nav ul li a").forEach(l => l.classList.remove("active"));
        this.classList.add("active");
    });
});
    // Cargar datos al iniciar la página
    loadLibrary();
    loadUpcomingReads();
});
