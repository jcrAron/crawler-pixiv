package net.jcraron.pixivcrawler.util;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class LazyObject<T> implements Supplier<T>, Serializable {
	private static final long serialVersionUID = 8689140228040241785L;
	private T obj;
	private Supplier<T> supplier;

	public LazyObject(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T get() {
		if (obj == null) {
			obj = supplier.get();
		}
		return obj;
	}

	public static <T> LazyObject<T> of(Supplier<T> supplier) {
		return new LazyObject<>(supplier);
	}

	public static <T> LazyObject<T> of(Callable<T> callable, @Nullable Function<Exception, T> catcher, @Nullable Runnable finallySegment) {
		return new LazyObject<T>(() -> {
			try {
				return callable.call();
			} catch (Exception e) {
				if (catcher != null) {
					return catcher.apply(e);
				} else {
					e.printStackTrace();
					return null;
				}
			} finally {
				if (finallySegment != null) {
					finallySegment.run();
				}
			}
		});
	}
}
