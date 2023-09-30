package me.khajiitos.servercountryflags.common.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.APIResponse;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerMapScreen extends Screen {
    public static final ResourceLocation MAP_TEXTURE = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/misc/map.jpg");
    public static final ResourceLocation POINT_TEXTURE = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/misc/point.png");
    public static final ResourceLocation POINT_HOVERED_TEXTURE = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/misc/point_hovered.png");
    public static final ResourceLocation POINT_HOME_TEXTURE = new ResourceLocation(ServerCountryFlags.MOD_ID, "textures/misc/point_home.png");
    public static final double MAP_TEXTURE_ASPECT = 3600.0 / 1800.0;
    public static final double POINT_TEXTURE_ASPECT = 526.0 / 754.0;
    public static final double ZOOM_STRENGTH = 0.1;

    private int mapStartX, mapStartY, mapWidth, mapHeight;
    private final Screen parent;
    private final ArrayList<Point> points = new ArrayList<>();

    private double zoomedAreaStartX = 0.0;
    private double zoomedAreaStartY = 0.0;
    private double zoomedAreaWidth = 1.0;
    private double zoomedAreaHeight = 1.0;

    private boolean movingMap = false;
    private double movingMapLastX = -1.0;
    private double movingMapLastY = -1.0;

    public ServerMapScreen(Screen parent) {
        super(Component.translatable("servercountryflags.servermap.title"));
        this.parent = parent;

        if (Config.cfg.showHomeOnMap && ServerCountryFlags.localLocation != null) {
            addPoint(null, ServerCountryFlags.localLocation);
        }

        for (Map.Entry<String, APIResponse> entry : ServerCountryFlags.servers.entrySet()) {
            if (entry.getValue().locationInfo() != null) {
                addPoint(entry.getKey(), entry.getValue().locationInfo());
            }
        }
    }

    public Point getPoint(double lon, double lat) {
        for (Point point : points) {

            if (point.locationInfo.longitude == lon && point.locationInfo.latitude == lat) {
                return point;
            }
        }
        return null;
    }

    public double clampDouble(double value, double min, double max) {
        if (value > max)
            value = max;
        else if (value < min)
            value = min;
        return value;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (this.movingMap) {
            double deltaX = (this.movingMapLastX - mouseX) / this.mapWidth * zoomedAreaWidth;
            double deltaY = (this.movingMapLastY - mouseY) / this.mapHeight * zoomedAreaHeight;
            this.movingMapLastX = mouseX;
            this.movingMapLastY = mouseY;

            zoomedAreaStartX = clampDouble(zoomedAreaStartX + deltaX, 0.0, 1.0 - zoomedAreaWidth);
            zoomedAreaStartY = clampDouble(zoomedAreaStartY + deltaY, 0.0, 1.0 - zoomedAreaHeight);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.movingMap = false;
            this.movingMapLastX = -1.0;
            this.movingMapLastY = -1.0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= mapStartX && mouseX <= mapStartX + mapWidth && mouseY >= mapStartY && mouseY <= mapStartY + mapHeight) {
            this.movingMap = true;
            this.movingMapLastX = mouseX;
            this.movingMapLastY = mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= mapStartX && mouseX <= mapStartX + mapWidth && mouseY >= mapStartY && mouseY <= mapStartY + mapHeight) {
            double oldWidth = zoomedAreaWidth;
            double oldHeight = zoomedAreaHeight;

            zoomedAreaWidth = clampDouble(zoomedAreaWidth - amount * ZOOM_STRENGTH, 0.05, 1.0);
            zoomedAreaHeight = clampDouble(zoomedAreaHeight - amount * ZOOM_STRENGTH, 0.05, 1.0);

            double widthDelta = oldWidth - zoomedAreaWidth;
            double heightDelta = oldHeight - zoomedAreaHeight;

            zoomedAreaStartX = clampDouble(zoomedAreaStartX + ((mouseX - mapStartX) / mapWidth) * widthDelta, 0.0, 1.0 - zoomedAreaWidth);
            zoomedAreaStartY = clampDouble(zoomedAreaStartY + ((mouseY - mapStartY) / mapHeight) * heightDelta, 0.0, 1.0 - zoomedAreaHeight);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }
    }

    private void addPoint(String name, LocationInfo locationInfo) {
        Point point = getPoint(locationInfo.longitude, locationInfo.latitude);
        if (point != null) {
            point.addServer(name);
        } else {
            points.add(new Point(name, locationInfo));
        }
    }

    @Override
    public void init() {

        this.addRenderableWidget(new Button.Builder(
                Component.translatable("selectServer.refresh"),
                (button) -> {
                    this.clearWidgets();
                    this.init();

                    if (ServerCountryFlags.serverList == null) {
                        return;
                    }

                    if (Config.cfg.reloadOnRefresh) {
                        points.clear();
                        ServerCountryFlags.servers.clear();
                        ServerCountryFlags.localLocation = null;
                    }

                    if (ServerCountryFlags.localLocation == null || NetworkChangeDetector.check()) {
                        ServerCountryFlags.updateLocalLocationInfo();
                    }

                    for (int i = 0; i < ServerCountryFlags.serverList.size(); i++) {
                        if (ServerCountryFlags.servers.containsKey(ServerCountryFlags.serverList.get(i).ip)) {
                            continue;
                        }
                        ServerCountryFlags.updateServerLocationInfo(ServerCountryFlags.serverList.get(i).ip);
                    }
                }
        ).bounds(this.width / 2 - 105, this.height - 26, 100, 20).build());

        this.addRenderableWidget(new Button.Builder(
                Component.translatable("gui.back"),
                (button) -> Minecraft.getInstance().setScreen(this.parent)
        ).bounds(this.width / 2 + 5, this.height - 26, 100, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.getTitle().getVisualOrderText(), this.width / 2, 12, 0xFFFFFFFF);
        context.fill(0, 32, this.width, this.height - 32, 0xAA000000);

        mapHeight = this.height - 64;
        mapWidth = (int)(mapHeight * MAP_TEXTURE_ASPECT);

        if (mapWidth > this.width) {
            mapWidth = this.width;
            mapHeight = (int)(mapWidth / MAP_TEXTURE_ASPECT);
        }

        mapStartX = this.width / 2 - mapWidth / 2;
        mapStartY = 32 + ((this.height - 64) / 2 - mapHeight / 2);

        // TODO: this doesn't seem to work anymore
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        context.blit(MAP_TEXTURE, mapStartX, mapStartY, mapWidth, mapHeight, (float)(mapWidth * zoomedAreaStartX), (float)(mapHeight * zoomedAreaStartY), (int)(mapWidth * zoomedAreaWidth), (int)(mapHeight * zoomedAreaHeight), mapWidth, mapHeight);
        Point hoveredPoint = null;

        int pointHeight = mapHeight / 20;
        int pointWidth = (int)(pointHeight * POINT_TEXTURE_ASPECT);

        for (int i = points.size() - 1; i >= 0; i--) {
            Point point = points.get(i);
            Coordinates coords = latlonToPos(point.locationInfo.latitude, point.locationInfo.longitude, mapWidth, mapHeight);
            int pointStartX = mapStartX + coords.x - (pointWidth / 2);
            int pointStartY = mapStartY + coords.y - pointHeight;

            if (coords.x < 0 || coords.x > mapWidth - (pointWidth / 2) || coords.y < pointHeight || coords.y > mapHeight)
                continue;

            if (mouseX >= pointStartX && mouseX <= pointStartX + pointWidth && mouseY >= pointStartY && mouseY <= pointStartY + pointHeight) {
                hoveredPoint = point;
                break;
            }
        }

        for (Point point : this.points) {
            point.render(context, hoveredPoint == point);
        }

        if (hoveredPoint != null) {
            this.setTooltipForNextRenderPass(hoveredPoint.getTooltip());
        }

        RenderSystem.disableBlend();
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void tick() {
        super.tick();

        // Syncs the location infos from the server list to here
        this.points.clear();

        if (ServerCountryFlags.localLocation != null) {
            addPoint(null, ServerCountryFlags.localLocation);
        }

        for (Map.Entry<String, APIResponse> entry : ServerCountryFlags.servers.entrySet()) {
            if (entry.getValue().locationInfo() != null) {
                addPoint(entry.getKey(), entry.getValue().locationInfo());
            }
        }
    }

    public class Point {
        LocationInfo locationInfo;
        List<String> servers;
        public boolean hasHome;

        public Point(String beginningName, LocationInfo beginningLocationInfo) {
            this.servers = new ArrayList<>();
            this.locationInfo = beginningLocationInfo;
            this.addServer(beginningName);
        }

        public void addServer(String name) {
            if (name == null) {
                this.hasHome = true;
            }
            this.servers.add(name);
        }

        public List<FormattedCharSequence> getTooltip() {
            List<FormattedCharSequence> list = new ArrayList<>();
            list.add(Component.literal((Config.cfg.showDistrict && !locationInfo.districtName.equals("") ? (locationInfo.districtName + ", ") : "") + locationInfo.cityName + ", " + locationInfo.countryName).withStyle(ChatFormatting.BOLD).getVisualOrderText());
            list.add(Component.nullToEmpty(null).getVisualOrderText());

            for (String server : this.servers) {
                if (server == null) {
                    list.add(Component.translatable("servercountryflags.servermap.home").withStyle(ChatFormatting.BOLD).getVisualOrderText());
                } else {
                    list.add(Component.literal(server).getVisualOrderText());
                }
            }
            return list;
        }

        private void render(GuiGraphics context, boolean hovered) {
            Coordinates coords = latlonToPos(this.locationInfo.latitude, this.locationInfo.longitude, mapWidth, mapHeight);
            int pointHeight = mapHeight / 20;
            int pointWidth = (int)(pointHeight * POINT_TEXTURE_ASPECT);
            int pointStartX = mapStartX + coords.x - (pointWidth / 2);
            int pointStartY = mapStartY + coords.y - pointHeight;

            if (coords.x < 0 || coords.x > mapWidth - (pointWidth / 2) || coords.y < pointHeight || coords.y > mapHeight)
                return;

            ResourceLocation texture = POINT_TEXTURE;

            if (this.hasHome) {
                texture = POINT_HOME_TEXTURE;
            } else if (hovered) {
                texture = POINT_HOVERED_TEXTURE;
            }

            context.blit(texture, pointStartX, pointStartY, 0, 0, pointWidth, pointHeight, pointWidth, pointHeight);
        }
    }

    public static class Coordinates {
        public int x, y;

        public Coordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Coordinates latlonToPos(double lat, double lon, int width, int height) {
        int x = (int)(width * (((180.0 + lon) / 360.0 - zoomedAreaStartX) / zoomedAreaHeight));
        int y = (int)(height * (((90.0 - lat) / 180.0 - zoomedAreaStartY) / zoomedAreaWidth));
        return new Coordinates(x, y);
    }
}
