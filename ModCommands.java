package com.example.ytscare;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.example.ytscare.entity.YtMonster;
import com.example.ytscare.network.JumpscarePacket;
import com.example.ytscare.network.Net;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 *  /ytscare spawn <monster>        spawn a monster 6 blocks in front of you
 *  /ytscare jumpscare <monster>    play the fullscreen jumpscare on yourself
 *  /ytscare ambient [0-7]          trigger a random (or specific) ambient scare on yourself
 */
@Mod.EventBusSubscriber(modid = YtScareMod.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("ytscare")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("monster", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(MonsterType.ids(), b))
                                .executes(ModCommands::spawn)))
                .then(Commands.literal("jumpscare")
                        .then(Commands.argument("monster", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(MonsterType.ids(), b))
                                .executes(ModCommands::jumpscare)))
                .then(Commands.literal("ambient")
                        .executes(c -> ambient(c, -1))
                        .then(Commands.argument("kind", IntegerArgumentType.integer(0, 7))
                                .executes(c -> ambient(c, IntegerArgumentType.getInteger(c, "kind"))))));
    }

    private static int spawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        MonsterType t = MonsterType.byId(StringArgumentType.getString(c, "monster"));
        ServerLevel level = p.serverLevel();
        YtMonster m = ModEntities.TYPES.get(t).get().create(level);
        if (m == null) return 0;
        Vec3 pos = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
        m.moveTo(pos.x, p.getY(), pos.z, p.getYRot() + 180, 0);
        level.addFreshEntity(m);
        c.getSource().sendSuccess(() -> Component.literal("Spawned " + t.displayName), true);
        return 1;
    }

    private static int jumpscare(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        MonsterType t = MonsterType.byId(StringArgumentType.getString(c, "monster"));
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new JumpscarePacket(t.ordinal(), 40));
        return 1;
    }

    private static int ambient(CommandContext<CommandSourceStack> c, int kind) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        AmbientScares.trigger(p, kind >= 0 ? kind : p.getRandom().nextInt(8));
        return 1;
    }
}
