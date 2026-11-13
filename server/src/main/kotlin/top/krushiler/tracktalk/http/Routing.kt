package top.krushiler.tracktalk.http

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import top.krushiler.tracktalk.feature.auth.AuthService
import top.krushiler.tracktalk.feature.auth.TokenService
import top.krushiler.tracktalk.feature.auth.authRoutes
import top.krushiler.tracktalk.feature.chat.ChatService
import top.krushiler.tracktalk.feature.chat.chatByUserRoutes
import top.krushiler.tracktalk.feature.chat.chatRoutes
import top.krushiler.tracktalk.feature.friendship.FriendshipService
import top.krushiler.tracktalk.feature.friendship.friendshipByUserRoutes
import top.krushiler.tracktalk.feature.friendship.friendshipRoutes
import top.krushiler.tracktalk.feature.location.LocationService
import top.krushiler.tracktalk.feature.location.locationByUserRoutes
import top.krushiler.tracktalk.feature.location.locationRoutes
import top.krushiler.tracktalk.feature.realtime.RealtimeService
import top.krushiler.tracktalk.feature.realtime.realtimeRoutes
import top.krushiler.tracktalk.feature.user.UserService
import top.krushiler.tracktalk.feature.user.userRoutes

internal fun Application.configureRouting() {
    val auth by inject<AuthService>()
    val users by inject<UserService>()
    val friendships by inject<FriendshipService>()
    val locations by inject<LocationService>()
    val chat by inject<ChatService>()
    val realtime by inject<RealtimeService>()
    val tokens by inject<TokenService>()
    routing {
        healthRoutes()
        authRoutes(auth)
        authenticate("auth-jwt") {
            route("/users") {
                userRoutes(users)
                friendshipRoutes(friendships)
                locationRoutes(locations)
                chatRoutes(chat)
                friendshipByUserRoutes(friendships)
                chatByUserRoutes(chat)
                locationByUserRoutes(locations)
            }
        }
        realtimeRoutes(
            realtime = realtime,
            tokens = tokens,
            users = users,
        )
    }
}
