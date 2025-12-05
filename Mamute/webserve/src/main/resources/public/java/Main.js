var imageAutal = "java/e.jfif";
var imagemAnterior = "java/Cartao.png";


function trocar()
{
document.getElementById("figura").src = imageAutal;
let aux = imageAutal;
imageAutal = imagemAnterior;
imagemAnterior = aux	
	
}
  document.addEventListener('DOMContentLoaded', function() {
    const menuButton = document.querySelector('.hamburger');
    const menu = document.querySelector('.menu-bar ul');

    menuButton.addEventListener('click', () => {
      menu.classList.toggle('active');
    });
  });
  document.addEventListener("DOMContentLoaded", function () {
  document.getElementById("meuFormulario").addEventListener("submit", async function (e) {
    e.preventDefault();

    const formData = new FormData(this);
    const urlEncoded = new URLSearchParams(formData);

    try {
     const response = await fetch("/Gravar", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: urlEncoded.toString(),
      });

      if (response.ok) {
        alert("✅ Dados enviados com sucesso!");
      } else {
        alert("❌ Erro ao enviar os dados.");
      }
    } catch (error) {
      alert("❌ Erro de conexão: " + error.message);
    }
  });
});