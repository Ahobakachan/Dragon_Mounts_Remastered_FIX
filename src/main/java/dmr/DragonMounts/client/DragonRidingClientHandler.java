package dmr.DragonMounts.client;

import dmr.DragonMounts.DMR;
import dmr.DragonMounts.server.entity.TameableDragonEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only handling for DMR dragon riding.
 *
 * Keeps the camera in third-person while riding a DMR dragon and clears
 * stale sneak state after dismounting.
 */
@EventBusSubscriber(modid = DMR.MOD_ID, value = Dist.CLIENT, bus = Bus.GAME)
public final class DragonRidingClientHandler {

    private static CameraType previousCameraType;
    private static boolean forcedThirdPerson;
    private static boolean wasRidingDragon;

    private DragonRidingClientHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) {
            restoreCamera(minecraft);
            wasRidingDragon = false;
            return;
        }

        boolean ridingDragon = player.getVehicle() instanceof TameableDragonEntity;

        if (ridingDragon) {
            wasRidingDragon = true;

            if (!forcedThirdPerson) {
                previousCameraType = minecraft.options.getCameraType();
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                forcedThirdPerson = true;
            }
            return;
        }

        if (wasRidingDragon) {
            wasRidingDragon = false;

            // The server also clears the synced flag, but clear it locally as well
            // so a stale client-side sneak state cannot block right-click interaction.
            player.setShiftKeyDown(false);
            if (player.getPose() == Pose.CROUCHING) {
                player.setPose(Pose.STANDING);
            }

            restoreCamera(minecraft);
        }
    }

    private static void restoreCamera(Minecraft minecraft) {
        if (!forcedThirdPerson) {
            previousCameraType = null;
            return;
        }

        minecraft.options.setCameraType(
                previousCameraType != null ? previousCameraType : CameraType.FIRST_PERSON);
        previousCameraType = null;
        forcedThirdPerson = false;
    }
}
