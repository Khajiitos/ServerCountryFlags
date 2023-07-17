package me.khajiitos.servercountryflags.common.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.LocationInfo;
import me.khajiitos.servercountryflags.common.util.NetworkChangeDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
        super(new TranslatableComponent("servermap.title"));
        this.parent = parent;

        if (Config.showHomeOnMap && ServerCountryFlags.localLocation != null) {
            addPoint(null, ServerCountryFlags.localLocation);
        }

        for (Map.Entry<String, LocationInfo> entry : ServerCountryFlags.servers.entrySet()) {
            addPoint(entry.getKey(), entry.getValue());
        }
    }

    public Point getPoint(double lon, double lat) {
        for (Point point : points) {
            if (point.lon == lon && point.lat == lat) {
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
            point.addLocationInfo(name, locationInfo);
        } else {
            points.add(new Point(name, locationInfo));
        }
    }

    @Override
    public void init() {

        this.addRenderableWidget(new Button(
                this.width / 2 - 105, this.height - 26, 100, 20,
                new TranslatableComponent("selectServer.refresh"),
                (button) -> {
                    this.clearWidgets();
                    this.init();

                    if (ServerCountryFlags.serverList == null) {
                        return;
                    }

                    if (Config.reloadOnRefresh) {
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
        ));

        this.addRenderableWidget(new Button(
                this.width / 2 + 5, this.height - 26, 100, 20,
                new TranslatableComponent("gui.back"),
                (button) -> Minecraft.getInstance().setScreen(this.parent)
        ));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);
        Gui.drawCenteredString(poseStack, this.font, this.getTitle().getVisualOrderText(), this.width / 2, 12, 0xFFFFFFFF);
        Gui.fill(poseStack, 0, 32, this.width, this.height - 32, 0xAA000000);

        mapHeight = this.height - 64;
        mapWidth = (int)(mapHeight * MAP_TEXTURE_ASPECT);

        if (mapWidth > this.width) {
            mapWidth = this.width;
            mapHeight = (int)(mapWidth / MAP_TEXTURE_ASPECT);
        }

        mapStartX = this.width / 2 - mapWidth / 2;
        mapStartY = 32 + ((this.height - 64) / 2 - mapHeight / 2);

        RenderSystem.setShaderTexture(0, MAP_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        blit(poseStack, mapStartX, mapStartY, mapWidth, mapHeight, (float)(mapWidth * zoomedAreaStartX), (float)(mapHeight * zoomedAreaStartY), (int)(mapWidth * zoomedAreaWidth), (int)(mapHeight * zoomedAreaHeight), mapWidth, mapHeight);
        Point hoveredPoint = null;

        int pointHeight = mapHeight / 20;
        int pointWidth = (int)(pointHeight * POINT_TEXTURE_ASPECT);

        for (int i = points.size() - 1; i >= 0; i--) {
            Point point = points.get(i);
            Coordinates coords = latlonToPos(point.lat, point.lon, mapWidth, mapHeight);
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
            point.render(poseStack, hoveredPoint == point);
        }

        if (hoveredPoint != null) {
            this.renderTooltip(poseStack, hoveredPoint.getTooltip(), mouseX, mouseY);
        }

        RenderSystem.disableBlend();
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void tick() {
        super.tick();

        // is this stupid?
        boolean home = false;
        List<LocationInfo> alreadyUsedLocationInfos = new ArrayList<>();
        for (Point point : points) {
            if (point.hasHome)
                home = true;
            for (Point.NamedLocationInfo namedLocationInfo : point.locationInfos) {
                alreadyUsedLocationInfos.add(namedLocationInfo.locationInfo);
            }
        }

        for (Map.Entry<String, LocationInfo> entry : ServerCountryFlags.servers.entrySet()) {
            if (!alreadyUsedLocationInfos.contains(entry.getValue())) {
                addPoint(entry.getKey(), entry.getValue());
            }
        }

        if (!home && ServerCountryFlags.localLocation != null) {
            addPoint(null, ServerCountryFlags.localLocation);
        }
    }

    public class Point {
        public double lat, lon;
        List<NamedLocationInfo> locationInfos;
        public boolean hasHome;

        public Point(String beginningName, LocationInfo beginningLocationInfo) {
            locationInfos = new ArrayList<>();
            this.lat = beginningLocationInfo.latitude;
            this.lon = beginningLocationInfo.longitude;
            this.addLocationInfo(beginningName, beginningLocationInfo);
        }

        public void addLocationInfo(String name, LocationInfo info) {
            if (name == null) {
                this.hasHome = true;
            }
            locationInfos.add(new NamedLocationInfo(name, info));
        }

        public List<FormattedCharSequence> getTooltip() {
            List<FormattedCharSequence> list = new ArrayList<>();
            for (NamedLocationInfo info : locationInfos) {
                if (!list.isEmpty()) {
                    list.add(Component.nullToEmpty("").getVisualOrderText());
                }
                if (info.name == null) {
                    list.add(new TranslatableComponent("servermap.home").withStyle(ChatFormatting.BOLD).getVisualOrderText());
                } else {
                    list.add(new TextComponent(info.name).withStyle(ChatFormatting.BOLD).getVisualOrderText());
                    list.add(new TextComponent((Config.showDistrict && !info.locationInfo.districtName.equals("") ? (info.locationInfo.districtName + ", ") : "") + info.locationInfo.cityName + ", " + info.locationInfo.countryName).getVisualOrderText());
                }
            }
            return list;
        }

        private void render(PoseStack poseStack, boolean hovered) {
            Coordinates coords = latlonToPos(this.lat, this.lon, mapWidth, mapHeight);
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

            RenderSystem.setShaderTexture(0, texture);
            blit(poseStack, pointStartX, pointStartY, 0, 0, pointWidth, pointHeight, pointWidth, pointHeight);
        }

        public class NamedLocationInfo {
            public String name;
            public LocationInfo locationInfo;

            public NamedLocationInfo(String name, LocationInfo locationInfo) {
                this.name = name;
                this.locationInfo = locationInfo;
            }
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
