package dmr.tests;

import dmr.DMRTestConstants;
import dmr.DragonMounts.common.capability.DragonOwnerCapability;
import dmr.DragonMounts.common.handlers.DragonWhistleHandler;
import dmr.DragonMounts.common.handlers.DragonWhistleHandler.DragonInstance;
import dmr.DragonMounts.registry.DragonBreedsRegistry;
import dmr.DragonMounts.registry.ModBlocks;
import dmr.DragonMounts.registry.ModEntities;
import dmr.DragonMounts.registry.ModItems;
import dmr.DragonMounts.server.blockentities.DMREggBlockEntity;
import dmr.DragonMounts.server.entity.TameableDragonEntity;
import dmr.DragonMounts.server.items.DragonWhistleItem;
import dmr.DragonMounts.util.PlayerStateUtils;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;

@PrefixGameTestTemplate(false)
@ForEachTest(groups = "Dragon Dimensions")
public class DragonDimensionTests {
    private static ItemStack whistle(int index) {
        return ModItems.DRAGON_WHISTLES.values().stream()
                .filter(item -> ((DragonWhistleItem) item.get()).getColor().getId() == index)
                .map(item -> new ItemStack(item.get())).findFirst().orElseThrow();
    }

    @EmptyTemplate(floor = true)
    @GameTest
    @TestHolder
    public static void hatchHomeSurvivesPortalBeforeFirstBinding(ExtendedGameTestHelper helper) {
        var pos = helper.absolutePos(DMRTestConstants.TEST_POS);
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, ModBlocks.DRAGON_EGG_BLOCK.get().defaultBlockState());
        var egg = (DMREggBlockEntity) level.getBlockEntity(pos);
        egg.setBreed(DragonBreedsRegistry.getDefault());
        egg.hatch(level, pos);
        var baby = level.getEntitiesOfClass(TameableDragonEntity.class, new AABB(pos).inflate(1))
                .stream().findFirst().orElseThrow();
        var home = level.dimension().location().toString();
        helper.assertTrue(home.equals(baby.getHomeDimension()), "Hatching must persist home before any binding");
        var nether = level.getServer().getLevel(Level.NETHER);
        var moved = (TameableDragonEntity) baby.changeDimension(new DimensionTransition(
                nether, new Vec3(0, 100, 0), Vec3.ZERO, 0, 0, true, DimensionTransition.DO_NOTHING));
        helper.assertTrue(moved != null && home.equals(moved.getHomeDimension()), "Portal must preserve hatch home");
        var player = helper.makeTickingMockServerPlayerInLevel(GameType.DEFAULT_MODE);
        DragonWhistleHandler.setDragon(player, moved, 0);
        var cap = PlayerStateUtils.getHandler(player);
        helper.assertTrue(home.equals(cap.getDragonInstance(0).getHomeDimension()), "First binding must use hatch home");
        helper.assertTrue("minecraft:the_nether".equals(cap.getDragonInstance(0).getDimension()), "Location must track destination");
        moved.discard();
        helper.succeed();
    }

    @EmptyTemplate(floor = true)
    @GameTest
    @TestHolder
    public static void wrongDimensionDoesNotFallbackOrMutate(ExtendedGameTestHelper helper) {
        var player = helper.makeTickingMockServerPlayerInLevel(GameType.DEFAULT_MODE);
        var foreign = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS);
        foreign.setBreed(DragonBreedsRegistry.getDefault());
        foreign.initializeHomeDimension("aether:the_aether");
        var local = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS.offset(2, 0, 0));
        local.setBreed(DragonBreedsRegistry.getDefault());
        DragonWhistleHandler.setDragon(player, foreign, 0);
        DragonWhistleHandler.setDragon(player, local, 1);
        foreign.setOrderedToSit(true);
        local.setOrderedToSit(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, whistle(0));
        player.setItemInHand(InteractionHand.OFF_HAND, whistle(1));
        var cap = PlayerStateUtils.getHandler(player);
        var before = cap.serializeNBT(player.registryAccess()).copy();
        helper.assertTrue(DragonWhistleHandler.getDragonSummonIndex(player) == 0, "Selected whistle must win");
        helper.assertTrue(!DragonWhistleHandler.callDragon(player), "Foreign home must reject summon");
        helper.assertTrue(before.equals(cap.serializeNBT(player.registryAccess())), "Rejected call must preserve saved data");
        helper.assertTrue(foreign.isOrderedToSit() && local.isOrderedToSit(), "No other dragon may respond");
        helper.assertTrue(cap.lastCall == null, "Rejected call must not start cooldown");
        player.setItemInHand(InteractionHand.MAIN_HAND, whistle(1));
        helper.assertTrue(DragonWhistleHandler.callDragon(player), "Local dragon must remain summonable");
        helper.succeed();
    }

    @EmptyTemplate
    @GameTest
    @TestHolder
    public static void replacingWhistleDoesNotInheritOldHome(ExtendedGameTestHelper helper) {
        var player = helper.makeTickingMockServerPlayerInLevel(GameType.DEFAULT_MODE);
        var oldDragon = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS);
        oldDragon.setBreed(DragonBreedsRegistry.getDefault());
        oldDragon.initializeHomeDimension("minecraft:the_nether");
        DragonWhistleHandler.setDragon(player, oldDragon, 0);
        var replacement = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS);
        replacement.setBreed(DragonBreedsRegistry.getDefault());
        DragonWhistleHandler.setDragon(player, replacement, 0);
        var cap = PlayerStateUtils.getHandler(player);
        helper.assertTrue(player.level().dimension().location().toString().equals(cap.getDragonInstance(0).getHomeDimension()),
                "Replacement must not inherit previous dragon home");
        DragonWhistleHandler.setDragon(player, oldDragon, 1);
        helper.assertTrue("minecraft:the_nether".equals(cap.getDragonInstance(1).getHomeDimension()), "Rebinding must retain own home");
        helper.succeed();
    }

    @EmptyTemplate
    @GameTest
    @TestHolder
    public static void legacyHomeMigratesThroughSaveAndRespawn(ExtendedGameTestHelper helper) {
        var player = helper.makeTickingMockServerPlayerInLevel(GameType.DEFAULT_MODE);
        var dragon = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS);
        dragon.setBreed(DragonBreedsRegistry.getDefault());
        DragonWhistleHandler.setDragon(player, dragon, 0);
        var cap = PlayerStateUtils.getHandler(player);
        var saved = cap.serializeNBT(player.registryAccess());
        var record = saved.getCompound("dragonInstances").getCompound("0");
        record.remove("homeDimension");
        record.putString("dimension", "minecraft:the_nether");
        saved.getCompound("dragonNBT_0").remove("homeDimension");
        var migrated = new DragonOwnerCapability();
        migrated.deserializeNBT(player.registryAccess(), saved);
        var restored = migrated.createDragonEntity(player, helper.getLevel(), 0);
        helper.assertTrue(restored != null, "Legacy dragon must restore");
        helper.assertTrue(dragon.getDragonUUID().equals(restored.getDragonUUID()), "Logical dragon UUID must survive");
        helper.assertTrue("minecraft:the_nether".equals(restored.getHomeDimension()), "Legacy stored dimension must become home");
        var reloaded = ModEntities.DRAGON_ENTITY.get().create(helper.getLevel());
        reloaded.load(restored.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue("minecraft:the_nether".equals(reloaded.getHomeDimension()), "Entity save/reload must preserve home");
        helper.succeed();
    }
}
