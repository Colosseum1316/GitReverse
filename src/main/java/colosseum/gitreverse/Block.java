package colosseum.gitreverse;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("FieldMayBeFinal")
public final class Block {
    @Getter
    @Setter
    private short id;
    @Getter
    @Setter
    private byte data;
    private Location location;

    public Block(short id, byte data, int x, int y, int z) {
        this.id = id;
        this.data = data;
        this.location = new Location(x, y, z);
    }

    public int getX() {
        return location.getBlockX();
    }

    public int getY() {
        return location.getBlockY();
    }

    public int getZ() {
        return location.getBlockZ();
    }
}
