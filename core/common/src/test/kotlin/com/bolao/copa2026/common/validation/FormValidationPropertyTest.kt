// Feature: bolao-copa-2026, Property 8: Form field validation
package com.bolao.copa2026.common.validation

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class FormValidationPropertyTest : FreeSpec({

    "Property 8 — display name validation" - {
        "rejects names shorter than 3 characters" {
            checkAll(1000, Arb.string(0..2)) { name ->
                validateDisplayName(name).isValid shouldBe false
            }
        }

        "rejects names longer than 50 characters" {
            checkAll(1000, Arb.string(51..200)) { name ->
                validateDisplayName(name).isValid shouldBe false
            }
        }

        "accepts names in [3, 50] characters" {
            checkAll(1000, Arb.string(3..50)) { name ->
                validateDisplayName(name).isValid shouldBe true
            }
        }
    }

    "Property 8 — password validation" - {
        "rejects passwords shorter than 8 characters" {
            checkAll(1000, Arb.string(0..7)) { pwd ->
                validatePassword(pwd).isValid shouldBe false
            }
        }

        "rejects passwords longer than 128 characters" {
            checkAll(1000, Arb.string(129..300)) { pwd ->
                validatePassword(pwd).isValid shouldBe false
            }
        }

        "accepts passwords in [8, 128] characters" {
            checkAll(1000, Arb.string(8..128)) { pwd ->
                validatePassword(pwd).isValid shouldBe true
            }
        }
    }

    "Property 8 — email validation" - {
        "rejects emails longer than 254 characters" {
            checkAll(1000, Arb.string(250..260)) { s ->
                val longEmail = "a@${s}.com"
                if (longEmail.length > 254) {
                    validateEmail(longEmail).isValid shouldBe false
                }
            }
        }

        "accepts well-formed emails" {
            val validEmails = listOf(
                "user@example.com",
                "user.name+tag@domain.org",
                "test123@sub.domain.com",
                "a@b.co"
            )
            validEmails.forEach { email ->
                validateEmail(email).isValid shouldBe true
            }
        }

        "rejects malformed emails" {
            val invalidEmails = listOf(
                "notanemail",
                "@missing-local.org",
                "missing-domain@",
                "two@@signs.com",
                ""
            )
            invalidEmails.forEach { email ->
                validateEmail(email).isValid shouldBe false
            }
        }
    }
})
