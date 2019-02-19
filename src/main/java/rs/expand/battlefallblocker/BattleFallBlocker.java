package rs.expand.battlefallblocker;

// Remote imports.
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.RidePokemonEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*                                                              *\
       THE WHO-KNOWS-WHEN LIST OF POTENTIALLY AWESOME IDEAS
    TODO: Add new TODOs here. Cross off TODOs if they're done.
      NOTE: Stuff that's here will not necessarily get made.
\*                                                              */

// New things:
// TODO: Stuff.

// Improvements to existing things:
// TODO: More stuff.


// Note: printUnformattedMessage is a static import for a function from PrintingMethods, for convenience.
@Mod
(
        modid = "battlefallblocker",
        name = "Battle Fall Blocker",
        version = "0.1",
        dependencies = "required-after:pixelmon",
        acceptableRemoteVersions = "*"

        // Not listed but certainly appreciated:
        // salmjak (GitHub snippets for dealing with Forge events -- we've come full circle!)
)

public class BattleFallBlocker
{
    // Set up a logger for logging stuff. Yup.
    private static final Logger logger = LogManager.getLogger("battlefallblocker");

    // Set up a timer we can access from multiple places. Used so we can clear our flag if players fall into liquids.
    private final ScheduledExecutorService liquidCheckTimer = Executors.newSingleThreadScheduledExecutor();

    @Mod.EventHandler
    public void onPreInitEvent(final FMLPreInitializationEvent event)
    {
        logger.info("");
        logger.info("=================== B A T T L E  F A L L  B L O C K E R ===================");
        logger.info("--> §aPre-init completed. All systems nominal.");
        logger.info("===========================================================================");
        logger.info("");

        // Register our listeners with Forge and Pixelmon.
        Pixelmon.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    // FIXME: Custom mod liquids will allow people to stack "uses" of fall protection by dismounting into them. Fixable?
    @SubscribeEvent
    public void onRidePokemonEvent(final RidePokemonEvent event)
    {
        // Clear the falling flag if it exists.
        event.player.getEntityData().removeTag("fallingAfterDismount");

        // Execute a timed task that checks if our player is still riding a Pokémon.
        final ScheduledExecutorService rideCheckTimer = Executors.newSingleThreadScheduledExecutor();
        rideCheckTimer.scheduleWithFixedDelay(() ->
        {
            // Did the player stop merrily riding their Pokémon?
            if (!event.player.isRiding())
            {
                // Is the player dismounted but still up in the air?
                if (!event.player.onGround)
                {
                    // Is the player not currently in a liquid?
                    if (!event.player.isInWater() && !event.player.isInLava())
                    {
                        logger.warn("Stopped riding. We're airborne.");

                        // They're falling!! Quick, set the flag!
                        event.player.getEntityData().setBoolean("fallingAfterDismount", true);

                        if (event.player.onGround)
                            logger.warn("Player dismounted on the ground.");

                        if (event.player.isInWater())
                            logger.warn("Player dismounted in water.");

                        if (event.player.isInLava())
                            logger.warn("Player dismounted in lava.");

                        // Check for contact with liquids. Prevents them from giving players free fall protection.
                        // FIXME: Does not fire most of the time, you seem to dismount too fast?
                        liquidCheckTimer.scheduleWithFixedDelay(() ->
                        {
                            // Was the player caught dipping in some strange liquid? Don't attempt to save them.
                            if (event.player.isInWater() || event.player.isInLava())
                            {
                                logger.warn("Player was found in a liquid. Removing protection.");

                                // Clear the falling flag.
                                event.player.getEntityData().removeTag("fallingAfterDismount");

                                // Clean up after yourself.
                                liquidCheckTimer.shutdown();
                                liquidCheckTimer.shutdown();
                            }
                        }, 0, 200, TimeUnit.MILLISECONDS); // Faster check. Important.
                    }
                }

                // Cancel this task since our player is now dismounted.
                rideCheckTimer.shutdown();
            }
            else
                logger.warn("§aPlayer " + event.player.getName() + " is riding something.");
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onFallDamageEvent(final LivingFallEvent event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            logger.warn("§aFalling player! fallDistance is: " + event.getEntity().fallDistance);

            // Was our player previously on a Pokémon?
            if (event.getEntity().getEntityData().getBoolean("fallingAfterDismount"))
            {
                logger.warn("§aEntity has flag! Saving.");

                // Kill the flag.
                event.getEntity().getEntityData().removeTag("fallingAfterDismount");

                // Spring our net. This saves the player.
                event.setCanceled(true);

                if (liquidCheckTimer.isShutdown())
                    logger.error("The liquid check timer isn't running anymore!");

                // Shutdown the liquid check timer, too. Should still be running if we got here.
                liquidCheckTimer.shutdown();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoinEvent(final PlayerEvent.PlayerLoggedInEvent event)
    {
        // Make sure our joining player doesn't have the Pokémon-riding tag set anymore, if they had it.
        event.player.removeTag("fallingAfterDismount");
    }
}