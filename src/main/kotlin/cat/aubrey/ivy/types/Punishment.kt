package cat.aubrey.ivy.types

import java.util.UUID

data class Punishment(
    val id: Int,
    val playerUUID: UUID,
    val type: Type,
    val reason: String?,
    val expiration: Long?,
    val issuer: String,
    val issuedAt: Long = System.currentTimeMillis()
) {
    enum class Type {
        MUTE, BAN, KICK
    }
}