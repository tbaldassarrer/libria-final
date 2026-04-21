// Espera a que el contenido de la página cargue
document.addEventListener("DOMContentLoaded", () => {
  const contactForm = document.getElementById("contactForm");

  // Escuchar el envío del formulario
  contactForm.addEventListener("submit", function (event) {
    event.preventDefault(); // Evitar el envío por defecto

    // Obtener los valores de los campos
    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const message = document.getElementById("message").value.trim();

    // Validar que todos los campos estén llenos
    if (name === "" || email === "" || message === "") {
      mostrarSweetAlert("Error", "Por favor, completa todos los campos.", "error");
      return;
    }

    // Mostrar mensaje de éxito con SweetAlert
    mostrarSweetAlert("¡Éxito!", "Tu mensaje ha sido enviado correctamente.", "success");

    // Limpiar el formulario
    contactForm.reset();
  });

  // Función para mostrar SweetAlert2
  function mostrarSweetAlert(titulo, mensaje, tipo) {
    Swal.fire({
      icon: tipo, // "success", "error", "warning", "info"
      title: titulo,
      text: mensaje,
      confirmButtonText: "OK",
      confirmButtonColor: "#6b2db5", // Lila fuerte
      background: "linear-gradient(135deg, #a375d3, #c9b6e4)", // Fondo degradado
      color: "#ffffff", // Texto blanco
    });
  }
});
