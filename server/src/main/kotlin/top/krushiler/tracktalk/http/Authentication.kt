package top.krushiler.tracktalk.http

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject
import top.krushiler.tracktalk.feature.auth.TokenService

internal fun Application.configureAuthentication() {
    val tokens by inject<TokenService>()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "gms"
            verifier(tokens.verifier)
            validate { credential ->
                if (tokens.isAccessToken(credential.payload)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                val header = call.request.headers[HttpHeaders.Authorization]
                val detail = if (header.isNullOrBlank()) {
                    "Not authenticated"
                } else {
                    "Invalid or expired token"
                }
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(detail))
            }
        }
    }
}

internal fun ApplicationCall.userId(): Long {
    val principal = principal<JWTPrincipal>()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Not authenticated")
    return principal.payload.getClaim("user_id").asLong()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid token payload")
}
