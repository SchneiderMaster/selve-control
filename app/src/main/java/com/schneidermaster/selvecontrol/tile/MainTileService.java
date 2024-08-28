package com.schneidermaster.selvecontrol.tile;

import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.protolayout.material.Text;
import androidx.wear.protolayout.material.Typography;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.schneidermaster.selvecontrol.R;

public class MainTileService extends TileService {

    private static final String RESOURCES_VERSION = "1";

    @NonNull
    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(@NonNull RequestBuilders.TileRequest requestParams) {


        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(getPackageName(), "com.schneidermaster.selvecontrol.presentation.MainActivity"));

        LayoutElementBuilders.Box box = new LayoutElementBuilders.Box.Builder()
                .addContent(new Text.Builder(this, getResources().getString(R.string.tile_text))
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .build())
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(new ModifiersBuilders.Clickable.Builder()
                                .setOnClick(new ActionBuilders.LaunchAction.Builder()
                                        .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                                                .setPackageName(getPackageName())
                                                .setClassName("com.schneidermaster.selvecontrol.presentation.MainActivity")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        return Futures.immediateFuture(new TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION).setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(box)).build());
    }

    @NonNull
    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(@NonNull RequestBuilders.ResourcesRequest requestParams) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build());
    }
}
