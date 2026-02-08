package com.example.auraarchive.module

sealed class AppNavigationItems {
    object Home : AppNavigationItems()
    object About : AppNavigationItems()
    object Resource : AppNavigationItems()
    data class DraftReview(val postId: String) : AppNavigationItems()
    data class DocContent(val postId: String) : AppNavigationItems()
}