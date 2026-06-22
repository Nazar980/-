package xyz.breadloaf.imguimc;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.joml.Vector2d;

public class WindowScaling {

    public static int X_OFFSET = 0;
    public static int Y_OFFSET = 0;
    public static int Y_TOP_OFFSET = 0;

    public static int WIDTH = 0;
    public static int HEIGHT = 0;

    public static boolean DISABLE_POST_PROCESSORS = false;

    private static Window getGameWindow() {
        return Minecraft.getInstance().getWindow();
    }

    private static boolean isReady() {
        return WIDTH > 0 && HEIGHT > 0;
    }

    public static Vector2d scalePoint(Vector2d point) {
        return scalePoint(point.x, point.y);
    }

    public static Vector2d scalePoint(double x, double y) {
        if (!isReady()) {
            return new Vector2d(x, y);
        }

        Window window = getGameWindow();

        float xScale = (float) WIDTH / window.getScreenWidth();
        float yScale = (float) HEIGHT / window.getScreenHeight();

        x *= xScale;
        y *= yScale;

        x += X_OFFSET;
        y += Y_OFFSET;

        return new Vector2d(x, y);
    }

    public static Vector2d unscalePoint(Vector2d point) {
        return unscalePoint(point.x, point.y);
    }

    public static Vector2d unscalePoint(double x, double y) {
        if (!isReady()) {
            return new Vector2d(x, y);
        }

        Window window = getGameWindow();

        float xScale = (float) WIDTH / window.getScreenWidth();
        float yScale = (float) HEIGHT / window.getScreenHeight();

        x -= X_OFFSET;
        y -= Y_OFFSET;

        x /= xScale;
        y /= yScale;

        return new Vector2d(x, y);
    }

    public static Vector2d scaleWidthHeight(double width, double height) {
        if (!isReady()) {
            return new Vector2d(width, height);
        }

        Window window = getGameWindow();

        float xScale = (float) WIDTH / window.getScreenWidth();
        float yScale = (float) HEIGHT / window.getScreenHeight();

        width *= xScale;
        height *= yScale;

        return new Vector2d(width, height);
    }

    public static Vector2d unscaleWidthHeight(double width, double height) {
        if (!isReady()) {
            return new Vector2d(width, height);
        }

        Window window = getGameWindow();

        float xScale = (float) WIDTH / window.getScreenWidth();
        float yScale = (float) HEIGHT / window.getScreenHeight();

        width /= xScale;
        height /= yScale;

        return new Vector2d(width, height);
    }

    public static boolean isChanged() {
        if (!isReady()) {
            return false;
        }

        Window window = getGameWindow();
        return !(window.getWidth() == WIDTH && window.getHeight() == HEIGHT && X_OFFSET == 0 && Y_OFFSET == 0);
    }

    public static void update() {
        DISABLE_POST_PROCESSORS = isChanged();
    }
}
