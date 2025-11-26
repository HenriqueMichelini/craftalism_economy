package io.github.HenriqueMichelini.craftalism_economy.presentation.validation;

public class PlayerNameCheck {
    public boolean isValid(String name) {
        return name.length() >= 3 && name.length() <= 16 && name.matches("[A-Za-z0-9_]+");
    }
}