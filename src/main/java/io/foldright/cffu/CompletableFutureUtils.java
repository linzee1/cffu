package io.foldright.cffu;

import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
import io.foldright.cffu.tuple.Tuple2;
import io.foldright.cffu.tuple.Tuple3;
import io.foldright.cffu.tuple.Tuple4;
import io.foldright.cffu.tuple.Tuple5;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/**
 * This class contains the enhanced methods for {@link CompletableFuture}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CompletableFutureUtils {
    /**
     * Returns a new CompletableFuture with the result of all the given CompletableFutures,
     * the new CompletableFuture is completed when all the given CompletableFutures complete.
     * If any of the given CompletableFutures complete exceptionally, then the returned CompletableFuture
     * also does so, with a CompletionException holding this exception as its cause.
     * If no CompletableFutures are provided, returns a CompletableFuture completed
     * with the value {@link Collections#emptyList() emptyList}.
     * <p>
     * Same to {@link CompletableFuture#allOf(CompletableFuture[])},
     * but the returned CompletableFuture contains the results of the given CompletableFutures.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that is completed when all the given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<List<T>> allOfWithResult(CompletableFuture<T>... cfs) {
        final int size = cfs.length;
        if (size == 0) return CompletableFuture.completedFuture(Collections.emptyList());
        if (size == 1) return cfs[0].thenApply(Arrays::asList);
        requireCfEleNonNull(cfs);

        final Object[] result = new Object[size];

        final CompletableFuture<?>[] thenCfs = new CompletableFuture[size];
        for (int i = 0; i < size; i++) {
            final int index = i;
            final CompletableFuture<T> cf = cfs[index];

            CompletableFuture<Void> thenCf = cf.thenAccept(x -> result[index] = x);
            thenCfs[index] = thenCf;
        }

        return CompletableFuture.allOf(thenCfs)
                .thenApply(unused -> (List<T>) Arrays.asList(result));
    }

    /**
     * Returns a new CompletableFuture that success when all the given CompletableFutures success.
     * If any of the given CompletableFutures complete exceptionally, then the returned CompletableFuture
     * also does so *without* waiting other incomplete given CompletableFutures,
     * with a CompletionException holding this exception as its cause.
     * Otherwise, the results, if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by inspecting them individually.
     * If no CompletableFutures are provided, returns a CompletableFuture completed with the value {@code null}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that success when all the given CompletableFutures success
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static CompletableFuture<Void> allOfFastFail(CompletableFuture<?>... cfs) {
        final int size = cfs.length;
        if (size == 0) return CompletableFuture.completedFuture(null);
        if (size == 1) return cfs[0].thenApply(v -> null);
        requireCfEleNonNull(cfs);

        final CompletableFuture[] successOrBeIncomplete = new CompletableFuture[size];
        // NOTE: fill ONE MORE element of failedOrBeIncomplete LATER
        final CompletableFuture[] failedOrBeIncomplete = new CompletableFuture[size + 1];
        fill(cfs, successOrBeIncomplete, failedOrBeIncomplete);

        // NOTE: fill the ONE MORE element of failedOrBeIncomplete HERE:
        //       a cf which success when all given cfs success, otherwise be incomplete
        failedOrBeIncomplete[size] = CompletableFuture.allOf(successOrBeIncomplete);

        return (CompletableFuture) CompletableFuture.anyOf(failedOrBeIncomplete);
    }

    /**
     * Returns a new CompletableFuture with the result of all the given CompletableFutures,
     * the new CompletableFuture success when all the given CompletableFutures success.
     * If any of the given CompletableFutures complete exceptionally, then the returned CompletableFuture
     * also does so *without* waiting other incomplete given CompletableFutures,
     * with a CompletionException holding this exception as its cause.
     * Otherwise, the results, if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by inspecting them individually.
     * If no CompletableFutures are provided, returns a CompletableFuture completed
     * with the value {@link Collections#emptyList() emptyList}.
     * <p>
     * Same to {@link CompletableFutureUtils#allOfFastFail(CompletableFuture[])},
     * but the returned CompletableFuture contains the results of the given CompletableFutures.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that success when all the given CompletableFutures success
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see CompletableFutureUtils#allOfFastFail(CompletableFuture[])
     */
    @Contract(pure = true)
    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> CompletableFuture<List<T>> allOfFastFailWithResult(CompletableFuture<T>... cfs) {
        final int size = cfs.length;
        if (size == 0) return CompletableFuture.completedFuture(Collections.emptyList());
        if (size == 1) return cfs[0].thenApply(Arrays::asList);
        requireCfEleNonNull(cfs);

        final CompletableFuture[] successOrBeIncomplete = new CompletableFuture[size];
        // NOTE: fill ONE MORE element of failedOrBeIncomplete LATER
        final CompletableFuture[] failedOrBeIncomplete = new CompletableFuture[size + 1];
        fill(cfs, successOrBeIncomplete, failedOrBeIncomplete);

        // NOTE: fill the ONE MORE element of failedOrBeIncomplete HERE:
        //       a cf which success when all given cfs success, otherwise be incomplete
        failedOrBeIncomplete[size] = allOfWithResult(successOrBeIncomplete);

        return (CompletableFuture) CompletableFuture.anyOf(failedOrBeIncomplete);
    }

    private static void requireCfEleNonNull(CompletableFuture<?>... cfs) {
        for (int i = 0; i < cfs.length; i++) {
            requireNonNull(cfs[i], "cf" + i + " is null");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fill(CompletableFuture[] cfs,
                             CompletableFuture[] successOrBeIncomplete,
                             CompletableFuture[] failedOrBeIncomplete) {
        final CompletableFuture incomplete = new CompletableFuture();

        for (int i = 0; i < cfs.length; i++) {
            final CompletableFuture cf = cfs[i];

            successOrBeIncomplete[i] = cf.handle((v, ex) -> ex == null ? cf : incomplete)
                    .thenCompose(Function.identity());

            failedOrBeIncomplete[i] = cf.handle((v, ex) -> ex == null ? incomplete : cf)
                    .thenCompose(Function.identity());
        }
    }

    /**
     * Returns a new CompletableFuture that is completed
     * when any of the given CompletableFutures complete, with the same result.
     * <p>
     * Same as {@link CompletableFuture#anyOf(CompletableFuture[])},
     * but return result type is specified type instead of {@code Object}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that is completed with the result
     * or exception from any of the given CompletableFutures when one completes
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see #anyOfSuccessWithType(CompletableFuture[])
     * @see CompletableFuture#anyOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> anyOfWithType(CompletableFuture<T>... cfs) {
        return (CompletableFuture<T>) CompletableFuture.anyOf(cfs);
    }

    /**
     * Returns a new CompletableFuture that success when any of the given CompletableFutures success,
     * with the same result. Otherwise, all the given CompletableFutures complete exceptionally,
     * the returned CompletableFuture also does so, with a CompletionException holding
     * an exception from any of the given CompletableFutures as its cause. If no CompletableFutures are provided,
     * returns a new CompletableFuture that is already completed exceptionally
     * with a CompletionException holding a {@link NoCfsProvidedException} exception as its cause.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that success
     * when any of the given CompletableFutures success, with the same result
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see #anyOfSuccessWithType(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static CompletableFuture<Object> anyOfSuccess(CompletableFuture<?>... cfs) {
        final int size = cfs.length;
        if (size == 0) return failedFuture(new NoCfsProvidedException());
        if (size == 1) return (CompletableFuture<Object>) copy(cfs[0]);
        requireCfEleNonNull(cfs);

        // NOTE: fill ONE MORE element of successOrBeIncompleteCfs LATER
        final CompletableFuture[] successOrBeIncomplete = new CompletableFuture[size + 1];
        final CompletableFuture[] failedOrBeIncomplete = new CompletableFuture[size];
        fill(cfs, successOrBeIncomplete, failedOrBeIncomplete);

        // NOTE: fill the ONE MORE element of successOrBeIncompleteCfs HERE
        //       a cf which failed when all given cfs failed, otherwise be incomplete
        successOrBeIncomplete[size] = CompletableFuture.allOf(failedOrBeIncomplete);

        return CompletableFuture.anyOf(successOrBeIncomplete);
    }

    /**
     * Returns a new CompletableFuture that success when any of the given CompletableFutures success,
     * with the same result. Otherwise, all the given CompletableFutures complete exceptionally,
     * the returned CompletableFuture also does so, with a CompletionException holding
     * an exception from any of the given CompletableFutures as its cause. If no CompletableFutures are provided,
     * returns a new CompletableFuture that is already completed exceptionally
     * with a CompletionException holding a {@link NoCfsProvidedException} exception as its cause.
     * <p>
     * Same as {@link #anyOfSuccess(CompletableFuture[])},
     * but return result type is specified type instead of {@code Object}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that success
     * when any of the given CompletableFutures success, with the same result
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @see #anyOfWithType(CompletableFuture[])
     */
    @Contract(pure = true)
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> anyOfSuccessWithType(CompletableFuture<T>... cfs) {
        return (CompletableFuture<T>) anyOfSuccess(cfs);
    }

    /**
     * Returns a new CompletableFuture that is completed when the given two CompletableFutures complete.
     * If any of the given CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException holding this exception as its cause.
     *
     * @return a new CompletableFuture that is completed when the given 2 CompletableFutures complete
     * @throws NullPointerException if any of the given CompletableFutures are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <T1, T2> CompletableFuture<Tuple2<T1, T2>> combine(
            CompletableFuture<T1> cf1, CompletableFuture<T2> cf2) {
        requireNonNull(cf1, "cf1 is null");
        requireNonNull(cf2, "cf2 is null");

        final Object[] result = new Object[2];
        return CompletableFuture.allOf(
                cf1.thenAccept(t1 -> result[0] = t1),
                cf2.thenAccept(t2 -> result[1] = t2)
        ).thenApply(unused ->
                Tuple2.of((T1) result[0], (T2) result[1])
        );
    }

    /**
     * Returns a new CompletableFuture that is completed when the given three CompletableFutures complete.
     * If any of the given CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException holding this exception as its cause.
     *
     * @return a new CompletableFuture that is completed when the given 3 CompletableFutures complete
     * @throws NullPointerException if any of the given CompletableFutures are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3> CompletableFuture<Tuple3<T1, T2, T3>> combine(
            CompletableFuture<T1> cf1, CompletableFuture<T2> cf2, CompletableFuture<T3> cf3) {
        requireNonNull(cf1, "cf1 is null");
        requireNonNull(cf2, "cf2 is null");
        requireNonNull(cf3, "cf3 is null");

        final Object[] result = new Object[3];
        return CompletableFuture.allOf(
                cf1.thenAccept(t1 -> result[0] = t1),
                cf2.thenAccept(t2 -> result[1] = t2),
                cf3.thenAccept(t3 -> result[2] = t3)
        ).thenApply(unused ->
                Tuple3.of((T1) result[0], (T2) result[1], (T3) result[2])
        );
    }

    /**
     * Returns a new CompletableFuture that is completed when the given 4 CompletableFutures complete.
     * If any of the given CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException holding this exception as its cause.
     *
     * @return a new CompletableFuture that is completed when the given 4 CompletableFutures complete
     * @throws NullPointerException if any of the given CompletableFutures are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4> CompletableFuture<Tuple4<T1, T2, T3, T4>> combine(
            CompletableFuture<T1> cf1, CompletableFuture<T2> cf2,
            CompletableFuture<T3> cf3, CompletableFuture<T4> cf4) {
        requireNonNull(cf1, "cf1 is null");
        requireNonNull(cf2, "cf2 is null");
        requireNonNull(cf3, "cf3 is null");
        requireNonNull(cf4, "cf4 is null");

        final Object[] result = new Object[4];
        return CompletableFuture.allOf(
                cf1.thenAccept(t1 -> result[0] = t1),
                cf2.thenAccept(t2 -> result[1] = t2),
                cf3.thenAccept(t3 -> result[2] = t3),
                cf4.thenAccept(t4 -> result[3] = t4)
        ).thenApply(unused ->
                Tuple4.of((T1) result[0], (T2) result[1], (T3) result[2], (T4) result[3])
        );
    }

    /**
     * Returns a new CompletableFuture that is completed when the given 5 CompletableFutures complete.
     * If any of the given CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException holding this exception as its cause.
     *
     * @return a new CompletableFuture that is completed when the given 5 CompletableFutures complete
     * @throws NullPointerException if any of the given CompletableFutures are {@code null}
     * @see CompletableFuture#allOf(CompletableFuture[])
     */
    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5> CompletableFuture<Tuple5<T1, T2, T3, T4, T5>> combine(
            CompletableFuture<T1> cf1, CompletableFuture<T2> cf2,
            CompletableFuture<T3> cf3, CompletableFuture<T4> cf4, CompletableFuture<T5> cf5) {
        requireNonNull(cf1, "cf1 is null");
        requireNonNull(cf2, "cf2 is null");
        requireNonNull(cf3, "cf3 is null");
        requireNonNull(cf4, "cf4 is null");
        requireNonNull(cf5, "cf5 is null");

        final Object[] result = new Object[5];
        return CompletableFuture.allOf(
                cf1.thenAccept(t1 -> result[0] = t1),
                cf2.thenAccept(t2 -> result[1] = t2),
                cf3.thenAccept(t3 -> result[2] = t3),
                cf4.thenAccept(t4 -> result[3] = t4),
                cf5.thenAccept(t5 -> result[4] = t5)
        ).thenApply(unused ->
                Tuple5.of((T1) result[0], (T2) result[1], (T3) result[2], (T4) result[3], (T5) result[4])
        );
    }

    ////////////////////////////////////////////////////////////////////////////////
    //# helper methods
    ////////////////////////////////////////////////////////////////////////////////

    @Contract(pure = true)
    static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        if (IS_JAVA9_PLUS) {
            return CompletableFuture.failedFuture(ex);
        }
        final CompletableFuture<T> cf = new CompletableFuture<>();
        cf.completeExceptionally(ex);
        return cf;
    }

    @Contract(pure = true)
    static <U> CompletableFuture<U> copy(CompletableFuture<U> cf) {
        if (IS_JAVA9_PLUS) {
            return cf.copy();
        }
        return cf.thenApply(Function.identity());
    }

    ////////////////////////////////////////////////////////////////////////////////
    //# Java version check logic for compatibility
    ////////////////////////////////////////////////////////////////////////////////

    static final boolean IS_JAVA9_PLUS;

    static final boolean IS_JAVA12_PLUS;

    static final boolean IS_JAVA19_PLUS;

    static {
        boolean b;

        try {
            // `completedStage` is the new method of CompletableFuture since java 9
            CompletableFuture.completedStage(null);
            b = true;
        } catch (NoSuchMethodError e) {
            b = false;
        }
        IS_JAVA9_PLUS = b;

        final CompletableFuture<Integer> cf = CompletableFuture.completedFuture(42);
        try {
            // `exceptionallyCompose` is the new method of CompletableFuture since java 12
            cf.exceptionallyCompose(x -> cf);
            b = true;
        } catch (NoSuchMethodError e) {
            b = false;
        }
        IS_JAVA12_PLUS = b;

        try {
            // `resultNow` is the new method of CompletableFuture since java 19
            cf.resultNow();
            b = true;
        } catch (NoSuchMethodError e) {
            b = false;
        }
        IS_JAVA19_PLUS = b;
    }

    private CompletableFutureUtils() {
    }
}
