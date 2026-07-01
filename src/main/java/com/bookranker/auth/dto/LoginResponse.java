package com.bookranker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after teacher login")
public record LoginResponse(
    @Schema(description = "JWT bearer token", example = "jwt-token")
    String token
) {
}
