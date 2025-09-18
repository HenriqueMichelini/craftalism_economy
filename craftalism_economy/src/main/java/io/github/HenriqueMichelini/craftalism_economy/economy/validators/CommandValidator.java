package io.github.HenriqueMichelini.craftalism_economy.economy.validators;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class CommandValidator {
    public boolean validateArguments(CommandSender sender, String[] args, int numOfArgs, String usageMessage) {
        if (args.length == numOfArgs) return true;
        sender.sendMessage(Component.text(usageMessage).color(ERROR_COLOR));
        return false;
    }
}
