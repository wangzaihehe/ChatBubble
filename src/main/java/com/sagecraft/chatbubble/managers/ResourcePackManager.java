package com.sagecraft.chatbubble.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.utils.CharacterUtils;
import com.sagecraft.chatbubble.utils.ZipUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ResourcePackManager {
    
    private final ChatBubblePlugin plugin;
    private final Logger logger;
    private final String namespace = "chatbubble";
    private final String fontPath = "font/";
    
    // 缓存机制
    private final Map<String, String> imageToUnicode = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> fontProviderCache = new ConcurrentHashMap<>();
    private final Set<String> processedImages = ConcurrentHashMap.newKeySet();
    
    private int unicodeCounter = 0;
    
    public ResourcePackManager(ChatBubblePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void generate() {
        logger.info("开始生成材质包...");
        
        // 清理缓存
        imageToUnicode.clear();
        fontProviderCache.clear();
        processedImages.clear();
        unicodeCounter = 0;
        
        File resourcePackFolder = new File(plugin.getDataFolder(), "ResourcePack");
        
        // 删除旧的资源包
        if (resourcePackFolder.exists()) {
            deleteDirectory(resourcePackFolder);
        }
        
        // 创建文件夹结构
        File assetsFolder = new File(resourcePackFolder, "assets" + File.separator + namespace);
        File fontFolder = new File(assetsFolder, "font");
        File texturesFolder = new File(assetsFolder, "textures" + File.separator + fontPath.replace("/", File.separator));
        
        if (!fontFolder.mkdirs() || !texturesFolder.mkdirs()) {
            logger.severe("创建材质包文件夹失败");
            return;
        }
        
        // 生成字体JSON
        JsonObject fontJson = new JsonObject();
        JsonArray providers = new JsonArray();
        
        // 获取气泡字符
        List<JsonObject> bubbleCharacters = getBubbleCharacters(texturesFolder);
        for (JsonObject provider : bubbleCharacters) {
            providers.add(provider);
        }
        
        fontJson.add("providers", providers);
        
        // 保存字体文件
        saveFont(fontJson);
        
        // 设置pack.mcmeta
        setPackFormat();
        
        // 生成zip文件
        generateZipFile(resourcePackFolder);
        
        logger.info("材质包生成完成！");
    }
    
    private void generateZipFile(File resourcePackFolder) {
        if (!plugin.getConfigManager().isGenerateZip()) {
            logger.info("zip生成已禁用，跳过zip文件生成");
            return;
        }

        try {
            Path resourcePackPath = resourcePackFolder.toPath();
            String zipFilename = plugin.getConfigManager().getZipFilename();
            Path zipFilePath = plugin.getDataFolder().toPath().resolve(zipFilename);

            ZipUtils.zipDirectory(resourcePackPath, zipFilePath);

            logger.info("材质包zip文件生成成功: " + zipFilePath.toAbsolutePath());

            if (!plugin.getConfigManager().isKeepFolder()) {
                deleteDirectory(resourcePackFolder);
                logger.info("已删除ResourcePack文件夹");
            }
        } catch (IOException e) {
            logger.severe("生成材质包zip文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private List<JsonObject> getBubbleCharacters(File texturesFolder) {
        List<JsonObject> list = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection fontImages = config.getConfigurationSection("font_images");
        
        if (fontImages == null) {
            logger.warning("未找到font_images配置");
            return list;
        }
        
        for (String key : fontImages.getKeys(false)) {
            ConfigurationSection imageConfig = fontImages.getConfigurationSection(key);
            if (imageConfig == null) continue;
            
            String path = imageConfig.getString("path");
            int scaleRatio = imageConfig.getInt("scale_ratio", 13);
            int yPosition = imageConfig.getInt("y_position", 8);
            
            if (path == null) continue;
            
            // 检查缓存
            String cacheKey = key + "_" + scaleRatio + "_" + yPosition;
            if (fontProviderCache.containsKey(cacheKey)) {
                list.add(fontProviderCache.get(cacheKey));
                continue;
            }
            
            // 获取或分配Unicode字符
            String unicode = imageToUnicode.computeIfAbsent(key, k -> {
                String unicodeChar = CharacterUtils.getUnicodeString(unicodeCounter++);
                logger.info("为图片 " + key + " 分配Unicode字符: " + unicodeChar);
                return unicodeChar;
            });
            
            // 创建JSON对象
            JsonObject jo = new JsonObject();
            jo.add("type", new JsonPrimitive("bitmap"));
            jo.add("file", new JsonPrimitive(namespace + ":" + fontPath + path));
            jo.add("ascent", new JsonPrimitive(yPosition));
            jo.add("height", new JsonPrimitive(scaleRatio));
            
            JsonArray chars = new JsonArray();
            chars.add(unicode);
            jo.add("chars", chars);
            
            // 缓存字体提供者
            fontProviderCache.put(cacheKey, jo);
            list.add(jo);
            
            // 从jar文件复制图片文件
            try {
                String resourcePath = "textures/" + path;
                File targetFile = new File(texturesFolder, fontPath.replace("/", File.separator) + path);
                
                // 确保目标文件夹存在
                targetFile.getParentFile().mkdirs();
                
                // 从jar文件复制
                if (copyResourceFromJar(resourcePath, targetFile)) {
                    logger.info("复制图片文件: " + path);
                    processedImages.add(key);
                } else {
                    logger.warning("图片文件不存在于jar中: " + resourcePath);
                }
            } catch (IOException e) {
                logger.severe("复制图片文件失败: " + path + " - " + e.getMessage());
            }
        }
        
        return list;
    }
    
    private boolean copyResourceFromJar(String resourcePath, File targetFile) throws IOException {
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                return false;
            }
            
            try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return true;
        }
    }
    
    private void saveFont(JsonObject fontJson) {
        try (FileWriter fileWriter = new FileWriter(
                plugin.getDataFolder() +
                        File.separator + "ResourcePack" +
                        File.separator + "assets" +
                        File.separator + namespace +
                        File.separator + "font" +
                        File.separator + "default.json")) {
            fileWriter.write(fontJson.toString());
        } catch (IOException e) {
            logger.severe("保存字体文件失败: " + e.getMessage());
        }
    }
    
    private void setPackFormat() {
        // 创建pack.mcmeta文件
        String packMcmeta = """
            {
                "pack": {
                    "pack_format": %d,
                    "description": "%s"
                }
            }
            """.formatted(
                plugin.getConfigManager().getPackFormat(),
                plugin.getConfigManager().getPackDescription()
            );
        
        try (FileWriter writer = new FileWriter(
                new File(plugin.getDataFolder(), "ResourcePack" + File.separator + "pack.mcmeta"))) {
            writer.write(packMcmeta);
        } catch (IOException e) {
            logger.severe("创建pack.mcmeta失败: " + e.getMessage());
        }
    }
    
    private void copyFile(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
    
    private void deleteDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        deleteDirectory(child);
                    }
                }
            }
            if (!file.delete()) {
                logger.warning("无法删除文件: " + file.getAbsolutePath());
            }
        }
    }
    
    /**
     * 获取图片对应的Unicode字符
     * @param imageKey 图片键名
     * @return Unicode字符
     */
    public String getUnicodeForImage(String imageKey) {
        return imageToUnicode.get(imageKey);
    }
    
    /**
     * 获取所有图片的Unicode映射
     * @return Unicode映射
     */
    public Map<String, String> getImageUnicodeMap() {
        return new HashMap<>(imageToUnicode);
    }
    
    /**
     * 获取已处理的图片列表
     * @return 已处理的图片列表
     */
    public Set<String> getProcessedImages() {
        return new HashSet<>(processedImages);
    }
    
    /**
     * 获取生成的zip文件路径
     * @return zip文件路径
     */
    public Path getZipFilePath() {
        String zipFilename = plugin.getConfigManager().getZipFilename();
        return plugin.getDataFolder().toPath().resolve(zipFilename);
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        imageToUnicode.clear();
        fontProviderCache.clear();
        processedImages.clear();
        unicodeCounter = 0;
    }
}
