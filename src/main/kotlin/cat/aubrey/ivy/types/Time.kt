/**
 * Time.kt
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * @author FlutterMC (https://github.com/FlutterMC/)
 * @contributor Aubrey @ aubrey.rs
 * @since 2024-09-12
 * @version 1.0
 */

package cat.aubrey.ivy.types

enum class Time(val duration: String) {
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    TWO_HOURS("2h"),
    SIX_HOURS("6h"),
    TWELVE_HOURS("12h"),
    ONE_DAY("1d"),
    THREE_DAYS("3d"),
    ONE_WEEK("7d"),
    TWO_WEEKS("14d"),
    ONE_MONTH("30d"),
    PERMANENT("permanent");

    companion object {

        fun fromString(input: String): Time? {
            return entries.find { it.duration == input }
        }
    }
}