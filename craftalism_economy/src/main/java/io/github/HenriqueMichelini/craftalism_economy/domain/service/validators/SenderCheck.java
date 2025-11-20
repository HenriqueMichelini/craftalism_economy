package io.github.HenriqueMichelini.craftalism_economy.domain.service.validators;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SenderCheck {
    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
}