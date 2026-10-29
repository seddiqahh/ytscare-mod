package com.example.ytscare;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YtScareMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YtScareMod.MODID);
    public static final Map<MonsterType, RegistryObject<Item>> EGGS = new EnumMap<>(MonsterType.class);

    // ---- custom loot (dropped via data/ytscare/loot_tables/entities/<monster>.json)
    public static final RegistryObject<Item> FEAR_SHARD = ITEMS.register("fear_shard",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CURSED_TAPE = ITEMS.register("cursed_tape",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MONSTER_EYE = ITEMS.register("monster_eye",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ABYSS_HEART = ITEMS.register("abyss_heart",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true; // enchant glint
                }
            });

    static {
        for (MonsterType t : MonsterType.values()) {
            EGGS.put(t, ITEMS.register(t.id + "_spawn_egg", () ->
                    new ForgeSpawnEggItem(ModEntities.TYPES.get(t), t.eggPrimary, t.eggSecondary, new Item.Properties())));
        }
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ytscare"))
                .icon(() -> new ItemStack(EGGS.get(MonsterType.RED_SPIRAL).get()))
                .displayItems((params, out) -> {
                    EGGS.values().forEach(r -> out.accept(r.get()));
                    out.accept(FEAR_SHARD.get());
                    out.accept(CURSED_TAPE.get());
                    out.accept(MONSTER_EYE.get());
                    out.accept(ABYSS_HEART.get());
                })
                .build());
    }
}
