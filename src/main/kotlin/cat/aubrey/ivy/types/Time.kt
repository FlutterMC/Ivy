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