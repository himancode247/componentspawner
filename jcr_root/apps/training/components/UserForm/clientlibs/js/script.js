document.addEventListener("DOMContentLoaded", function () {

    const btn = document.querySelector(".userform-component__button");

    console.log("Button found:", btn);

    if (btn && window.trainingData && window.trainingData.button) {
        btn.innerText = window.trainingData.button;
    }

});