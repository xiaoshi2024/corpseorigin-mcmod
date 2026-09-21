package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

/** Refresh legacy chunks that were originally generated without a sky-light engine. */
public final class CorpseNestLighting {
    private CorpseNestLighting() {}

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (generated || !level.dimension().equals(CorpseNestDimension.KEY)) return;
            chunk.initializeLightSources();
            var engine = level.getChunkSource().getLightEngine();
            engine.setLightEnabled(chunk.getPos(), true);
            engine.propagateLightSources(chunk.getPos());
        });
    }
}
