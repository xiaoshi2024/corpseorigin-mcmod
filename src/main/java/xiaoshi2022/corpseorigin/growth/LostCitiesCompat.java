package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.event.EvolutionEventHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional Lost Cities API bridge. No Lost Cities classes are loaded without the mod. */
public final class LostCitiesCompat {
    private static final String EVENT = "exploration:lostcities_city";
    private static boolean checked;
    private static Object api;
    private static Method getLostInfo;
    private static Method getChunkInfo;
    private static Method isCity;

    private LostCitiesCompat() {}

    private static boolean available() {
        if (!checked) {
            checked = true;
            if (FabricLoader.getInstance().isModLoaded("lostcities")) {
                try {
                    Class<?> cities = Class.forName("mcjty.lostcities.LostCities");
                    Field instance = cities.getField("lostCitiesImp");
                    api = instance.get(null);
                    getLostInfo = api.getClass().getMethod("getLostInfo", Level.class);
                    Class<?> info = Class.forName("mcjty.lostcities.api.ILostCityInformation");
                    getChunkInfo = info.getMethod("getChunkInfo", int.class, int.class);
                    Class<?> chunk = Class.forName("mcjty.lostcities.api.ILostChunkInfo");
                    isCity = chunk.getMethod("isCity");
                } catch (ReflectiveOperationException | LinkageError error) {
                    api = null;
                    CorpseOrigin.LOGGER.warn("Lost Cities exploration compatibility is unavailable", error);
                }
            }
        }
        return api != null;
    }

    /** Null means Lost Cities is absent or this dimension has no city profile. */
    public static Boolean isCity(Level level, BlockPos pos) {
        if (!CorpseConfig.get().ruinLoot.lostCitiesEnabled || !available()) return null;
        try {
            Object info = getLostInfo.invoke(api, level);
            if (info == null) return null;
            Object chunk = getChunkInfo.invoke(info, pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? false : Boolean.TRUE.equals(isCity.invoke(chunk));
        } catch (ReflectiveOperationException | LinkageError error) {
            api = null;
            CorpseOrigin.LOGGER.warn("Lost Cities city lookup failed; disabling compatibility", error);
            return null;
        }
    }

    public static void explore(ServerPlayer player) {
        var cfg = CorpseConfig.get().ruinLoot;
        if (!cfg.lostCitiesEnabled || player.getAttachedOrCreate(SurvivalGrowth.JOURNAL).getBooleanOr(EVENT, false)
                || !Boolean.TRUE.equals(isCity(player.level(), player.blockPosition()))) return;
        var journal = player.getAttachedOrCreate(SurvivalGrowth.JOURNAL).copy();
        journal.putBoolean(EVENT, true);
        player.setAttached(SurvivalGrowth.JOURNAL, journal);
        FreeGrowth.opportunity(player, EVENT);
        int points = EvolutionEventHandler.awardPoints(player, cfg.lostCitiesDiscoveryPoints);
        player.sendSystemMessage(Component.translatable("growth.corpseorigin.lostcities_discovered", points));
    }
}
