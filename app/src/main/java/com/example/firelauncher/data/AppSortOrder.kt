package com.example.firelauncher.data

/**
 * Sort order options for the Fire TV application library.
 */
enum class AppSortOrder(val title: String, val subtitle: String) {
    ALPHABETICAL(
        title = "Alphabetical (A–Z)",
        subtitle = "Standard alphabetical order by application name"
    ),
    MOST_RECENT(
        title = "Most Recently Used",
        subtitle = "Applications opened most recently appear first"
    ),
    MOST_FREQUENT(
        title = "Most Frequently Used",
        subtitle = "Applications you launch most often appear first"
    );

    companion object {
        fun fromName(name: String?): AppSortOrder {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALPHABETICAL
        }
    }
}

/**
 * Usage metrics tracked per application package to power Recents and Frequent sorting.
 */
data class AppUsageStat(
    val packageName: String,
    val launchCount: Int = 0,
    val lastLaunchedTimeMs: Long = 0L
)
