package consulo.flutter.color;

import com.jetbrains.lang.dart.highlight.DartSyntaxHighlighterColors;
import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.RGBColor;

/**
 * Upstream {@code <additionalTextAttributes scheme="Default" file="colorSchemes/FlutterCodeColorSchemeDefault.xml"/>}.
 * <p>
 * {@code colorSchemes/FlutterLogColorSchemeDefault.xml} is not ported: its {@code FLUTTER_LOG_*} keys are not used by the plugin.
 */
@ExtensionImpl
public final class FlutterDefaultEditorColorSchemeExtender implements EditorColorSchemeExtender {
  @Override
  public void extend(Builder builder) {
    // Customizations to the regular Dart color scheme to make Flutter UI as code more readable.
    builder.add(DartSyntaxHighlighterColors.CONSTRUCTOR,
                AttributesFlyweightBuilder.create().withForeground(RGBColor.fromRGBValue(0x2196f3)).build());
  }

  @Override
  public String getColorSchemeId() {
    return EditorColorsScheme.DEFAULT_SCHEME_NAME;
  }
}
