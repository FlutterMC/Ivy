package cat.aubrey.ivy.types

data class AuditLog(
    val id: Int,
    val action: String,
    val moderator: String,
    val target: String,
    val details: String,
    val timestamp: Long
)