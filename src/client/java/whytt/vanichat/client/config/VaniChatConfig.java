package whytt.vanichat.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class VaniChatConfig {
    private static VaniChatConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("vanichat.json");

    public boolean enableFilter = true;
    public String selectedOption = "vanichat.config.option.color_scheme.2_colors";

    public int customColor1 = 0x67E8F9;
    public int customColor2 = 0x22D3EE;
    public int prefixColor = 0xFFCFF1;
    public boolean chatPrefixEnabled = true; // New field to toggle chat prefixes

    public static VaniChatConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VaniChatConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public void load() {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                    VaniChatConfig loaded = GSON.fromJson(reader, VaniChatConfig.class);
                    if (loaded != null) {
                        this.enableFilter = loaded.enableFilter;
                        this.selectedOption = loaded.selectedOption;
                        this.customColor1 = loaded.customColor1;
                        this.customColor2 = loaded.customColor2;
                        this.prefixColor = (loaded.prefixColor != 0) ? loaded.prefixColor : 0xFFCFF1;
                        // Load the new field, default to true if not present
                        this.chatPrefixEnabled = loaded.chatPrefixEnabled;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) { // Catch more general exceptions for robustness
             System.err.println("Error loading config or config is outdated, will use defaults: " + e.getMessage());
             save();
        }
    }

    public void save() {
        try {
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }
}
