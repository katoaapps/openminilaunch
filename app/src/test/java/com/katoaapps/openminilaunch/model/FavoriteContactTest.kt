package com.katoaapps.openminilaunch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FavoriteContactTest {
    @Test
    fun noPhonesReturnsNone() {
        assertSame(AndroidPhoneSelection.None, favorite(emptyList()).phoneSelection())
    }

    @Test
    fun onlyPhoneResolvesDirectly() {
        val mobile = phone("111", "Mobile")

        assertEquals(AndroidPhoneSelection.Resolved(mobile), favorite(listOf(mobile)).phoneSelection())
    }

    @Test
    fun multiplePhonesRequireChoiceEvenWhenOneIsSuperPrimary() {
        val work = phone("111", "Work", primary = true)
        val mobile = phone("222", "Mobile", superPrimary = true)

        assertEquals(
            AndroidPhoneSelection.Choose(listOf(work, mobile)),
            favorite(listOf(work, mobile)).phoneSelection(),
        )
    }

    @Test
    fun multiplePhonesRequireChoiceEvenWhenOneIsPrimary() {
        val home = phone("111", "Home")
        val mobile = phone("222", "Mobile", primary = true)

        assertEquals(
            AndroidPhoneSelection.Choose(listOf(home, mobile)),
            favorite(listOf(home, mobile)).phoneSelection(),
        )
    }

    @Test
    fun multipleUnrankedPhonesRequireChoice() {
        val home = phone("111", "Home")
        val mobile = phone("222", "Mobile")

        assertEquals(
            AndroidPhoneSelection.Choose(listOf(home, mobile)),
            favorite(listOf(home, mobile)).phoneSelection(),
        )
    }

    @Test
    fun explicitPreferredPhoneResolvesMultipleNumbers() {
        val home = phone("111", "Home")
        val mobile = phone("222", "Mobile")
        val contact = favorite(listOf(home, mobile))
        val fingerprint = favoritePhoneFingerprint(contact.id, mobile.normalizedNumber)

        assertEquals(
            AndroidPhoneSelection.Resolved(mobile),
            contact.phoneSelection(contact.preferredPhone(contact.id, fingerprint)),
        )
    }

    @Test
    fun missingPreferredPhoneFallsBackToChoice() {
        val home = phone("111", "Home")
        val mobile = phone("222", "Mobile")
        val contact = favorite(listOf(home, mobile))
        val removedPhone = favoritePhoneFingerprint(contact.id, "333")

        assertEquals(
            AndroidPhoneSelection.Choose(listOf(home, mobile)),
            contact.phoneSelection(contact.preferredPhone(contact.id, removedPhone)),
        )
    }

    @Test
    fun duplicateNormalizedNumbersDoNotForceChoice() {
        val formatted = phone("+1 (555) 123-4567", "Mobile", normalized = "+15551234567")
        val plain = phone("+15551234567", "Other", normalized = "+15551234567")

        assertEquals(
            AndroidPhoneSelection.Resolved(formatted),
            favorite(listOf(formatted, plain)).phoneSelection(),
        )
    }

    @Test
    fun preferredDuplicateRepresentsItsNumberWithoutHidingOtherNumbers() {
        val unranked = phone("+1 (555) 123-4567", "Other", normalized = "+15551234567")
        val preferred = phone(
            "+15551234567",
            "Mobile",
            normalized = "+15551234567",
            superPrimary = true,
        )
        val work = phone("5557654321", "Work")

        assertEquals(
            AndroidPhoneSelection.Choose(listOf(preferred, work)),
            favorite(listOf(unranked, preferred, work)).phoneSelection(),
        )
    }

    private fun favorite(phones: List<AndroidContactPhone>) = FavoriteAndroidContact(
        id = FavoriteContactId.Android("may"),
        contactUri = "content://contacts/may",
        displayName = "May Wong",
        photoUri = null,
        organization = null,
        phones = phones,
    )

    private fun phone(
        number: String,
        label: String,
        normalized: String = number,
        primary: Boolean = false,
        superPrimary: Boolean = false,
    ) = AndroidContactPhone(
        rawNumber = number,
        normalizedNumber = normalized,
        label = label,
        isPrimary = primary,
        isSuperPrimary = superPrimary,
    )
}
