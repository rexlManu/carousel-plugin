package de.rexlmanu.carousel.command;

import de.rexlmanu.carousel.Carousel;
import de.rexlmanu.carousel.CarouselPlugin;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Accessors(fluent = true)
public class CarouselCommand implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        if (args.length <= 1) {
            sender.sendMessage(ChatColor.GRAY + "Usage:");
            sender.sendMessage(ChatColor.GRAY + "/carousel create <name>");
            sender.sendMessage(ChatColor.GRAY + "/carousel setlocation <name>");
            sender.sendMessage(ChatColor.GRAY + "/carousel setting <name> <amount=8, ticks=1, degreeIncrease=2, radius=4, stepHeight=0.05, maxHeight=1> <value>");
            sender.sendMessage(ChatColor.GRAY + "/carousel spawn <name>");
            sender.sendMessage(ChatColor.GRAY + "/carousel start <name>");
            sender.sendMessage(ChatColor.GRAY + "/carousel destroy <name>");
            sender.sendMessage(ChatColor.GRAY + "/carousel delete <name>");
            return true;
        }

        if (args.length == 2) {
            String name = args[1];
            Optional<Carousel> optionalCarousel = CarouselPlugin.plugin().carousels().stream().filter(carousel -> carousel.name().equals(name)).findAny();
            if (optionalCarousel.isEmpty() && !args[0].equals("create")) {
                sender.sendMessage(ChatColor.RED + "The carousel could not be found.");
                return true;
            }
            switch (args[0]) {
                case "create":
                    sender.sendMessage(ChatColor.GRAY + "You just created a carousel.");
                    CarouselPlugin.plugin().carousels().add(new Carousel(
                            name,
                            8,
                            1,
                            2,
                            4,
                            0.05,
                            1,
                            ((Player) sender).getLocation()
                    ));
                    CarouselPlugin.plugin().save();
                    break;
                case "setlocation":
                    optionalCarousel.get().location(((Player) sender).getLocation());
                    sender.sendMessage(ChatColor.GRAY + "You set the location for the carousel.");
                    break;
                case "spawn":
                    optionalCarousel.get().spawn();
                    sender.sendMessage(ChatColor.GRAY + "You spawn the carousel.");
                    break;
                case "start":
                    optionalCarousel.get().start();
                    sender.sendMessage(ChatColor.GRAY + "You start the carousel.");
                    break;
                case "destroy":
                    optionalCarousel.get().destroy();
                    sender.sendMessage(ChatColor.GRAY + "You destroyed the carousel.");
                    break;
                case "delete":
                    optionalCarousel.get().destroy();
                    CarouselPlugin.plugin().carousels().remove(optionalCarousel.get());
                    CarouselPlugin.plugin().save();
                    sender.sendMessage(ChatColor.GRAY + "You deleted the carousel.");
                    break;
            }
            return true;
        }
        if (args.length == 4) {
            if (!args[0].equals("setting")) return false;

            String name = args[1];
            Optional<Carousel> optionalCarousel = CarouselPlugin.plugin().carousels().stream().filter(carousel -> carousel.name().equals(name)).findAny();
            if (optionalCarousel.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "The carousel could not be found.");
                return true;
            }
            // amount=8, ticks=1, degreeIncrease=2, radius=4

            double number;
            try {
                number = Double.parseDouble(args[3]);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "That wasnt a number.");
                return true;
            }
            switch (args[2]) {
                case "amount":
                    optionalCarousel.get().amount((int) number);
                    break;
                case "ticks":
                    optionalCarousel.get().ticks((int) number);
                    break;
                case "degreeIncrease":
                    optionalCarousel.get().degreeIncrease((int) number);
                    break;
                case "stepHeight":
                    optionalCarousel.get().stepHeight((int) number);
                    break;
                case "maxHeight":
                    optionalCarousel.get().maxHeight((int) number);
                    break;
                case "radius":
                    optionalCarousel.get().radius((int) number);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Setting could not be found.");
                    return true;
            }
            CarouselPlugin.plugin().save();
            sender.sendMessage(ChatColor.RED + "Setting for carousel was modified.");
            optionalCarousel.get().spawn();
            optionalCarousel.get().start();
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
        }
        if (args.length == 1)
            return Stream.of("create", "setlocation", "spawn", "start", "destroy", "delete", "setting").filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        if (args.length == 2) {
            if (Arrays.asList("setlocation", "spawn", "start", "destroy", "delete", "setting").contains(args[0])) {
                return CarouselPlugin.plugin().carousels().stream().map(Carousel::name).filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equals("setting") && CarouselPlugin.plugin().carousels().stream().anyMatch(carousel -> carousel.name().equals(args[1]))) {
            return Stream.of("amount",
                    "ticks",
                    "degreeIncrease",
                    "stepHeight",
                    "maxHeight",
                    "radius").filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equals("setting")) {
            Optional<Carousel> any = CarouselPlugin.plugin().carousels().stream().filter(carousel -> carousel.name().equals(args[1])).findAny();
            if (any.isEmpty()) return new ArrayList<>();
            Optional<String> setting = Stream.of("amount",
                    "ticks",
                    "degreeIncrease",
                    "stepHeight",
                    "maxHeight",
                    "radius").filter(s -> args[2].equals(s)).findAny();
            if (setting.isEmpty()) return new ArrayList<>();
            switch (setting.get()) {
                case "amount":
                    return Collections.singletonList(String.valueOf(any.get().amount()));
                case "ticks":
                    return Collections.singletonList(String.valueOf(any.get().ticks()));
                case "degreeIncrease":
                    return Collections.singletonList(String.valueOf(any.get().degreeIncrease()));
                case "stepHeight":
                    return Collections.singletonList(String.valueOf(any.get().stepHeight()));
                case "maxHeight":
                    return Collections.singletonList(String.valueOf(any.get().maxHeight()));
                case "radius":
                    return Collections.singletonList(String.valueOf(any.get().radius()));
            }
        }

        return new ArrayList<>();
    }
}
