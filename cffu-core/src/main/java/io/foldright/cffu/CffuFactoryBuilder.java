package io.foldright.cffu;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
import io.foldright.cffu.spi.ExecutorWrapperProvider;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;


/**
 * {@link CffuFactoryBuilder} is the builder of {@link CffuFactory}, creates by {@link CffuFactory#builder(Executor)}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see CffuFactory
 * @see Cffu
 */
@ThreadSafe
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CffuFactoryBuilder {
    @NonNull
    private final Executor defaultExecutor;

    private volatile boolean forbidObtrudeMethods = false;

    CffuFactoryBuilder(Executor defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Builder Methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets {@code forbidObtrudeMethods} or not.
     *
     * @see CffuFactory#forbidObtrudeMethods()
     * @see Cffu#obtrudeValue(Object)
     * @see Cffu#obtrudeException(Throwable)
     */
    public CffuFactoryBuilder forbidObtrudeMethods(boolean forbid) {
        this.forbidObtrudeMethods = forbid;
        return this;
    }

    /**
     * Builds the cffu factory.
     *
     * @return the built cffu factory
     */
    @Contract(pure = true)
    public CffuFactory build() {
        return new CffuFactory(defaultExecutor, forbidObtrudeMethods);
    }

    static Executor wrapExecutor(Executor executor) {
        for (ExecutorWrapperProvider provider : EXECUTOR_WRAPPER_PROVIDERS) {
            Supplier<String> msg = () -> provider + "(class: " + provider.getClass().getName() + ") return null";
            executor = requireNonNull(provider.wrap(executor), msg);
        }
        return executor;
    }

    private static final List<ExecutorWrapperProvider> EXECUTOR_WRAPPER_PROVIDERS = loadExecutorWrapperProviders();

    private static List<ExecutorWrapperProvider> loadExecutorWrapperProviders() {
        final ServiceLoader<ExecutorWrapperProvider> loader = ServiceLoader.load(ExecutorWrapperProvider.class);
        return StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toList());
    }
}
