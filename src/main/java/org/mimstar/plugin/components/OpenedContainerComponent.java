package org.mimstar.plugin.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class OpenedContainerComponent implements Component<EntityStore> {

    private int x, y, z;

    public OpenedContainerComponent() {
    }

    public OpenedContainerComponent(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public OpenedContainerComponent(OpenedContainerComponent other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new OpenedContainerComponent(this);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
