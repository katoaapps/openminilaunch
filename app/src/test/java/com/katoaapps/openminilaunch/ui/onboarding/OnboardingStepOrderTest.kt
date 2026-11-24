package com.katoaapps.openminilaunch.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepOrderTest {
    @Test
    fun slabChoosesThemeBeforeKeyboard() {
        assertEquals(
            listOf(
                OnboardingStep.LAYOUT,
                OnboardingStep.THEME,
                OnboardingStep.KEYBOARD,
                OnboardingStep.CONTACTS,
                OnboardingStep.ASSISTANT,
                OnboardingStep.HOME,
            ),
            onboardingStepOrder(showKeyboardSetup = true),
        )
    }

    @Test
    fun hardwareKeyboardPhoneStillChoosesThemeWithoutSoftwareKeyboardStep() {
        assertEquals(
            listOf(
                OnboardingStep.LAYOUT,
                OnboardingStep.THEME,
                OnboardingStep.CONTACTS,
                OnboardingStep.ASSISTANT,
                OnboardingStep.HOME,
            ),
            onboardingStepOrder(showKeyboardSetup = false),
        )
    }
}
