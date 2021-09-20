package net.kunmc.lab;

import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.mca.Section;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

public class InvertYAxis {
    public static void main(String... args) {
        File mcaDirectory = new File(".");
        File[] allMca = Objects.requireNonNull(mcaDirectory.listFiles(e -> e.getName().endsWith(".mca")));
        if (allMca.length == 0) {
            System.err.println("No .mca files found");
            System.exit(1);
        }

        System.out.println("Conversion started.");

        Arrays.stream(allMca).parallel().forEach(x -> {
            try {
                convert(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void convert(File mcaFile) throws IOException {
        MCAFile mca = MCAUtil.read(mcaFile);

        for (int i = 0; i < 1024; i++) {
            Chunk chunk = mca.getChunk(i);
            if (chunk == null) {
                continue;
            }

            for (int j = 0; j < 8; j++) {
                Section higherSection = chunk.getSection(15 - j);
                if (higherSection == null) {
                    continue;
                }
                if (higherSection.getPalette() == null) {
                    continue;
                }

                Section lowerSection = chunk.getSection(j);
                if (lowerSection == null) {
                    continue;
                }
                if (lowerSection.getPalette() == null) {
                    continue;
                }

                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            CompoundTag higher = higherSection.getBlockStateAt(x, y, z);
                            CompoundTag lower = lowerSection.getBlockStateAt(x, 15 - y, z);

                            Tag tag = lower.get("Name");
                            if (tag != null) {
                                try {
                                    Field field = tag.getClass().getSuperclass().getDeclaredField("value");
                                    field.setAccessible(true);
                                    Object value = field.get(tag);
                                    if (value.equals("minecraft:bedrock")) {
                                        CompoundTag stone = new CompoundTag();
                                        stone.putString("Name", "minecraft:stone");
                                        lower = stone;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            higherSection.setBlockStateAt(x, y, z, lower, false);
                            lowerSection.setBlockStateAt(x, 15 - y, z, higher, false);
                        }
                    }
                }
            }

            mca.setChunk(i, chunk);
        }
        MCAUtil.write(mca, mcaFile);
    }
}
