document.addEventListener("DOMContentLoaded", function () {
  const topBar = document.querySelector(".barra_superior");

  if (!topBar) {
    return;
  }

  const syncTopBarState = () => {
    topBar.classList.toggle("barra_superior--scrolled", window.scrollY > 24);
  };

  syncTopBarState();
  window.addEventListener("scroll", syncTopBarState, { passive: true });
});
