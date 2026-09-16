package com.fireflytornado.mcupdate.javafx;

import com.zack88604.autoupdater.gui.api.GuiAdapterContext;
import com.zack88604.autoupdater.gui.api.JavaHelperGuiPresetFactory;
import com.zack88604.autoupdater.gui.api.JavaHelperLaunchSpec;

import java.util.Objects;

/**
 * Lightweight bootstrap loaded by the updater before JavaFX is available.
 *
 * <p>This class deliberately has no {@code javafx.*} references. The updater
 * loads it in the Minecraft JVM, while the actual UI is created later in the
 * isolated helper JVM.</p>
 */
public final class JavaFxPresetFactory implements JavaHelperGuiPresetFactory {

    /** Public no-argument constructor required by the preset loader. */
    public JavaFxPresetFactory() {
    }

    @Override
    public JavaHelperLaunchSpec create(GuiAdapterContext context) {
        Objects.requireNonNull(context, "context");
        return JavaHelperLaunchSpec.empty();
    }
}
