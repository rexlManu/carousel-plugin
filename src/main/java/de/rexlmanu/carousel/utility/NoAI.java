package de.rexlmanu.carousel.utility;

import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;

public class NoAI {

    private static Class<?> NBT_TAG_COMPOUND_CLASS;

    static {
        try {
            NBT_TAG_COMPOUND_CLASS = Reflection.nmsClass("NBTTagCompound");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void setEntityAi(Entity entity, boolean ai) {
        try {
            Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            Object tag = nmsEntity.getClass().getMethod("getNBTTag").invoke(nmsEntity);
            if (tag == null) {
                tag = NBT_TAG_COMPOUND_CLASS.newInstance();
            }

            nmsEntity.getClass().getMethod("c", NBT_TAG_COMPOUND_CLASS).invoke(nmsEntity, tag);
            NBT_TAG_COMPOUND_CLASS.getMethod("setInt", String.class, int.class).invoke(tag, "NoAI", ai ? 0 : 1);
            NBT_TAG_COMPOUND_CLASS.getMethod("setBoolean", String.class, boolean.class).invoke(tag, "Silent", !ai);
            nmsEntity.getClass().getMethod("f", NBT_TAG_COMPOUND_CLASS).invoke(nmsEntity, tag);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

}
