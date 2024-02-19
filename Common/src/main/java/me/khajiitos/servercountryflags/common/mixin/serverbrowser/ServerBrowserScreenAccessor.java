package me.khajiitos.servercountryflags.common.mixin.serverbrowser;

import com.epherical.serverbrowser.client.screen.ServerBrowserScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Pseudo
@Mixin(ServerBrowserScreen.class)
public interface ServerBrowserScreenAccessor {

    @Accessor(remap = false)
    List<Component> getToolTip();
}
