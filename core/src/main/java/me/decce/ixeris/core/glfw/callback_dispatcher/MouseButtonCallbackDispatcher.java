/*
Auto-generated. See the generator directory in project root.
*/

package me.decce.ixeris.core.glfw.callback_dispatcher;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.decce.ixeris.core.Ixeris;
import me.decce.ixeris.core.threading.RenderThreadDispatcher;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.system.Callback;

public class MouseButtonCallbackDispatcher {
    private static final Long2ReferenceMap<MouseButtonCallbackDispatcher> instance = new Long2ReferenceArrayMap<>(1);

    private final ReferenceArrayList<GLFWMouseButtonCallbackI> mainThreadCallbacks = new ReferenceArrayList<>(1);
    private boolean lastCallbackSet;
    public GLFWMouseButtonCallbackI lastCallback;
    public long lastCallbackAddress;

    private final long window;
    public volatile boolean suppressChecks;

    private volatile boolean isGameActive = true;

    private static volatile boolean isDisconnecting = false;

    private MouseButtonCallbackDispatcher(long window) {
        this.window = window;
    }

    public synchronized static MouseButtonCallbackDispatcher get(long window) {
        if (!instance.containsKey(window)) {
            instance.put(window, new MouseButtonCallbackDispatcher(window));
            instance.get(window).validate();
        }
        return instance.get(window);
    }

    public synchronized void registerMainThreadCallback(GLFWMouseButtonCallbackI callback) {
        mainThreadCallbacks.add(callback);
        this.validate();
    }

    public synchronized long update(long newAddress) {
        suppressChecks = true;
        long ret = lastCallbackAddress;
        if (newAddress == 0L && this.mainThreadCallbacks.isEmpty()) {
            GLFW.nglfwSetMouseButtonCallback(window, 0L);
        }
        else {
            GLFW.nglfwSetMouseButtonCallback(window, CommonCallbacks.mouseButtonCallback.address());
        }
        lastCallbackAddress = newAddress;
        if (!lastCallbackSet) {
            lastCallback = newAddress == 0L ? null : Callback.get(newAddress);
        }
        lastCallbackSet = false;
        suppressChecks = false;
        return ret;
    }

    public synchronized void update(GLFWMouseButtonCallbackI callback) {
        lastCallback = callback;
        lastCallbackSet = true;
    }

    public synchronized void validate() {
        suppressChecks = true;
        var current = GLFW.nglfwSetMouseButtonCallback(window, CommonCallbacks.mouseButtonCallback.address());
        if (current == 0L) {
            if (this.mainThreadCallbacks.isEmpty()) {
                // Remove callback when not needed
                GLFW.nglfwSetMouseButtonCallback(window, 0L);
            }
        }
        else if (current != CommonCallbacks.mouseButtonCallback.address()) {
            // This only happens when mods register callbacks without using LWJGL (e.x. directly in native code)
            lastCallback = Callback.get(current);
            lastCallbackAddress = current;
        }
        suppressChecks = false;
    }

    public void onCallback(long window, int button, int action, int mods) {
        if (this.window != window) {
            return;
        }

        if (!isGameActive || isDisconnecting) {
            return;
        }

        for (int i = 0; i < mainThreadCallbacks.size(); i++) {
            mainThreadCallbacks.get(i).invoke(window, button, action, mods);
        }
        if (lastCallback != null) {
            RenderThreadDispatcher.runLater((DispatchedRunnable) () -> {
                if (!isGameActive || isDisconnecting || lastCallback == null) {
                    return;
                }
                try {
                    lastCallback.invoke(window, button, action, mods);
                } catch (Exception e) {
                    Ixeris.LOGGER.error("Error in mouse callback", e);
                }
            });
        }
    }

    public synchronized void setGameActive(boolean active) {
        this.isGameActive = active;
        if (!active) {
            mainThreadCallbacks.clear();
            lastCallback = null;
            lastCallbackAddress = 0L;
        }
    }

    public static void setDisconnecting(boolean disconnecting) {
        isDisconnecting = disconnecting;
        if (disconnecting) {
            for (var dispatcher : instance.values()) {
                dispatcher.setGameActive(false);
            }
        }
    }

    public synchronized static void cleanupAll() {
        for (var dispatcher : instance.values()) {
            dispatcher.setGameActive(false);
        }
        instance.clear();
    }

    @FunctionalInterface
    public interface DispatchedRunnable extends Runnable {
    }
}