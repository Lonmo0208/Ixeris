package me.decce.ixeris.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import me.decce.ixeris.core.glfw.callback_dispatcher.MouseButtonCallbackDispatcher;
import me.decce.ixeris.core.threading.RenderThreadDispatcher;

public class Ixeris {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MAIN_THREAD_NAME = "Ixeris Event Polling Thread";

    public static IxerisMinecraftAccessor accessor = new IxerisNoopAccessor();

    public static volatile boolean shouldExit;
    public static volatile boolean inEarlyDisplay;
    public static boolean suppressEventPollingWarning;

    public static Thread mainThread;

    private static IxerisConfig config;

    private volatile boolean disconnecting = false;

    public boolean isDisconnecting() {
        return disconnecting;
    }

    public void setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
        MouseButtonCallbackDispatcher.cleanupAll();
        RenderThreadDispatcher.setDisconnecting(disconnecting);
    }

    public void onDisconnect() {
        setDisconnecting(true);
        RenderThreadDispatcher.cleanup();
    }

    public void onConnect() {
        setDisconnecting(false);
        RenderThreadDispatcher.setActive(true);
    }

    public static IxerisConfig getConfig() {
        if (config == null) {
            config = IxerisConfig.load();
            config.save();
        }
        return config;
    }

    public static boolean isOnMainThread() {
        return mainThread == null || Thread.currentThread() == mainThread;
    }
}
