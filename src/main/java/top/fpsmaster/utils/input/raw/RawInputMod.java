package top.fpsmaster.utils.input.raw;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RawInputMod {
    public static final AtomicInteger dx = new AtomicInteger(0);
    public static final AtomicInteger dy = new AtomicInteger(0);
    private Thread inputThread;
    private static ArrayList<Mouse> mouses = new ArrayList<>();
    private static Controller[] controllers;
    private static boolean hasDx8 = false;

    public void start() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                return;
            }
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
            String environment;
            if (checkLibrary("jinput-dx8")) {
                environment = "DirectInputEnvironmentPlugin";
                hasDx8 = true;
            } else if (checkLibrary("jinput-raw")) {
                environment = "DirectAndRawInputEnvironmentPlugin";
            } else {
                return;
            }
            Class<?> aClass = Class.forName("net.java.games.input." + environment);
            aClass.getDeclaredConstructor().setAccessible(true);
            controllers = (((ControllerEnvironment) aClass.newInstance())).getControllers();
            inputThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (controllers != null && mouses.isEmpty()) {
                            for (Controller controller : controllers) {
                                if (controller.getType() == Controller.Type.MOUSE) {
                                    RawInputMod.mouses.add((Mouse) controller);
                                    if (RawInputMod.hasDx8) break; // Thank UknownPerson for discovering the logical mouse.
                                }
                            }
                        }
                        if (Minecraft.getMinecraft().currentScreen != null) {
                            dx.set(0);
                            dy.set(0);
                            for (Mouse mouse : mouses) {
                                mouse.poll();
                            }
                        } else {
                            for (Mouse mouse : mouses) {
                                mouse.poll();
                                dx.addAndGet((int) mouse.getX().getPollData());
                                dy.addAndGet((int) mouse.getY().getPollData());
                            }
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    ClientLogger.error("Failed to start raw input");
                }
            });
            inputThread.setName("inputThread");
            inputThread.setDaemon(true);
            inputThread.start();
        } catch (Exception e) {
            ClientLogger.error("Failed to start raw input");
        }
    }

    public void stop() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
                inputThread.join(200L);
            }
            inputThread = null;
            mouses = new ArrayList<>();
            dx.set(0);
            dy.set(0);
            Minecraft.getMinecraft().mouseHelper = new MouseHelper();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            ClientLogger.error("Failed to stop raw input");
        }
    }

    public static boolean checkLibrary(String name) {
        try {
            String path = System.getProperty("java.library.path");
            if (path != null) {
                String mapped = System.mapLibraryName(name);
                String[] paths = path.split(File.pathSeparator);
                for (String libPath : paths) {
                    if (new File(libPath, mapped).exists()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}



