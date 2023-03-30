package me.khajiitos.servercountryflags;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerMapScreen extends Screen {
    public static final Identifier MAP_TEXTURE = new Identifier(ServerCountryFlags.MOD_ID, "textures/misc/map.png");
    public static final Identifier POINT_TEXTURE = new Identifier(ServerCountryFlags.MOD_ID, "textures/misc/point.png");
    public static final Identifier POINT_HOVERED_TEXTURE = new Identifier(ServerCountryFlags.MOD_ID, "textures/misc/point_hovered.png");
    public static final Identifier POINT_HOME_TEXTURE = new Identifier(ServerCountryFlags.MOD_ID, "textures/misc/point_home.png");
    public static final double MAP_TEXTURE_ASPECT = 1920.0 / 960.0;
    public static final double POINT_TEXTURE_ASPECT = 526.0 / 754.0;

    public final Screen parent;
    public ArrayList<Point> points = new ArrayList<>();

    public ServerMapScreen(Screen parent) {
        super(Text.literal("Server Map"));
        this.parent = parent;
    }

    public Point getPoint(double lon, double lat) {
        for (Point point : points) {
            if (point.lon == lon && point.lat == lat) {
                return point;
            }
        }
        return null;
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
        if (Config.showHomeOnMap && ServerCountryFlags.localLocation != null) {
            addPoint(null, ServerCountryFlags.localLocation);
        }

        for (Map.Entry<String, LocationInfo> entry : ServerCountryFlags.servers.entrySet()) {
            addPoint(entry.getKey(), entry.getValue());
        }

        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("selectServer.refresh"), (button) -> {
            if (ServerCountryFlags.serverList == null)
                return;

            if (Config.reloadOnRefresh) {
                points.clear();
                ServerCountryFlags.servers.clear();
            }

            for (int i = 0; i < ServerCountryFlags.serverList.size(); i++) {
                if (ServerCountryFlags.servers.containsKey(ServerCountryFlags.serverList.get(i).address))
                    continue;
                ServerCountryFlags.updateServerLocationInfo(ServerCountryFlags.serverList.get(i).address);
            }
        }).dimensions(this.width / 2 - 105, this.height - 26, 100, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.back"), (button) -> {
            MinecraftClient.getInstance().setScreen(this.parent);
        }).dimensions(this.width / 2 + 5, this.height - 26, 100, 20).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer, Text.translatable("servermap.title"), this.width / 2, 12, 0xFFFFFFFF);
        DrawableHelper.fill(matrices, 0, 32, this.width, this.height - 32, 0xAA000000);

        int height = this.height - 64;
        int width = (int)(height * MAP_TEXTURE_ASPECT);

        if (width > this.width) {
            width = this.width;
            height = (int)(width / MAP_TEXTURE_ASPECT);
        }

        int startX = this.width / 2 - width / 2;
        int startY = 32 + ((this.height - 64) / 2 - height / 2);

        RenderSystem.setShaderTexture(0, MAP_TEXTURE);
        RenderSystem.enableBlend();
        DrawableHelper.drawTexture(matrices, startX, startY, 0, 0, width, height, width, height);

        Point hoveredPoint = null;

        for (int i = points.size() - 1; i >= 0; i--) {
            Point point = points.get(i);
            Vector2i coords = latlonToPos(point.lat, point.lon, width, height);
            int pointHeight = height / 20;
            int pointWidth = (int)(pointHeight * POINT_TEXTURE_ASPECT);
            int pointStartX = startX + coords.x - (pointWidth / 2);
            int pointStartY = startY + coords.y - pointHeight;

            if (mouseX >= pointStartX && mouseX <= pointStartX + pointWidth && mouseY >= pointStartY && mouseY <= pointStartY + pointHeight) {
                hoveredPoint = point;
                break;
            }
        }

        for (Point point : this.points) {
            point.render(matrices, startX, startY, width, height, hoveredPoint == point);
        }

        if (hoveredPoint != null) {
            this.setTooltip(hoveredPoint.getTooltip());
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public void tick() {
        super.tick();

        // is this stupid?
        List<LocationInfo> alreadyUsedLocationInfos = new ArrayList<>();
        for (Point point : points) {
            for (Point.NamedLocationInfo namedLocationInfo : point.locationInfos) {
                alreadyUsedLocationInfos.add(namedLocationInfo.locationInfo);
            }
        }

        for (Map.Entry<String, LocationInfo> entry : ServerCountryFlags.servers.entrySet()) {
            if (!alreadyUsedLocationInfos.contains(entry.getValue())) {
                addPoint(entry.getKey(), entry.getValue());
            }
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

        public List<OrderedText> getTooltip() {
            List<OrderedText> list = new ArrayList<>();
            for (NamedLocationInfo info : locationInfos) {
                if (!list.isEmpty()) {
                    list.add(OrderedText.EMPTY);
                }
                if (info.name == null) {
                    list.add(Text.translatable("servermap.home").formatted(Formatting.BOLD).asOrderedText());
                } else {
                    list.add(Text.literal(info.name).formatted(Formatting.BOLD).asOrderedText());
                    list.add(Text.literal((Config.showDistrict && !info.locationInfo.districtName.equals("") ? (info.locationInfo.districtName + ", ") : "") + info.locationInfo.cityName + ", " + info.locationInfo.countryName).asOrderedText());
                }
            }
            return list;
        }

        private boolean render(MatrixStack matrices, int mapStartX, int mapStartY, int mapWidth, int mapHeight, boolean hovered) {
            Vector2i coords = latlonToPos(this.lat, this.lon, mapWidth, mapHeight);
            int pointHeight = mapHeight / 20;
            int pointWidth = (int)(pointHeight * POINT_TEXTURE_ASPECT);
            int pointStartX = mapStartX + coords.x - (pointWidth / 2);
            int pointStartY = mapStartY + coords.y - pointHeight;

            Identifier texture = POINT_TEXTURE;

            if (this.hasHome) {
                texture = POINT_HOME_TEXTURE;
            } else if (hovered) {
                texture = POINT_HOVERED_TEXTURE;
            }

            RenderSystem.setShaderTexture(0, texture);
            DrawableHelper.drawTexture(matrices, pointStartX, pointStartY, 0, 0, pointWidth, pointHeight, pointWidth, pointHeight);

            return hovered;
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

    private Vector2i latlonToPos(double lat, double lon, int width, int height) {
        int x = (int)(width * (180.0 + lon) / 360.0);
        int y = (int)(height * (90.0 - lat) / 180.0);
        return new Vector2i(x, y);
    }
}
