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
 

