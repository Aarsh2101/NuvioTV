package com.nuvio.tv.domain.model

enum class HomeLayout(val displayName: String) {
    CLASSIC("Classic View"),
    GRID("Grid View"),
    MODERN("Modern View"),
    CINEMA("Cinema View")
}

val HomeLayout.isModernFamily: Boolean
    get() = this == HomeLayout.MODERN || this == HomeLayout.CINEMA
