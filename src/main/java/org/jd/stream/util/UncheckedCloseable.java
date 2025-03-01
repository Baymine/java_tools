package org.jd.stream.util;

@FunctionalInterface
public interface UncheckedCloseable extends Runnable, AutoCloseable{

    @Override
    void close() throws Exception;

    @Override
    default void run(){
        try {
            close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static UncheckedCloseable wrap(AutoCloseable c) {
        return c::close;
    }

    default UncheckedCloseable nest(AutoCloseable c) {
        return () -> {
            try (UncheckedCloseable c1 = this){
                c.close();
            }
        };
    }
}
