package dmr.tests;

import dmr.DMRTestConstants;
import dmr.DragonMounts.config.ServerConfig;
import dmr.DragonMounts.registry.DragonBreedsRegistry;
import dmr.DragonMounts.registry.ModEntities;
import dmr.DragonMounts.server.entity.DragonConstants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;

@PrefixGameTestTemplate(false)
@ForEachTest(groups = "Dragons")
public class DragonMovementTests {
    @EmptyTemplate(floor = true)
    @GameTest
    @TestHolder
    public static void riderFlyingSpeedDoesNotSlowAutonomousFlight(ExtendedGameTestHelper helper) {
        var player = helper.makeTickingMockServerPlayerInLevel(GameType.DEFAULT_MODE);
        player.moveToCentre();
        var dragon = helper.spawn(ModEntities.DRAGON_ENTITY.get(), DMRTestConstants.TEST_POS);
        dragon.setBreed(DragonBreedsRegistry.getDefault());
        dragon.tamedFor(player, true);

        double originalModifier = ServerConfig.BASE_FLYING_SPEED;
        try {
            ServerConfig.BASE_FLYING_SPEED = 0.3;
            dragon.tick();
            dragon.setSprinting(false);

            double autonomousSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            helper.assertTrue(
                    Math.abs(autonomousSpeed - DragonConstants.BASE_SPEED_FLYING) < 0.0001,
                    "Rider setting must not change the AI flying speed attribute");
            helper.assertTrue(
                    Math.abs(dragon.getFlyingSpeed() - autonomousSpeed) < 0.0001,
                    "Unridden dragon must fly at its normal speed");

            dragon.setRidingPlayer(player);
            helper.assertTrue(dragon.getControllingPassenger() == player, "Owner must control the ridden dragon");
            helper.assertTrue(
                    Math.abs(dragon.getFlyingSpeed() - autonomousSpeed * 0.3) < 0.0001,
                    "Ridden dragon must use the configured flying speed multiplier");

            player.stopRiding();
            helper.assertTrue(
                    Math.abs(dragon.getFlyingSpeed() - autonomousSpeed) < 0.0001,
                    "Dragon must regain normal flying speed after dismounting");
        } finally {
            ServerConfig.BASE_FLYING_SPEED = originalModifier;
        }
        helper.succeed();
    }
}
