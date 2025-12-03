package colosseum.gitreverse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public final class Location {
    private double x;
    private double y;
    private double z;

    private static int locToBlock(double loc) {
        final int floor = (int) loc;
        return floor == loc ? floor : floor - (int) (Double.doubleToRawLongBits(loc) >>> 63);
    }

    public int getBlockX() {
        return locToBlock(this.x);
    }

    public int getBlockY() {
        return locToBlock(this.y);
    }

    public int getBlockZ() {
        return locToBlock(this.z);
    }
}
