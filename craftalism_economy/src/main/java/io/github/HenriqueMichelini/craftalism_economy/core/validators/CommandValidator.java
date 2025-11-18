package io.github.HenriqueMichelini.craftalism_economy.core.validators;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class CommandValidator {
    private static final NamedTextColor ERROR_COLOR = NamedTextColor.RED;

    public boolean validateArguments(CommandSender sender, String[] args, int numOfArgs, String usageMessage) {
        if (args.length == numOfArgs) return true;
        sender.sendMessage(Component.text(usageMessage).color(ERROR_COLOR));
        return false;
    }
}
