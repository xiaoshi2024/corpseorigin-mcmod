package xiaoshi2022.corpseorigin.shell;


import org.jetbrains.annotations.Nullable;

public interface ClientShell {

    @Nullable
    String beginSync(ShellState state);

    void endSync(ShellState storedState);
}