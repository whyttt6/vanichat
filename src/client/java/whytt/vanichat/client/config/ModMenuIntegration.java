package whytt.vanichat.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Objects;

import static whytt.vanichat.client.PaintDirectMessage.PaintText;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("vanichat.config.title"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // General category
            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("vanichat.config.category.general"));

            general.addEntry(entryBuilder.startBooleanToggle(
                            Text.translatable("vanichat.config.option.enable_filter"),
                            VaniChatConfig.getInstance().enableFilter)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> VaniChatConfig.getInstance().enableFilter = newValue)
                    .build()
            );

            // Direct messages category
            ConfigCategory direct_messages = builder.getOrCreateCategory(Text.translatable("vanichat.config.category.direct_messages"));

            direct_messages.addEntry(entryBuilder.startSelector(
                            Text.translatable("vanichat.config.option.color_scheme"),
                            new String[]{
                                    "vanichat.config.option.color_scheme.2_colors",
                                    "vanichat.config.option.color_scheme.gradient"
                            },
                            VaniChatConfig.getInstance().selectedOption)
                    .setDefaultValue("vanichat.config.option.color_scheme.2_colors")
                    .setSaveConsumer(newValue -> VaniChatConfig.getInstance().selectedOption = newValue)
                    .build()
            );

            direct_messages.addEntry(entryBuilder.startColorField(
                            Text.translatable("vanichat.config.option.custom_color1"),
                            VaniChatConfig.getInstance().customColor1)
                    .setDefaultValue(0x67E8F9)
                    .setSaveConsumer(newValue -> VaniChatConfig.getInstance().customColor1 = newValue)
                    .build()
            );

            direct_messages.addEntry(entryBuilder.startColorField(
                            Text.translatable("vanichat.config.option.custom_color2"),
                            VaniChatConfig.getInstance().customColor2)
                    .setDefaultValue(0x22D3EE)
                    .setSaveConsumer(newValue -> VaniChatConfig.getInstance().customColor2 = newValue)
                    .build()
            );

            // Use a generic example name
            Text exampleText = PaintText(false, "Игрок", "Это пример личного сообщения!", VaniChatConfig.getInstance().customColor1, VaniChatConfig.getInstance().customColor2, Objects.equals(VaniChatConfig.getInstance().selectedOption, "vanichat.config.option.color_scheme.2_colors"));

            direct_messages.addEntry(entryBuilder.startTextDescription(exampleText)
                    .build());

            builder.setSavingRunnable(VaniChatConfig.getInstance()::save);

            return builder.build();
        };
    }
}
