package com.example.firelauncher.data

/**
 * Pure (Android-free) helpers for encoding launcher preferences and manipulating
 * favourite ordering. Kept separate from DataStore so they are unit-testable on the JVM.
 */
object PrefsCodec {

    private const val LIST_SEPARATOR = "\n"
    private const val PAIR_SEPARATOR = "|"
    const val MAX_LABEL_LENGTH = 40

    fun decodeList(raw: String?): List<String> =
        raw?.split(LIST_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()

    fun encodeList(items: List<String>): String =
        items.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(LIST_SEPARATOR)

    /** Strips delimiter characters and bounds length so a label can never corrupt stored data. */
    fun sanitizeLabel(label: String): String =
        label.replace(PAIR_SEPARATOR, "-")
            .replace(LIST_SEPARATOR, " ")
            .replace("\r", " ")
            .trim()
            .take(MAX_LABEL_LENGTH)

    fun encodeLabel(packageName: String, label: String): String =
        "${packageName.replace(PAIR_SEPARATOR, "_").trim()}$PAIR_SEPARATOR${sanitizeLabel(label)}"

    fun decodeLabels(entries: Set<String>): Map<String, String> =
        entries.mapNotNull { entry ->
            val idx = entry.indexOf(PAIR_SEPARATOR)
            if (idx <= 0) return@mapNotNull null
            val pkg = entry.substring(0, idx).trim()
            val label = entry.substring(idx + 1).trim()
            if (pkg.isEmpty() || label.isEmpty()) null else pkg to label
        }.toMap()

    /** Removes any existing entry for [packageName] from a label set. */
    fun withoutLabel(entries: Set<String>, packageName: String): Set<String> =
        entries.filterNot { it.startsWith("$packageName$PAIR_SEPARATOR") }.toSet()

    /** Adds or removes [packageName]; newly added favourites go to the end of the row. */
    fun toggle(order: List<String>, packageName: String): List<String> =
        if (packageName in order) order - packageName else order + packageName

    /**
     * Moves [packageName] by [delta] positions (negative = left), clamped to the list bounds.
     * Returns the list unchanged if the package is not present.
     */
    fun move(order: List<String>, packageName: String, delta: Int): List<String> {
        val from = order.indexOf(packageName)
        if (from < 0 || delta == 0) return order
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return order
        return order.toMutableList().apply {
            removeAt(from)
            add(to, packageName)
        }
    }

    /**
     * Orders [items] by their key's position in [order]. Items not in [order] are dropped.
     */
    fun <T> applyOrder(items: List<T>, order: List<String>, key: (T) -> String): List<T> {
        val byKey = items.associateBy(key)
        return order.mapNotNull { byKey[it] }
    }
}
