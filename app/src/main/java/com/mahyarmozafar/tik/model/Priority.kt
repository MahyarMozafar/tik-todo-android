package com.mahyarmozafar.tik.model

/** How important a task is. Saved as a number so tasks can be sorted by it. */
enum class Priority(val raw: Int) {
    None(0),
    Low(1),
    Medium(2),
    High(3);

    /** "!", "!!" or "!!!", the way the priority shows next to a task. */
    val marks: String get() = "!".repeat(raw)

    companion object {
        fun fromRaw(raw: Int): Priority = entries.firstOrNull { it.raw == raw } ?: None
    }
}
