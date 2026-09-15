package com.almoxarifado.api.auth;

public record LoginResponse(String token, String usuario, Role role, String avatar) {
}
