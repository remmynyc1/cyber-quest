package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private static final String EMAIL = "maria.lopez@cyberquest.internal";

    @Test void rejectsShort() { assertNotNull(AccountController.policyProblem("Short1", "old", EMAIL)); }
    @Test void rejectsNoDigit() { assertNotNull(AccountController.policyProblem("OnlyLettersHereNow", "old", EMAIL)); }
    @Test void rejectsNoLetter() { assertNotNull(AccountController.policyProblem("123456789012345", "old", EMAIL)); }
    @Test void rejectsSameAsCurrent() { assertNotNull(AccountController.policyProblem("SamePassword123", "SamePassword123", EMAIL)); }
    @Test void rejectsEmailName() { assertNotNull(AccountController.policyProblem("maria.lopez-2026-x", "old", EMAIL)); }
    @Test void rejectsOver72Bytes() { assertNotNull(AccountController.policyProblem("a1".repeat(40), "old", EMAIL)); }
    @Test void acceptsGoodPassword() { assertNull(AccountController.policyProblem("Blue-Harbor-Seven-42", "old", EMAIL)); }
}
