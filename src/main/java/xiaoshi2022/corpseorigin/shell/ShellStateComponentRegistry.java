package xiaoshi2022.corpseorigin.shell;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ShellStateComponentRegistry {

    static ShellStateComponentRegistry getInstance() {
        return Impl.INSTANCE;
    }

    void register(ShellStateComponentFactory factory);
    Collection<ShellStateComponentFactory> getValues();

    default void register(Supplier<ShellStateComponent> emptyFactory,
                          Function<ServerPlayer, ShellStateComponent> playerFactory) {
        register(new ShellStateComponentFactory() {
            @Override public ShellStateComponent empty() { return emptyFactory.get(); }
            @Override public ShellStateComponent of(ServerPlayer player) { return playerFactory.apply(player); }
        });
    }

    interface ShellStateComponentFactory {
        ShellStateComponent empty();
        ShellStateComponent of(ServerPlayer player);
    }

    final class Impl implements ShellStateComponentRegistry {
        static final Impl INSTANCE = new Impl();
        private final Set<ShellStateComponentFactory> factories = new LinkedHashSet<>();

        @Override
        public void register(ShellStateComponentFactory factory) {
            this.factories.add(factory);
        }

        @Override
        public Collection<ShellStateComponentFactory> getValues() {
            return Collections.unmodifiableSet(this.factories);
        }
    }
}