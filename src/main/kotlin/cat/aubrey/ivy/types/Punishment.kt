/**
 * Punishment.kt
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * @author FlutterMC (https://github.com/FlutterMC/)
 * @contributor Aubrey @ aubrey.rs
 * @since 2024-09-08
 * @version 1.0
 */

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