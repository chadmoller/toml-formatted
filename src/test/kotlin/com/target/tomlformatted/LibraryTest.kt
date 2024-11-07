package com.target.tomlformatted

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryTest {
    @Test
    fun someLibraryMethodReturnsTrue() {
        val classUnderTest = Library()
        classUnderTest.someLibraryMethod() shouldBe true
    }
}
