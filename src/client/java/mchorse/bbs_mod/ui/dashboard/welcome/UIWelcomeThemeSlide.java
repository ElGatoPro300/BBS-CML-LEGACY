package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Slide 1: Theme color configuration with primary color picker and curated presets.
 */
public class UIWelcomeThemeSlide extends UIElement
{
    private UILabel title;
    private UILabel subtitle;
    private UIElement colorRow;
    private UIElement presetsRow;

    private UIColor primaryPicker;

    public UIWelcomeThemeSlide()
    {
        super();

        this.title = UI.label(UIKeys.WELCOME_THEME_TITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle = UI.label(UIKeys.WELCOME_THEME_SUBTITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle.color(0xaaaaaa, true);

        this.colorRow = new UIElement();
        this.colorRow.row(10);

        this.primaryPicker = new UIColor((c) -> BBSSettings.primaryColor.set(c));
        this.primaryPicker.picker.setColor(BBSSettings.primaryColor.get());
        this.primaryPicker.tooltip(UIKeys.WELCOME_THEME_PRIMARY);

        this.colorRow.add(this.primaryPicker);

        this.presetsRow = new UIElement();
        this.presetsRow.row(8);

        this.presetsRow.add(
            new ThemeCard(UIKeys.WELCOME_PRESET_NOSTALGIC, 0xea7a30, 0x181010),
            new ThemeCard(UIKeys.WELCOME_PRESET_CYAN, 0x00b4d8, 0x0c1218),
            new ThemeCard(UIKeys.WELCOME_PRESET_DARK, 0x5e81ac, 0x12141a),
            new ThemeCard(UIKeys.WELCOME_PRESET_LIGHT, 0xd8dee9, 0xf0f2f5)
        );

        this.title.relative(this).x(0.5F).y(12).w(1F).h(18).anchor(0.5F, 0F);
        this.subtitle.relative(this).x(0.5F).y(36).w(1F).h(14).anchor(0.5F, 0F);
        this.colorRow.relative(this).x(0.5F).y(66).w(130).h(24).anchor(0.5F, 0F);
        this.presetsRow.relative(this).x(0.5F).y(102).w(300).h(44).anchor(0.5F, 0F);

        this.add(this.title, this.subtitle, this.colorRow, this.presetsRow);
    }

    private class ThemeCard extends UIElement
    {
        private final IKey label;
        private final int primary;
        private final int background;

        public ThemeCard(IKey label, int primary, int background)
        {
            super();

            this.label = label;
            this.primary = primary;
            this.background = background;

            this.h(40);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context))
            {
                BBSSettings.primaryColor.set(this.primary);
                BBSSettings.backgroundColor.set(this.background);

                UIWelcomeThemeSlide.this.primaryPicker.picker.setColor(this.primary);
                UIUtils.playClick();

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            boolean hover = this.area.isInside(context);
            boolean selected = (BBSSettings.primaryColor.get() & Colors.RGB) == (this.primary & Colors.RGB);

            int cardBg = selected ? (0x44000000 | (this.primary & 0x40ffffff)) : (hover ? 0x22ffffff : 0x14ffffff);
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), cardBg);

            int outlineColor = selected ? (Colors.A100 | this.primary) : (hover ? 0x44ffffff : 0x22ffffff);

            if (selected)
            {
                int r = (this.primary >> 16) & 0xff;
                int g = (this.primary >> 8) & 0xff;
                int b = this.primary & 0xff;

                if ((r * 30 + g * 59 + b * 11) / 100 < 50)
                {
                    outlineColor = 0x88ffffff;
                }
            }

            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), outlineColor);

            /* Swatch pill */
            int swatchW = 24;
            int swatchH = 8;
            int swatchX = this.area.mx(swatchW);
            int swatchY = this.area.y + 8;
            int c1 = Colors.A100 | this.primary;

            context.batcher.box(swatchX, swatchY, swatchX + swatchW, swatchY + swatchH, c1);
            context.batcher.outline(swatchX, swatchY, swatchX + swatchW, swatchY + swatchH, 0x44ffffff);

            /* Label */
            FontRenderer font = context.batcher.getFont();
            String text = font.limitToWidth(this.label.get(), this.area.w - 4);
            int tx = this.area.mx(font.getWidth(text));
            int ty = this.area.y + 24;
            context.batcher.text(text, tx, ty, hover || selected ? Colors.WHITE : 0xaaaaaa, true);

            super.render(context);
        }
    }
}
