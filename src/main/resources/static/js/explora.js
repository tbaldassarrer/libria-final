document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("communityReviewsModal");
    const modalClose = document.querySelector(".community-reviews-modal__close");
    const modalBackdrop = document.querySelector(".community-reviews-modal__backdrop");
    const titleElement = document.getElementById("communityReviewsTitle");
    const authorElement = document.getElementById("communityReviewsAuthor");
    const coverElement = document.getElementById("communityReviewsCover");
    const listElement = document.getElementById("communityReviewsList");
    const carousel = document.getElementById("currentReadsCarousel");
    const prevButton = document.querySelector(".community-carousel__nav--prev");
    const nextButton = document.querySelector(".community-carousel__nav--next");

    if (!modal || !titleElement || !authorElement || !coverElement || !listElement) {
        return;
    }

    function openModal() {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";
    }

    function renderEmptyState(message = "Todavia no hay comentarios de lectores.") {
        listElement.innerHTML = `<div class="community-review-empty">${message}</div>`;
    }

    function buildReviewCard(review) {
        const card = document.createElement("article");
        card.className = "community-review-card";

        const meta = document.createElement("div");
        meta.className = "community-review-card__meta";

        const user = document.createElement("p");
        user.className = "community-review-card__user";
        user.textContent = review.usuario || "Lector";

        const info = document.createElement("p");
        info.className = "community-review-card__info";
        const ratingValue = Math.max(0, parseInt(review.puntuacion || "0", 10));
        const ratingText = ratingValue > 0 ? "★".repeat(ratingValue) : "Sin puntuacion";
        info.textContent = review.fecha ? `${ratingText} · ${review.fecha}` : ratingText;

        const text = document.createElement("p");
        text.className = "community-review-card__text";
        text.textContent = review.resenia || "";

        meta.append(user, info);
        card.append(meta, text);
        return card;
    }

    function renderReviews(reviews) {
        if (!Array.isArray(reviews) || reviews.length === 0) {
            renderEmptyState();
            return;
        }

        listElement.innerHTML = "";

        reviews.slice(0, 3).forEach(review => {
            listElement.appendChild(buildReviewCard(review));
        });

        if (reviews.length <= 3) {
            return;
        }

        const hiddenContainer = document.createElement("div");
        hiddenContainer.className = "community-review-more";
        hiddenContainer.hidden = true;

        reviews.slice(3).forEach(review => {
            hiddenContainer.appendChild(buildReviewCard(review));
        });

        const expandButton = document.createElement("button");
        expandButton.type = "button";
        expandButton.className = "community-review-expand";
        expandButton.textContent = "Leer todos los comentarios";
        expandButton.addEventListener("click", function () {
            hiddenContainer.hidden = false;
            expandButton.remove();
        });

        listElement.append(expandButton, hiddenContainer);
    }

    function loadReviews(card) {
        const bookId = card.dataset.bookId;
        titleElement.textContent = card.dataset.bookTitle || "Libro";
        authorElement.textContent = card.dataset.bookAuthor || "Autor desconocido";
        coverElement.src = card.dataset.bookCover || "/images/portadaLibro.jpg";
        renderEmptyState("Cargando comentarios...");
        openModal();

        fetch(`/communityReviews?idLibro=${encodeURIComponent(bookId)}`)
            .then(response => response.json())
            .then(renderReviews)
            .catch(error => {
                console.error("Error al cargar comentarios del libro:", error);
                renderEmptyState("No se pudieron cargar los comentarios.");
            });
    }

    document.addEventListener("click", function (event) {
        const card = event.target.closest(".js-community-book");
        if (card) {
            loadReviews(card);
        }
    });

    document.addEventListener("keydown", function (event) {
        const card = event.target.closest(".js-community-book");
        if (card && (event.key === "Enter" || event.key === " ")) {
            event.preventDefault();
            loadReviews(card);
            return;
        }

        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });

    document.querySelectorAll(".js-community-book").forEach(card => {
        card.tabIndex = 0;
    });

    modalClose?.addEventListener("click", closeModal);
    modalBackdrop?.addEventListener("click", closeModal);

    if (carousel && prevButton && nextButton) {
        const scrollAmount = () => Math.max(320, Math.floor(carousel.clientWidth * 0.88));

        prevButton.addEventListener("click", function () {
            carousel.scrollBy({ left: -scrollAmount(), behavior: "smooth" });
        });

        nextButton.addEventListener("click", function () {
            carousel.scrollBy({ left: scrollAmount(), behavior: "smooth" });
        });
    }
});
