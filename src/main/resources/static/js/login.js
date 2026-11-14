"use strict";
document.getElementById("login-form").addEventListener("submit", async event => {
    event.preventDefault();
    const button = document.getElementById("login-button");
    const errorBox = document.getElementById("login-error");
    button.disabled = true;
    errorBox.hidden = true;
    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email: document.getElementById("email").value.trim(),
                password: document.getElementById("password").value})
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || "Login failed. Please check your email and password.");
        sessionStorage.setItem("campus.accessToken", result.accessToken);
        window.location.replace("/");
    } catch (error) {
        errorBox.textContent = error.message;
        errorBox.hidden = false;
        button.disabled = false;
    }
});
