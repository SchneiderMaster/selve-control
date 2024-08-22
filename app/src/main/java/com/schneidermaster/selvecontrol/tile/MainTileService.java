package com.schneidermaster.selvecontrol.tile;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders.Timeline;
import androidx.wear.protolayout.material.Text;
import androidx.wear.protolayout.material.Typography;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.TileService;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MainTileService extends TileService {
    private static final String RESOURCES_VERSION = "1";

    @NonNull
    @Override
    protected ListenableFuture<Tile> onTileRequest(
            @NonNull RequestBuilders.TileRequest requestParams
    ) {
        return Futures.immediateFuture(new Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(
                        Timeline.fromLayoutElement(
                                new Text.Builder(this, "Hello world!")
                                        .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                                        .setColor(ColorBuilders.argb(0xFFFF0000))
                                        .build()))
                .build()
        );
    }

    @NonNull
    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
            @NonNull RequestBuilders.ResourcesRequest requestParams
    ) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        );
    }
}