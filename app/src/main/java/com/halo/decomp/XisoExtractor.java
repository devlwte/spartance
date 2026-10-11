package com.halo.decomp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies the maps folder out of an Xbox disc image (an "xiso", or a whole
 * disc's ".iso"), as the desktop games do (port/linux/src/xiso.c).
 *
 * Enhanced for Spartan CE:
 * Supports multi-language Xbox ISOs containing localized map directories
 * (e.g. maps_es for Spanish, maps_it for Italian, maps for English/default).
 * Detects all map candidates, extracts the selected language folder
 * directly into destination/maps/ so the game engine loads the chosen language natively.
 */
public final class XisoExtractor {
    private static final int SECTOR_SIZE = 2048;
    private static final long VOLUME_DESCRIPTOR_OFFSET = 0x10000;
    private static final int ENTRY_HEADER_SIZE = 14;
    private static final int ATTRIBUTE_DIRECTORY = 0x10;
    private static final long MAXIMUM_DIRECTORY_SIZE = 4 << 20;
    private static final int MAXIMUM_FILES = 512;
    private static final int COPY_BUFFER_SIZE = 1 << 20;
    private static final byte[] VOLUME_MAGIC = "MICROSOFT*XBOX*MEDIA".getBytes(StandardCharsets.US_ASCII);

    private static final long[] PARTITION_OFFSETS = { 0, 0x0FD90000L, 0x02080000L, 0x18300000L };

    public interface Progress {
        void report(String file, long done, long total);
    }

    public static final class ExtractException extends IOException {
        public ExtractException(String message) {
            super(message);
        }
    }

    public static final class Entry {
        public final String name;
        public final long sector;
        public final long size;

        Entry(String name, long sector, long size) {
            this.name = name;
            this.sector = sector;
            this.size = size;
        }
    }

    public static final class LanguageEntry {
        public final String folderName;
        public final String displayName;
        public final Entry rootEntry;
        public final List<Entry> files;
        public final long totalSize;

        public LanguageEntry(String folderName, String displayName, Entry rootEntry, List<Entry> files, long totalSize) {
            this.folderName = folderName;
            this.displayName = displayName;
            this.rootEntry = rootEntry;
            this.files = files;
            this.totalSize = totalSize;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final FileChannel image;
    private long partition;

    private XisoExtractor(FileChannel image) {
        this.image = image;
    }

    private void readAt(long offset, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int count = image.read(buffer, offset);
            if (count <= 0)
                throw new ExtractException("No se pudo leer la imagen del disco (¿está completa?).");
            offset += count;
        }
        buffer.flip();
    }

    private static boolean magicAt(ByteBuffer buffer, int offset) {
        for (int index = 0; index < VOLUME_MAGIC.length; index++) {
            if (buffer.get(offset + index) != VOLUME_MAGIC[index])
                return false;
        }
        return true;
    }

    private long[] findVolume() throws IOException {
        for (long offset : PARTITION_OFFSETS) {
            ByteBuffer descriptor = ByteBuffer.allocate(SECTOR_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            try {
                readAt(offset + VOLUME_DESCRIPTOR_OFFSET, descriptor);
            } catch (IOException e) {
                continue;
            }
            if (!magicAt(descriptor, 0) || !magicAt(descriptor, 0x7EC))
                continue;
            partition = offset;
            return new long[] {
                descriptor.getInt(20) & 0xFFFFFFFFL, descriptor.getInt(24) & 0xFFFFFFFFL,
            };
        }
        throw new ExtractException("Este archivo no es una imagen de disco válida de Xbox (.iso / .xiso).");
    }

    private ByteBuffer readDirectory(long sector, long size, String what) throws IOException {
        if (size <= 0 || size > MAXIMUM_DIRECTORY_SIZE)
            throw new ExtractException(what);
        ByteBuffer table = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);
        readAt(partition + sector * SECTOR_SIZE, table);
        return table;
    }

    private static void walk(ByteBuffer table, long offset, int depth, boolean directories, List<Entry> out,
        int[] visited) {
        offset *= 4;
        if (depth > 64 || ++visited[0] > 4096 || offset + ENTRY_HEADER_SIZE > table.limit())
            return;
        int at = (int) offset;
        int left = table.getShort(at) & 0xFFFF;
        int right = table.getShort(at + 2) & 0xFFFF;
        if (left == 0xFFFF)
            return;
        int nameLength = table.get(at + 13) & 0xFF;
        if (left != 0)
            walk(table, left, depth + 1, directories, out, visited);
        boolean isDirectory = (table.get(at + 12) & ATTRIBUTE_DIRECTORY) != 0;
        if (at + ENTRY_HEADER_SIZE + nameLength <= table.limit() && nameLength > 0 && isDirectory == directories
            && out.size() < MAXIMUM_FILES) {
            byte[] bytes = new byte[nameLength];
            for (int index = 0; index < nameLength; index++)
                bytes[index] = table.get(at + ENTRY_HEADER_SIZE + index);
            String name = new String(bytes, StandardCharsets.ISO_8859_1);
            if (!name.equals(".") && !name.equals("..") && name.indexOf('/') < 0 && name.indexOf('\\') < 0)
                out.add(new Entry(name, table.getInt(at + 4) & 0xFFFFFFFFL, table.getInt(at + 8) & 0xFFFFFFFFL));
        }
        if (right != 0)
            walk(table, right, depth + 1, directories, out, visited);
    }

    public static String getLanguageDisplayName(String folderName) {
        String lower = folderName.toLowerCase();
        if (lower.equals("maps_es") || lower.contains("_es") || lower.contains("spanish")) {
            return "Español (Audio y Textos en Español)";
        } else if (lower.equals("maps_it") || lower.contains("_it") || lower.contains("italian")) {
            return "Italiano (Audio e Testo in Italiano)";
        } else if (lower.equals("maps_fr") || lower.contains("_fr") || lower.contains("french")) {
            return "Français (Audio et Textes en Français)";
        } else if (lower.equals("maps_de") || lower.contains("_de") || lower.contains("german")) {
            return "Deutsch (Audio und Texte auf Deutsch)";
        } else if (lower.equals("maps_en") || lower.contains("_en") || lower.contains("english")) {
            return "English (Audio and Text in English)";
        } else if (lower.equals("maps")) {
            return "Predeterminado / Inglés (maps)";
        } else {
            return folderName;
        }
    }

    /**
     * Inspects the disc image and lists all available map folders (languages) containing ui.map.
     */
    public static List<LanguageEntry> listLanguageEntries(FileChannel image) throws IOException {
        XisoExtractor extractor = new XisoExtractor(image);
        long[] root = extractor.findVolume();

        ByteBuffer rootTable = extractor.readDirectory(root[0], root[1], "El sistema de archivos de la imagen está dañado.");
        List<Entry> directories = new ArrayList<>();
        walk(rootTable, 0, 0, true, directories, new int[1]);

        List<LanguageEntry> languages = new ArrayList<>();

        for (Entry dir : directories) {
            String nameLower = dir.name.toLowerCase();
            if (nameLower.equals("maps") || nameLower.startsWith("maps_") || nameLower.contains("map")) {
                try {
                    ByteBuffer dirTable = extractor.readDirectory(dir.sector, dir.size, "Error leyendo carpeta " + dir.name);
                    List<Entry> files = new ArrayList<>();
                    walk(dirTable, 0, 0, false, files, new int[1]);
                    boolean hasUi = false;
                    long totalSize = 0;
                    for (Entry file : files) {
                        totalSize += file.size;
                        if (file.name.equalsIgnoreCase("ui.map")) {
                            hasUi = true;
                        }
                    }
                    if (hasUi) {
                        String display = getLanguageDisplayName(dir.name);
                        languages.add(new LanguageEntry(dir.name, display, dir, files, totalSize));
                    }
                } catch (Exception ignored) {}
            }
        }

        // If no maps found by name pattern, check all directories for ui.map
        if (languages.isEmpty()) {
            for (Entry dir : directories) {
                try {
                    ByteBuffer dirTable = extractor.readDirectory(dir.sector, dir.size, "Error leyendo carpeta " + dir.name);
                    List<Entry> files = new ArrayList<>();
                    walk(dirTable, 0, 0, false, files, new int[1]);
                    boolean hasUi = false;
                    long totalSize = 0;
                    for (Entry file : files) {
                        totalSize += file.size;
                        if (file.name.equalsIgnoreCase("ui.map")) {
                            hasUi = true;
                        }
                    }
                    if (hasUi) {
                        languages.add(new LanguageEntry(dir.name, dir.name, dir, files, totalSize));
                    }
                } catch (Exception ignored) {}
            }
        }

        return languages;
    }

    /**
     * Extracts the chosen language maps folder into destination/maps.
     */
    public static void extractMaps(FileChannel image, LanguageEntry language, File destination, Progress progress) throws IOException {
        XisoExtractor extractor = new XisoExtractor(image);
        extractor.findVolume();
        extractor.extractLanguage(language, destination, progress);
    }

    /**
     * Backward-compatible default extraction: picks Spanish if available, else first detected maps folder.
     */
    public static void extractMaps(FileChannel image, File destination, Progress progress) throws IOException {
        List<LanguageEntry> languages = listLanguageEntries(image);
        if (languages.isEmpty())
            throw new ExtractException("La imagen ISO no contiene ninguna carpeta de datos compatible con ui.map.");

        LanguageEntry chosen = languages.get(0);
        for (LanguageEntry lang : languages) {
            if (lang.folderName.equalsIgnoreCase("maps_es") || lang.displayName.toLowerCase().contains("español")) {
                chosen = lang;
                break;
            }
        }
        extractMaps(image, chosen, destination, progress);
    }

    private void extractLanguage(LanguageEntry language, File destination, Progress progress) throws IOException {
        List<Entry> files = language.files;
        long total = language.totalSize;

        File partial = new File(destination, "maps.partial");
        File finished = new File(destination, "maps");

        if (partial.exists()) {
            deleteRecursive(partial);
        }
        if (!partial.isDirectory() && !partial.mkdirs())
            throw new ExtractException("No se pudo crear el directorio temporal " + partial + ".");

        ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_SIZE);
        long done = 0;

        for (Entry file : files) {
            File path = new File(partial, file.name);
            long offset = partition + file.sector * SECTOR_SIZE;
            long remaining = file.size;

            try (FileOutputStream out = new FileOutputStream(path)) {
                FileChannel output = out.getChannel();
                while (remaining > 0) {
                    buffer.clear();
                    buffer.limit((int) Math.min(remaining, COPY_BUFFER_SIZE));
                    readAt(offset, buffer);
                    int count = buffer.remaining();
                    while (buffer.hasRemaining())
                        output.write(buffer);
                    offset += count;
                    remaining -= count;
                    done += count;
                    if (progress != null) {
                        progress.report(file.name, done, total);
                    }
                }
            } catch (ExtractException e) {
                throw new ExtractException("No se pudo leer " + file.name + " desde el ISO (¿está dañado?).");
            } catch (IOException e) {
                throw new ExtractException("No se pudo escribir " + path + " (¿almacenamiento lleno?).");
            }
        }

        if (!finished.isDirectory() && !finished.mkdirs())
            throw new ExtractException("No se pudo crear el directorio final " + finished + ".");

        for (Entry file : files) {
            File from = new File(partial, file.name);
            File to = new File(finished, file.name);
            if (to.exists()) {
                to.delete();
            }
            if (!from.renameTo(to))
                throw new ExtractException("No se pudo mover " + file.name + " a " + finished + ".");
        }
        partial.delete();
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children)
                    deleteRecursive(child);
            }
        }
        file.delete();
    }
}
