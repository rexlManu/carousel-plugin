package de.rexlmanu.carousel.configuration;

import de.rexlmanu.carousel.Carousel;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

@Accessors(fluent = true)
public class CarouselFactory {

    public static Carousel read(String name, FileConfiguration fileConfiguration) {
        return new Carousel(
                name,
                fileConfiguration.getInt(name + ".amount"),
                fileConfiguration.getInt(name + ".ticks"),
                fileConfiguration.getDouble(name + ".degreeIncrease"),
                fileConfiguration.getDouble(name + ".radius"),
                fileConfiguration.getDouble(name + ".stepHeight"),
                fileConfiguration.getDouble(name + ".maxHeight"),
                (Location) fileConfiguration.get(name + ".location")
        );
    }

    public static void write(Carousel carousel, FileConfiguration fileConfiguration) {
        fileConfiguration.set(carousel.name() + ".amount", carousel.amount());
        fileConfiguration.set(carousel.name() + ".ticks", carousel.ticks());
        fileConfiguration.set(carousel.name() + ".degreeIncrease", carousel.degreeIncrease());
        fileConfiguration.set(carousel.name() + ".radius", carousel.radius());
        fileConfiguration.set(carousel.name() + ".maxHeight", carousel.maxHeight());
        fileConfiguration.set(carousel.name() + ".stepHeight", carousel.stepHeight());
        fileConfiguration.set(carousel.name() + ".location", carousel.location());
    }

}
