package net.jcraron.pixivcrawler.util;

import java.util.Objects;

public interface ConsumerThrowable<T> {
	void accept(T t) throws Exception;

	default ConsumerThrowable<T> andThen(ConsumerThrowable<? super T> after) {
		Objects.requireNonNull(after);
		return (T t) -> {
			accept(t);
			after.accept(t);
		};
	}
}
