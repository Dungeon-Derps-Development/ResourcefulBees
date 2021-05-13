package com.resourcefulbees.resourcefulbees.init;

import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.item.dispenser.ScraperDispenserBehavior;
import com.resourcefulbees.resourcefulbees.item.dispenser.ShearsDispenserBehavior;
import com.resourcefulbees.resourcefulbees.mixin.DispenserBlockInvoker;
import com.resourcefulbees.resourcefulbees.registry.ModItems;
import com.resourcefulbees.resourcefulbees.utils.color.RainbowColor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.resourcefulbees.resourcefulbees.ResourcefulBees.LOGGER;

public class ModSetup {

    private ModSetup() {
        throw new IllegalStateException("Utility Class");
    }

    public static void initialize() {
        setupPaths();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ModSetup::loadResources);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RainbowColor::init);
    }

    private static void setupPaths() {
        LOGGER.info("Setting up config paths...");
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path rbBeesPath = Paths.get(configPath.toAbsolutePath().toString(), ResourcefulBees.MOD_ID, "bees");
        Path rbBiomePath = Paths.get(configPath.toAbsolutePath().toString(), ResourcefulBees.MOD_ID, "biome_dictionary");
        Path rbTraitPath = Paths.get(configPath.toAbsolutePath().toString(), ResourcefulBees.MOD_ID, "bee_traits");
        Path rbAssetsPath = Paths.get(configPath.toAbsolutePath().toString(),ResourcefulBees.MOD_ID, "resources");
        Path rbHoneyPath = Paths.get(configPath.toAbsolutePath().toString(), ResourcefulBees.MOD_ID, "honey");
        BeeSetup.setBeePath(rbBeesPath);
        BeeSetup.setResourcePath(rbAssetsPath);
        BeeSetup.setHoneyPath(rbHoneyPath);
        BiomeDictionarySetup.setDictionaryPath(rbBiomePath);
        TraitSetup.setDictionaryPath(rbTraitPath);

        try { Files.createDirectories(rbBeesPath);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("failed to create \"bees\" directory");}

        try { Files.createDirectories(rbBiomePath);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("failed to create \"biome_dictionary\" directory");}

        try { Files.createDirectories(rbTraitPath);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("failed to create \"bee_traits\" directory");}

        try { Files.createDirectory(rbAssetsPath);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("Failed to create \"assets\" directory");}

        try { Files.createDirectories(rbHoneyPath);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("failed to create \"honey\" directory");}

        try (FileWriter file = new FileWriter(Paths.get(rbAssetsPath.toAbsolutePath().toString(), "pack.mcmeta").toFile())) {
            String mcMetaContent = "{\"pack\":{\"pack_format\":6,\"description\":\"Resourceful Bees resource pack used for lang purposes for the user to add lang for bee/items.\"}}";
            file.write(mcMetaContent);
        } catch (FileAlreadyExistsException ignored) { //ignored
        } catch (IOException e) { LOGGER.error("Failed to create pack.mcmeta file for resource loading");}
    }

    public static void registerDispenserBehaviors() {
        ShearsDispenserBehavior.setDefaultShearsDispenseBehavior(((DispenserBlockInvoker) Blocks.DISPENSER).invokeGetBehavior(new ItemStack(Items.SHEARS)));

        DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenserBehavior());

        DispenserBlock.registerBehavior(ModItems.SCRAPER.get().asItem(), new ScraperDispenserBehavior());
    }

    public static void loadResources() {
        Minecraft.getInstance().getResourcePackRepository().addPackFinder((consumer, factory) -> {
            final Pack packInfo = Pack.create(
                    ResourcefulBees.MOD_ID,
                    true,
                    () -> new FolderPackResources(BeeSetup.getResourcePath().toFile()) {
                        @Override
                        public boolean isHidden() {
                            return true;
                        }
                    },
                    factory,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (packInfo == null) {
                LOGGER.error("Failed to load resource pack, some things may not work.");
                return;
            }
            consumer.accept(packInfo);
        });
    }

    public static void parseType(File file, BiConsumer<Reader, String> consumer) throws IOException {
        String name = file.getName();
        name = name.substring(0, name.indexOf('.'));

        Reader r = Files.newBufferedReader(file.toPath());

        consumer.accept(r, name);
    }

    public static void parseType(ZipFile zf, ZipEntry zipEntry, BiConsumer<Reader, String> consumer) throws IOException {
        String name = zipEntry.getName();
        name = name.substring(name.lastIndexOf("/") + 1, name.indexOf('.'));

        InputStream input = zf.getInputStream(zipEntry);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        consumer.accept(reader, name);
    }
}