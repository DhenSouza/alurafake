package br.com.alura.AluraFake.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGenerationTest {

    @Test
    void generatePassword__password_should_be_six_digits() {
        String password = PasswordGeneration.generatePassword();
        assertEquals(6, password.length());
    }

    @Test
    void generatePassword__password_should_contain_only_digits() {
        String password = PasswordGeneration.generatePassword();
        assertTrue(password.matches("\\d{6}"));
    }

    @Test
    void generatePassword__generated_passwords_should_be_different_most_of_the_time() {
        String password1 = PasswordGeneration.generatePassword();
        String password2 = PasswordGeneration.generatePassword();
        assertNotEquals(password1, password2);
    }

}