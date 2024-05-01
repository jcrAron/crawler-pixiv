package net.jcraron.pixivcrawler.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WaittingResult<T> implements Future<T> {
	private T obj;
	private Callable<T> callable;
	private CountDownLatch latch;
	private volatile boolean isDone;
	private volatile boolean isCancel;

	public WaittingResult(Callable<T> callable) {
		this.callable = callable;
		this.latch = new CountDownLatch(1);
		this.isDone = false;
		this.isCancel = false;
	}

	public T get() throws InterruptedException {
		latch.await();
		return obj;
	}

	public T call() throws Exception {
		isDone = true;
		obj = callable.call();
		latch.countDown();
		return obj;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone) {
			return true;
		}
		if (mayInterruptIfRunning) {
			isCancel = true;
			latch.countDown();
			return Thread.interrupted();
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		return isCancel;
	}

	@Override
	public boolean isDone() {
		return isDone;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		latch.await(timeout, unit);
		return obj;
	}
}
