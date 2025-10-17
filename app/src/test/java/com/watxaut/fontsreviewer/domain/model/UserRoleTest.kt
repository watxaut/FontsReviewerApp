package com.watxaut.fontsreviewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun `fromString returns ADMIN for admin string lowercase`() {
        val role = UserRole.fromString("admin")
        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun `fromString returns ADMIN for admin string uppercase`() {
        val role = UserRole.fromString("ADMIN")
        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun `fromString returns ADMIN for admin string mixed case`() {
        val role = UserRole.fromString("AdMiN")
        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun `fromString returns OPERATOR for operator string lowercase`() {
        val role = UserRole.fromString("operator")
        assertEquals(UserRole.OPERATOR, role)
    }

    @Test
    fun `fromString returns OPERATOR for operator string uppercase`() {
        val role = UserRole.fromString("OPERATOR")
        assertEquals(UserRole.OPERATOR, role)
    }

    @Test
    fun `fromString returns OPERATOR for operator string mixed case`() {
        val role = UserRole.fromString("OpErAtOr")
        assertEquals(UserRole.OPERATOR, role)
    }

    @Test
    fun `fromString returns OPERATOR for unknown string`() {
        val role = UserRole.fromString("unknown")
        assertEquals(UserRole.OPERATOR, role)
    }

    @Test
    fun `fromString returns OPERATOR for empty string`() {
        val role = UserRole.fromString("")
        assertEquals(UserRole.OPERATOR, role)
    }

    @Test
    fun `fromString returns OPERATOR as default for invalid input`() {
        val role = UserRole.fromString("invalid_role_123")
        assertEquals(UserRole.OPERATOR, role)
    }
}
