package com.sms.client.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.scene.Node;

/** Chạy lời gọi RMI ngoài FX thread; callback onOk/onErr luôn chạy trên FX thread. */
public final class AsyncRunner {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "sms-rmi-call");
        thread.setDaemon(true);
        return thread;
    });

    private AsyncRunner() {
    }

    /** Lỗi được xử lý mặc định bằng {@link Alerts#handle(Throwable)}. */
    public static <T> void run(Callable<T> work, Consumer<T> onOk, Node... disableWhileRunning) {
        run(work, onOk, Alerts::handle, disableWhileRunning);
    }

    public static <T> void run(Callable<T> work, Consumer<T> onOk, Consumer<Throwable> onErr,
            Node... disableWhileRunning) {
        for (Node node : disableWhileRunning) {
            node.setDisable(true);
        }
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            enable(disableWhileRunning);
            onOk.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            enable(disableWhileRunning);
            onErr.accept(task.getException());
        });
        POOL.execute(task);
    }

    private static void enable(Node[] nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }
}
