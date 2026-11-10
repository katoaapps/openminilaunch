package com.katoaapps.openminilaunch.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepOrderTest {
    @Test
    fun slabChoosesThemeBeforeKeyboard() {
        assertEquals(
            listOf(
                OnboardingStep.HOME,
                OnboardingStep.THEME,
                OnboardingStep.KEYBOARD,
                OnboardingStep.CONTACTS,
                OnboardingStep.MESSAGING,
                OnboardingStep.ASSISTANT,
            ),
            onboardingStepOrder(showKeyboardSetup = true),
        )
    }

    @Test
    fun hardwareKeyboardPhoneStillChoosesThemeWithoutSoftwareKeyboardStep() {
        assertEquals(
            listOf(
                OnboardingStep.HOME,
                OnboardingStep.THEME,
                OnboardingStep.CONTACTS,
                OnboardingStep.MESSAGING,
                OnboardingStep.ASSISTANT,
            ),
            onboardingStepOrder(showKeyboardSetup = false),
        )
    }
}
