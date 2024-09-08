/**
 * Reasons.kt
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

enum class Reasons(val reason: String) {
    SPAM("Spamming"),
    INAPPROPRIATE_LANGUAGE("Inappropriate language"),
    HARASSMENT("Harassment"),
    ADVERTISING("Advertising"),
    TROLLING("Trolling"),
    CHAT_FLOOD("Chat flooding"),
    OTHER("Other");

    companion object
}