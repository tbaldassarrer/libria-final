document.addEventListener("DOMContentLoaded", () => {
  const contactForm = document.getElementById("contactForm");

  contactForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const message = document.getElementById("message").value.trim();

    if (!name || !email || !message) {
      mostrarSweetAlert("Error", "Por favor, completa todos los campos.", "error");
      return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const headers = {
      "Content-Type": "application/x-www-form-urlencoded",
    };

    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    try {
      const response = await fetch("/contacto", {
        method: "POST",
        headers,
        body: new URLSearchParams({ name, email, message }),
      });

      const result = await response.json();

      if (!response.ok) {
        mostrarSweetAlert("Error", result.message || "No se pudo enviar el mensaje.", "error");
        return;
      }

      mostrarSweetAlert("Enviado", result.message, "success");
      contactForm.reset();
    } catch (error) {
      mostrarSweetAlert("Error", "No se pudo conectar con el servidor.", "error");
    }
  });

  function mostrarSweetAlert(titulo, mensaje, tipo) {
    Swal.fire({
      icon: tipo,
      title: titulo,
      text: mensaje,
      confirmButtonText: "OK",
      confirmButtonColor: "#6b2db5",
      background: "linear-gradient(135deg, #a375d3, #c9b6e4)",
      color: "#ffffff",
    });
  }
});
