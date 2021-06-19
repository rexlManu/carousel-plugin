package de.rexlmanu.carousel;

import de.rexlmanu.carousel.utility.NoAI;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@Data
public class Carousel implements Runnable, Listener {

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
    private List<UUID> horses;
    private Map<UUID, Double> horseHeights;
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

        Bukkit.getPluginManager().registerEvents(this, CarouselPlugin.plugin());
    }

    public void destroy() {
        if (task != null) task.cancel();
        this.reset();
    }

    public void reset() {
        this.location.getWorld()
                .getLivingEntities()
                .stream()
                .filter(livingEntity -> this.horses.contains(livingEntity.getUniqueId()))
                .forEach(Entity::remove);
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
        this.loadChunk();

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
            } catch (Exception ignored) {
            }
            NoAI.setEntityAi(entity, false);
            entity.setTamed(true);
            entity.setAdult();
            entity.setVariant(Horse.Variant.HORSE);
            this.horses.add(entity.getUniqueId());
            if (this.maxHeight == 0) {
                this.horseHeights.put(entity.getUniqueId(), 0d);
            } else {
                this.horseHeights.put(entity.getUniqueId(), index % 2 == 0 ? this.maxHeight / 2 : 0);
            }
        }
    }

    @Override
    public void run() {
        if (this.currentDegree > 360) this.currentDegree = 0;
        List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(player -> player.getLocation().getWorld().equals(this.location.getWorld()))
                .filter(player -> player.getLocation().distance(this.location) < 25)
                .collect(Collectors.toList());
        if (players.isEmpty() && !this.horses.isEmpty()) {
            this.reset();
            return;
        }
        if (!players.isEmpty() && this.horses.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(CarouselPlugin.plugin(), () -> {
                this.spawn();
                this.start();
            }, 20);
            return;
        }

        if (this.horseHeights.isEmpty()) return;
        if (this.horses.isEmpty()) return;

        this.loadChunk();
        List<LivingEntity> livingEntities = location.getWorld().getLivingEntities();

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
            UUID uuid = this.horses.get(index);
            Optional<LivingEntity> any = livingEntities.stream().filter(livingEntity -> livingEntity.getUniqueId().equals(uuid)).findAny();
            Entity entity = any.orElse(null);
            Double height = this.horseHeights.get(uuid);
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
            if (entity != null) {
                entity.teleport(location);
                if (entity.getPassenger() != null)
                    try {
                        methods[1].invoke(methods[0].invoke(entity), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                    } catch (Exception ignored) {
                    }
            }
            horseHeights.put(uuid, height);
        }

        this.currentDegree += this.degreeIncrease;
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Location playerLocation = event.getPlayer().getLocation();
        if (!playerLocation.getWorld().equals(this.location.getWorld())) return;
        if (playerLocation.distance(this.location) > 25) return;

        List<? extends Player> nearPlayers = Bukkit.getOnlinePlayers().stream().filter(player -> player.getLocation().getWorld().equals(this.location.getWorld()))
                .filter(player -> player.getLocation().distance(this.location) < 25)
                .collect(Collectors.toList());
        if (nearPlayers.size() == 1) {
            this.reset();
            return;
        }
    }

    private void loadChunk() {
        // Loading chunk if its not loaded
        if (!this.location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            Bukkit.getScheduler().runTask(CarouselPlugin.plugin(), () -> this.location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4));
        }
    }
}
