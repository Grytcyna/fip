package com.grytsyna.fixitpro.enum_status

enum class RangeFilter(val description: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    WEEK("This Week"),
    MONTH("This Month"),
    MANUALLY("Manually");

    companion object {
        fun fromDescription(description: String): RangeFilter {
            return RangeFilter.entries.find {
                it.description.equals(
                    description,
                    ignoreCase = true
                )
            } ?: MANUALLY
        }

        fun fromIndex(index: Int): RangeFilter {
            return RangeFilter.entries.getOrNull(index) ?: MANUALLY
        }
    }
}
