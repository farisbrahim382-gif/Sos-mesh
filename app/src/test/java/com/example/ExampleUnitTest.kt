package com.example

import com.example.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun screenBottomNavItems_areAllNonNull() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull("Screen item should not be null", screen)
            assertNotNull("Screen route should not be null", screen.route)
            assertNotNull("Screen icon should not be null", screen.icon)
        }
    }
}
