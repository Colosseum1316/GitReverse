package colosseum.unparser;

import lombok.Getter;
import nl.rutgerkok.hammer.ChunkAccess;
import nl.rutgerkok.hammer.anvil.AnvilChunk;
import nl.rutgerkok.hammer.anvil.AnvilMaterialMap;
import nl.rutgerkok.hammer.anvil.AnvilWorld;
import nl.rutgerkok.hammer.anvil.tag.AnvilFormat;
import nl.rutgerkok.hammer.material.GlobalMaterialMap;
import nl.rutgerkok.hammer.tag.CompoundTag;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public final class World {
    private final Path directory;
    @Getter
    private final HashMap<Pair<Integer, Integer>, AnvilChunk> visitedChunks = new HashMap<>();
    private nl.rutgerkok.hammer.World offlineWorld;

    public World(Path directory) throws Exception {
        this.directory = directory;
        readOfflineWorld();
    }

    private void readOfflineWorld() throws Exception {
        this.offlineWorld = new AnvilWorld(new GlobalMaterialMap(), directory.resolve(AnvilWorld.LEVEL_DAT_NAME));
    }

    public ChunkAccess<AnvilChunk> getChunkAccess() throws Exception {
        return (ChunkAccess<AnvilChunk>) offlineWorld.getChunkAccess();
    }

    public AnvilChunk getChunk(ChunkAccess<AnvilChunk> chunkAccess, int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, AnvilChunk.CHUNK_X_SIZE);
        int chunkZ = Math.floorDiv(blockZ, AnvilChunk.CHUNK_Z_SIZE);
        return visitedChunks.computeIfAbsent(Pair.of(chunkX, chunkZ), p -> {
            try {
                return chunkAccess.getChunk(p.getLeft(), p.getRight());
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }

    public Block getBlock(Location location) throws Exception {
        try (ChunkAccess<AnvilChunk> chunkAccess = getChunkAccess()) {
            AnvilChunk chunk = getChunk(chunkAccess, location.getBlockX(), location.getBlockZ());
            return getBlock(chunk, location);
        }
    }

    public Block getBlock(AnvilChunk chunk, Location location) {
        return getBlock(chunk, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Block getBlock(AnvilChunk chunk, int blockX, int blockY, int blockZ) {
        char id = ((AnvilMaterialMap) offlineWorld.getGameFactory().getMaterialMap()).getOldMinecraftId(chunk.getMaterial(Math.floorMod(blockX, AnvilChunk.CHUNK_X_SIZE), blockY, Math.floorMod(blockZ, AnvilChunk.CHUNK_Z_SIZE)));
        short typeId = (short) (id >> 4);
        byte data = (byte) (id & 0xF);
        return new Block(typeId, data, blockX, blockY, blockZ);
    }

    public void setWool(Block block) throws Exception {
        setWool(block, (byte) 0);
    }

    public void setWool(Block block, byte data) throws Exception {
        setBlock(block, Util.MATERIAL_WOOL, data);
    }

    public void addIronPlate(Block baseBlock) throws Exception {
        setBlock(getBlock(new Location(baseBlock.getX(), baseBlock.getY() + 1, baseBlock.getZ())), Util.MATERIAL_IRON_PLATE, (byte) 0);
    }

    public void addGoldPlate(Block baseBlock) throws Exception {
        setBlock(getBlock(new Location(baseBlock.getX(), baseBlock.getY() + 1, baseBlock.getZ())), Util.MATERIAL_GOLD_PLATE, (byte) 0);
    }

    public void setBlock(Block block, short newTypeId, byte newData) throws Exception {
        block.setId(newTypeId);
        block.setData(newData);
        setBlock(block);
    }

    public void setBlock(Block block) throws Exception {
        setBlock(block.getX(), block.getY(), block.getZ(), block.getId(), block.getData());
    }

    public void addAndSetSign(Location baseBlockLocation, String[] lines) throws Exception {
        Validate.notEmpty(lines);
        try (ChunkAccess<AnvilChunk> chunkAccess = getChunkAccess()) {
            AnvilChunk chunk = getChunk(chunkAccess, baseBlockLocation.getBlockX(), baseBlockLocation.getBlockZ());
            Block sign = getBlock(chunk, baseBlockLocation.getBlockX(), baseBlockLocation.getBlockY() + 1, baseBlockLocation.getBlockZ());
            CompoundTag signTag = new CompoundTag();
            signTag.setString(AnvilFormat.TileEntityTag.ID, "Sign");
            signTag.setInt(AnvilFormat.TileEntityTag.X_POS, sign.getX());
            signTag.setInt(AnvilFormat.TileEntityTag.Y_POS, sign.getY());
            signTag.setInt(AnvilFormat.TileEntityTag.Z_POS, sign.getZ());
            for (int i = 0; i < lines.length; i++) {
                signTag.setString(AnvilFormat.TileEntityTag.SIGN_LINE_NAMES.get(i), lines[i] == null ? "" : lines[i]);
            }
            chunk.getTileEntities().add(signTag);
            setBlock(sign, Util.MATERIAL_SIGN_POST, (byte) 0);
        }
    }

    private void setBlock(int blockX, int blockY, int blockZ, short typeId, byte data) throws Exception {
        try (ChunkAccess<AnvilChunk> chunkAccess = getChunkAccess()) {
            AnvilChunk chunk = getChunk(chunkAccess, blockX, blockZ);
            chunk.setMaterial(Math.floorMod(blockX, AnvilChunk.CHUNK_X_SIZE), blockY, Math.floorMod(blockZ, AnvilChunk.CHUNK_Z_SIZE), ((AnvilMaterialMap) offlineWorld.getGameFactory().getMaterialMap()).getMaterialDataFromOldIds(typeId, data));
        }
    }
}
