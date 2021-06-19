package de.rexlmanu.carousel;

import de.rexlmanu.carousel.command.CarouselCommand;
import de.rexlmanu.carousel.configuration.CarouselFactory;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
public class CarouselPlugin extends JavaPlugin {
    @Getter
    private static CarouselPlugin plugin;

    private File file;
    private FileConfiguration configuration;

    private List<Carousel> carousels;

    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        CarouselPlugin.plugin = this;
        this.file = new File(this.getDataFolder(), "saves.yml");
        this.configuration = YamlConfiguration.loadConfiguration(file);

        this.carousels = new ArrayList<>();

        if (!file.exists()) {
            this.save();
        }

        this.configuration.getKeys(false).stream()
                .map(s -> CarouselFactory.read(s, this.configuration))
                .forEach(this.carousels::add);

        getLogger().info("Loaded " + this.carousels.size() + " carousels.");
        this.registerCommand();

        this.carousels.stream()
                .filter(carousel -> carousel.location().getWorld().isChunkLoaded(carousel.location().getBlockX() >> 4, carousel.location().getBlockZ() >> 4))
                .peek(Carousel::spawn)
                .forEach(Carousel::start);
    }

    @Override
    public void onDisable() {
        this.carousels.forEach(Carousel::destroy);
    }

    private void registerCommand() {
        CarouselCommand command = new CarouselCommand();
        PluginCommand pluginCommand = this.getCommand("carousel");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    public void save() {
        try {
            this.carousels.forEach(carousel -> CarouselFactory.write(carousel, this.configuration));
            this.configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
