package de.rexlmanu.carousel;

import de.rexlmanu.carousel.utility.NoAI;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Accessors(fluent = true)
@Data
public class Carousel implements Runnable {

    private final Method[] methods = ((Supplier<Method[]>) () -> {
        try {
            Method getHandle = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity").getDeclaredMethod("getHandle");
            return new Method[]{
                    getHandle, getHandle.getReturnType().getDeclaredMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class)
            };
        } catch (Exception ex) {
            return null;
        }
    }).get();

    private String name;
    private int amount, ticks;
    private double degreeIncrease, radius, maxHeight, stepHeight;
    private Location location;
    private List<Horse> horses;
    private Map<Horse, Double> horseHeights;
    private double currentDegree;
    private double singleDegree;
    private BukkitTask task;

    public Carousel(String name, int amount, int ticks, double degreeIncrease, double radius, double stepHeight, double maxHeight, Location location) {
        this.name = name;
        this.amount = amount;
        this.ticks = ticks;
        this.degreeIncrease = degreeIncrease;
        this.radius = radius;
        this.maxHeight = maxHeight;
        this.stepHeight = stepHeight;
        this.location = location;
        this.horses = new ArrayList<>();
        this.horseHeights = new HashMap<>();
        this.currentDegree = 0;
        this.singleDegree = 360d / this.amount;
    }

    public void destroy() {
        if (task != null) task.cancel();
        this.horses.forEach(Entity::remove);
        this.horses.clear();
        this.horseHeights.clear();
        this.currentDegree = 0;
        this.singleDegree = 360d / this.amount;
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(CarouselPlugin.plugin(), this, 10, this.ticks);
    }

    public void spawn() {
        this.destroy();

        for (int index = 1; index < (this.amount + 1); index++) {
            double singleRadian = index * Math.toRadians(this.singleDegree);
            double x = this.radius * Math.cos(singleRadian);
            double z = this.radius * Math.sin(index * singleRadian);
            Location location = this.location.clone().add(x, 0, z);
            location.setYaw((float) Math.toDegrees(singleRadian * index));
            location.setPitch(0);
            Horse entity = (Horse) location.getWorld().spawnEntity(location, EntityType.HORSE);
            try {
                methods[1].invoke(methods[0].invoke(entity), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            } catch (Exception ex) {
            }
            NoAI.setEntityAi(entity, false);
            entity.setTamed(true);
            entity.setAdult();
            entity.setVariant(Horse.Variant.HORSE);
            this.horses.add(entity);
            if (this.maxHeight == 0) {
                this.horseHeights.put(entity, 0d);
            } else {
                this.horseHeights.put(entity, index % 2 == 0 ? this.maxHeight / 2 : 0);
            }
        }
    }

    @Override
    public void run() {
        if (this.currentDegree > 360) this.currentDegree = 0;
        if (this.horseHeights.isEmpty()) return;
        if (this.horses.isEmpty()) return;

        for (int index = 0; index < this.amount; index++) {
            int step = index + 1;
            double degree = step * this.singleDegree + this.currentDegree;
            if (degree > 360) {
                degree -= 360;
            }
            double radian = Math.toRadians(degree);
            double x = radius * Math.cos(radian);
            double z = radius * Math.sin(radian);
            Location location = this.location.clone().add(x, 0, z);
            location.setYaw((float) degree);
            location.setPitch(0);
            Horse horse = this.horses.get(index);
            Double height = this.horseHeights.get(horse);
            if (height >= 1 * this.maxHeight) {
                if (height >= 2 * this.maxHeight) {
                    height = 0d;
                    location.add(0, 0, 0);
                } else {
                    height += this.stepHeight;
                    location.add(0, this.maxHeight - (height - 1 * this.maxHeight), 0);
                }
            } else if (height < 1 * this.maxHeight) {
                height += this.stepHeight;
                location.add(0, height, 0);
            }
            horse.teleport(location);
            horseHeights.put(horse, height);
            try {
                methods[1].invoke(methods[0].invoke(horse), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            } catch (Exception ex) {
            }
        }

        this.currentDegree += this.degreeIncrease;
    }
}
