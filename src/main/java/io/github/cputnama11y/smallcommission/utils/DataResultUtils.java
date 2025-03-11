package io.github.cputnama11y.smallcommission.utils;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;

public class DataResultUtils {
    public static <T> DataResult<T> exceptionToResult(FailableSupplier<T, Throwable> supplier) {
        try {
            return DataResult.success(supplier.get());
        } catch (Throwable t) {
            return DataResult.error(t::getMessage);
        }
    }

    public static DataResult<Unit> exceptionToResult(FailableRunnable<Throwable> action) {
        try {
            action.run();
            return DataResult.success(Unit.INSTANCE);
        } catch (Throwable t) {
            return DataResult.error(t::getMessage);
        }
    }
}
