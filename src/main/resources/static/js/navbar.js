document.addEventListener("DOMContentLoaded", function () {
  const topBar = document.querySelector(".barra_superior");
  const toggleButton = topBar?.querySelector(".navbar-toggle");
  const topBarPanel = topBar?.querySelector(".barra_superior__panel");

  if (!topBar) {
    return;
  }

  const syncTopBarState = () => {
    topBar.classList.toggle("barra_superior--scrolled", window.scrollY > 24);
  };

  syncTopBarState();
  window.addEventListener("scroll", syncTopBarState, { passive: true });

  if (!toggleButton || !topBarPanel) {
    return;
  }

  const syncMenuState = (isOpen) => {
    topBar.classList.toggle("is-menu-open", isOpen);
    toggleButton.setAttribute("aria-expanded", String(isOpen));
    const icon = toggleButton.querySelector("i");
    if (icon) {
      icon.classList.toggle("fa-bars", !isOpen);
      icon.classList.toggle("fa-xmark", isOpen);
    }
  };

  syncMenuState(false);

  toggleButton.addEventListener("click", () => {
    syncMenuState(!topBar.classList.contains("is-menu-open"));
  });

  topBarPanel.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      if (window.innerWidth <= 700) {
        syncMenuState(false);
      }
    });
  });

  document.addEventListener("click", (event) => {
    if (window.innerWidth > 700) {
      return;
    }

    if (!topBar.contains(event.target)) {
      syncMenuState(false);
    }
  });

  window.addEventListener("resize", () => {
    if (window.innerWidth > 700) {
      syncMenuState(false);
    }
  });
});
