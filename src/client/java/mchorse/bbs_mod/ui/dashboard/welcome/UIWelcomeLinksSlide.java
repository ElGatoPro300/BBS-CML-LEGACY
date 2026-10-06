package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Slide 4: Community resources, 3.0 next era link, shortcuts hint, and the main Start action.
 */
public class UIWelcomeLinksSlide extends UIElement
{
    private UILabel title;
    private UILabel subtitle;
    private UIElement linksList;
    private UIButton startButton;
    private final Runnable onStart;
    private final Runnable onReplayIntro;

    public UIWelcomeLinksSlide(Runnable onStart, Runnable onReplayIntro)
    {
        super();

        this.onStart = onStart;
        this.onReplayIntro = onReplayIntro;

        this.title = UI.label(UIKeys.WELCOME_COMMUNITY_TITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle = UI.label(UIKeys.WELCOME_COMMUNITY_SUBTITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle.color(0xaaaaaa, true);

        this.linksList = new UIElement();
        this.linksList.column(4).vertical().stretch();

        if (this.onReplayIntro != null)
        {
            this.linksList.add(new LinkButton(Icons.PLAY, UIKeys.WELCOME_REPLAY_INTRO, this.onReplayIntro));
        }

        this.linksList.add(
            new LinkButton(Icons.YOUTUBE, UIKeys.WELCOME_YOUTUBE, () -> UIUtils.openWebLink("https://www.youtube.com/@BBSCML")),
            new LinkButton(Icons.HELP, UIKeys.WELCOME_WIKI, () -> UIUtils.openWebLink("https://github.com/ElGatoPro300/BBS-CML/wiki")),
            new LinkButton(Icons.BUBBLE, UIKeys.WELCOME_DISCORD, () -> UIUtils.openWebLink("https://discord.gg/MAHVQBSce6"))
        );

        this.startButton = new UIButton(UIKeys.WELCOME_START_STUDIO, (b) ->
        {
            if (this.onStart != null)
            {
                this.onStart.run();
            }
        });

        this.title.relative(this).x(0.5F).y(8).w(1F).h(16).anchor(0.5F, 0F);
        this.subtitle.relative(this).x(0.5F).y(26).w(1F).h(12).anchor(0.5F, 0F);
        this.linksList.relative(this).x(0.5F).y(44).w(260).h(98).anchor(0.5F, 0F);
        this.startButton.relative(this).x(0.5F).y(168).w(240).h(24).anchor(0.5F, 0F);

        this.add(this.title, this.subtitle, this.linksList, this.startButton);
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        String hint = UIKeys.WELCOME_KEYBINDS_HINT.get();
        int cx = this.area.mx();
        int hintY = this.area.y + 150;

        context.batcher.text(hint, cx - font.getWidth(hint) / 2, hintY, 0x888899, true);

        this.startButton.custom = true;
        this.startButton.customColor = Colors.setA(BBSSettings.primaryColor.get(), 1.0F);
        this.startButton.textColor = Colors.WHITE;

        super.render(context);
    }

    private static class LinkButton extends UIClickable<LinkButton>
    {
        private final Icon icon;
        private final IKey label;

        public LinkButton(Icon icon, IKey label, Runnable action)
        {
            super((b) ->
            {
                if (action != null)
                {
                    action.run();
                }
            });

            this.icon = icon;
            this.label = label;
            this.h(19);
        }

        @Override
        protected LinkButton get()
        {
            return this;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            FontRenderer font = context.batcher.getFont();
            int iconColor = this.hover ? Colors.WHITE : 0xcccccc;
            int textColor = this.hover ? Colors.WHITE : 0xbbbbbb;

            int boxColor = this.hover ? 0x33ffffff : 0x18ffffff;
            int outlineColor = this.hover ? 0x55ffffff : 0x24ffffff;

            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), boxColor);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), outlineColor);

            context.batcher.icon(this.icon, iconColor, this.area.x + 8, this.area.my(), 0.5F, 0.5F);
            context.batcher.text(this.label.get(), this.area.x + 24, this.area.my(font.getHeight()), textColor, true);

            context.batcher.icon(Icons.EXTERNAL, this.hover ? 0xccffffff : 0x55ffffff, this.area.ex() - 10, this.area.my(), 0.5F, 0.5F);
        }
    }
}
