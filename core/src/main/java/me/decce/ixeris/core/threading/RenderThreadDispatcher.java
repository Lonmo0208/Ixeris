package me.decce.ixeris.core.threading;

import me.decce.ixeris.core.glfw.callback_dispatcher.CursorPosCallbackDispatcher;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderThreadDispatcher {
    private static final AtomicInteger suppressCursorPosCallbacks = new AtomicInteger();

    private static final ConcurrentLinkedQueue<Runnable> recordingQueue = new ConcurrentLinkedQueue<>();

    private static volatile boolean isActive = true;

    private static volatile boolean isDisconnecting = false;

    public static void runLater(Runnable runnable) {
        if (!isActive || isDisconnecting) {
            return;
        }

        if (suppressCursorPosCallbacks.get() > 0 && runnable instanceof CursorPosCallbackDispatcher.DispatchedRunnable) {
            return;
        }
        recordingQueue.add(runnable);
    }

    public static void replayQueue() {
        if (isDisconnecting || !isActive) {
            recordingQueue.clear();
            return;
        }

        Runnable nextTask;
        while ((nextTask = recordingQueue.poll()) != null) {
            try {
                nextTask.run();
            } catch (Exception e) {
                me.decce.ixeris.core.Ixeris.LOGGER.error("Error executing queued task", e);
            }
        }
    }

    public static void clearQueuedCursorPosCallbacks() {
        if (isDisconnecting) {
            recordingQueue.clear();
            return;
        }
        recordingQueue.removeIf(r -> r instanceof CursorPosCallbackDispatcher.DispatchedRunnable);
    }

    public static void suppressCursorPosCallbacks() {
        suppressCursorPosCallbacks.getAndIncrement();
    }

    public static void unsuppressCursorPosCallbacks() {
        suppressCursorPosCallbacks.getAndDecrement();
    }

    public static void setDisconnecting(boolean disconnecting) {
        isDisconnecting = disconnecting;
        if (disconnecting) {
            recordingQueue.clear();
        }
    }

    public static void setActive(boolean active) {
        isActive = active;
        if (!active) {
            recordingQueue.clear();
        }
    }

    public static void cleanup() {
        setDisconnecting(true);
        setActive(false);
        recordingQueue.clear();
        suppressCursorPosCallbacks.set(0);
    }
}