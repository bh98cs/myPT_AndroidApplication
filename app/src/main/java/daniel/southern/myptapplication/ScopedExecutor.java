package daniel.southern.myptapplication;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executor class required for pose detection
 */
public class ScopedExecutor implements Executor {
    private final Executor executor;
    private final AtomicBoolean shutdown = new AtomicBoolean();

    public ScopedExecutor(@NonNull Executor executor) {
        this.executor = executor;
    }


    @Override
    public void execute(Runnable command) {
        // Return early if this object has been shut down.
        if (shutdown.get()) {
            return;
        }
        executor.execute(
                () -> {
                    // Check again in case it has been shut down in the mean time.
                    if (shutdown.get()) {
                        return;
                    }
                    command.run();
                });
    }

    public void shutdown() {
        shutdown.set(true);
    }
}
