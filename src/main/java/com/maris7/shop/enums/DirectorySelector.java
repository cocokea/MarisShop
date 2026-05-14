package com.maris7.shop.enums;

import com.maris7.shop.MarisShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URL;

public enum DirectorySelector {

    CATEGORIES("categories");

    private final String directoryName;

    DirectorySelector(String directoryName) {
        this.directoryName = directoryName;
    }

    /**
     * Khởi tạo thư mục và copy file default từ jar (chỉ copy nếu chưa tồn tại).
     * KHÔNG xóa hay ghi đè file người dùng đã tạo.
     */
    public void initialize() {
        JavaPlugin plugin = MarisShop.getInstance();
        File dir = new File(plugin.getDataFolder(), directoryName);
        // Chỉ copy defaults khi thư mục chưa tồn tại (lần cài đầu tiên).
        // Nếu thư mục đã có rồi → không đụng vào, admin toàn quyền quản lý file.
        if (!dir.exists()) {
            dir.mkdirs();
            copyDefaultsFromJar(plugin, dir);
        }
    }

    /**
     * Copy default yml từ jar sang thư mục plugin.
     * Chỉ copy nếu file CHƯA tồn tại — không bao giờ ghi đè file người dùng.
     */
    private void copyDefaultsFromJar(JavaPlugin plugin, File targetDir) {
        try {
            URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            try (JarFile jar = new JarFile(jarUrl.getFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(directoryName + "/") && name.endsWith(".yml")) {
                        // Chỉ lấy phần filename, bỏ prefix thư mục
                        String fileName = name.substring(directoryName.length() + 1);
                        if (fileName.isEmpty() || fileName.contains("/")) continue; // bỏ qua subfolder
                        File outFile = new File(targetDir, fileName);
                        if (!outFile.exists()) {
                            try (InputStream in = plugin.getResource(name)) {
                                if (in != null) Files.copy(in, outFile.toPath());
                            } catch (IOException e) {
                                plugin.getLogger().warning("Failed to copy default file: " + name);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to access plugin jar for default files: " + e.getMessage());
        }
    }

    /**
     * Trả về TẤT CẢ file .yml trong thư mục — bao gồm cả file người dùng tạo thêm.
     * Đây là nguồn dữ liệu cho ShopManager.load().
     */
    public List<File> getAllYamlFiles() {
        File dir = new File(MarisShop.getInstance().getDataFolder(), directoryName);
        List<File> files = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] found = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (found != null) {
                for (File f : found) files.add(f);
            }
        }
        return files;
    }

    public String getDirectoryName() { return directoryName; }
}
