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
        // Hiroku (helping with questions and setting up UTF-8 encoding, which made § work)
        // salmjak (GitHub snippets for dealing with Forge events -- we've come full circle!)
        // Xenoyia (co-owning the server this started on, and basic task code)
)

public class BattleFallBlocker
{
    // Set up a logger for logging stuff. Yup.
    private static final Logger logger = LogManager.getLogger("battlefallblocker");

    @Mod.EventHandler
    public void onPreInitEvent(final FMLPreInitializationEvent event)
    {
        logger.info("");
        logger.info("=================== B A T T L E  F A L L  B L O C K E R ===================");
        logger.info("--> §aPre-init completed. All systems nominal.");
        logger.info("===========================================================================");
        logger.info("");

        Pixelmon.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRidePokemonEvent(final RidePokemonEvent event)
    {
        // Clear the falling flag if it exists.
        event.player.getEntityData().removeTag("fallingAfterDismount");

        // Execute a timed task that checks if our player is still riding a Pokémon.
        final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleWithFixedDelay(() ->
        {
            // Did the player stop merrily riding their Pokémon?
            if (!event.player.isRiding())
            {
                // Is the player dismounted but still up in the air? They're falling! Quick, set the flag!
                if (!event.player.onGround && !event.player.isInWater())
                {
                    logger.warn("--> §cStopped riding. We're airborne.");
                    event.player.getEntityData().setBoolean("fallingAfterDismount", true);
                }

                if (event.player.onGround)
                    logger.error("Player is on the ground.");

                if (event.player.isInWater())
                    logger.error("Player is in water.");

                // Cancel the task since our player is now dismounted.
                timer.shutdown();
            }
            else
                logger.warn("§aPlayer " + event.player.getName() + " is riding something.");
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    // FIXME: Dismounting into water preserves the flag. You can abuse that to get free immunity for your next fall.

    @SubscribeEvent
    public void onFallDamageEvent(final LivingFallEvent event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            logger.warn("§aFalling player! Entity is " + event.getEntity() + ", named " +
                    event.getEntity().getName() + ", fallDistance " + event.getEntity().fallDistance);

            // Was our player previously on a Pokémon?
            if (event.getEntity().getEntityData().getBoolean("fallingAfterDismount"))
            {
                logger.warn("§a1. Entity has flag! Currently: " +
                        event.getEntity().getEntityData().getBoolean("fallingAfterDismount"));

                // Kill the flag.
                event.getEntity().getEntityData().removeTag("fallingAfterDismount");

                // Spring our net. This saves the player.
                event.setCanceled(true);
            }
        }
    }

    public void onHitWaterEvent(final Water)

    @SubscribeEvent
    public void onPlayerJoinEvent(final PlayerEvent.PlayerLoggedInEvent event)
    {
        // Make sure our joining player doesn't have the Pokémon-riding tag set anymore, if they had it.
        event.player.removeTag("fallingAfterDismount");
    }

/*    private static boolean validateOptionalKeys(Object... keys)
    {
        for (Object key : keys)
        {
            if (key == null)
                return false;
        }

        return true;
    }*/
}