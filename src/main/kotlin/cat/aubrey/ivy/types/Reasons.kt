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