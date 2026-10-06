package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Slide 0: Farewell and tribute to BBS CML 2.x.
 *
 * <p>Presents the farewell message, player avatar greeting, and gratitude for all creations.</p>
 */
public class UIWelcomeFarewellSlide extends UIElement
{
    private static final Link BBS_LOGO = Link.assets("textures/bbs_cml.png");

    public UIWelcomeFarewellSlide()
    {
        super();
    }

    private void drawPlayerHead(GuiGraphicsExtractor drawContext, Identifier skinTexture, int x, int y, int size)
    {
        drawContext.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 8.0F, 8.0F, size, size, 8, 8, 64, 64);
        drawContext.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 40.0F, 8.0F, size, size, 8, 8, 64, 64);
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int cx = this.area.mx();
        int topY = this.area.y + 10;

        /* 1. BBS CML Logo / Header Badge */
        Texture logo = BBSModClient.getTextures().getTexture(BBS_LOGO);

        if (logo != null)
        {
            float logoScale = 2.4F;
            float logoW = logo.width * logoScale;
            float logoH = logo.height * logoScale;
            float logoX = cx - logoW / 2F;
            float logoY = topY;

            context.batcher.texturedBox(logo, Colors.WHITE, logoX, logoY, logoW, logoH, 0, 0, logo.width, logo.height);
            topY += logoH + 6;
        }
        else
        {
            String title = UIKeys.WELCOME_FAREWELL_TITLE.get();
            int tw = font.getWidth(title);
            context.batcher.textShadow(title, cx - tw / 2, topY, Colors.WHITE);
            topY += 16;
        }

        /* 2. Farewell Subtitle / Badge */
        String subtitle = UIKeys.WELCOME_FAREWELL_SUBTITLE.get();
        int subW = font.getWidth(subtitle);
        int badgePad = 8;
        int badgeW = subW + badgePad * 2;
        int badgeH = 13;
        int badgeX = cx - badgeW / 2;
        int badgeY = topY;

        context.batcher.box(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0x44ff8844);
        context.batcher.outline(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0x66ffaa66);
        context.batcher.text(subtitle, cx - subW / 2, badgeY + 3, 0xffeedd, true);

        topY += badgeH + 8;

        /* 3. Personalized Player Greeting with Avatar */
        Minecraft mc = Minecraft.getInstance();
        String username = mc.player != null ? mc.player.getGameProfile().name() : mc.getUser().getName();
        Identifier skinTexture = null;

        if (mc.player != null)
        {
            try
            {
                skinTexture = mc.getSkinManager().createLookup(mc.player.getGameProfile(), true).get().body().id();
            }
            catch (Exception e)
            {}
        }

        String welcomePrefix = UIKeys.WELCOME_TITLE1.get();
        String welcomeText = welcomePrefix + " " + username;
        int headSize = 12;
        int greetW = headSize + 5 + font.getWidth(welcomeText);
        int greetX = cx - greetW / 2;
        int greetY = topY;

        if (skinTexture != null)
        {
            this.drawPlayerHead(context.batcher.getContext(), skinTexture, greetX, greetY - 1, headSize);
        }
        else
        {
            context.batcher.iconArea(Icons.USER, Colors.WHITE, greetX, greetY - 1, headSize, headSize);
        }

        context.batcher.textShadow(welcomeText, greetX + headSize + 5, greetY + 1, 0xffe8c0);

        topY += 20;

        /* 4. Farewell & Gratitude description in glassmorphism note card */
        String desc = UIKeys.WELCOME_FAREWELL_DESC.get();
        int cardW = this.area.w - 30;
        int cardH = 50;
        int cardX = cx - cardW / 2;
        int cardY = topY;

        context.batcher.box(cardX, cardY, cardX + cardW, cardY + cardH, 0x22ffffff);
        context.batcher.outline(cardX, cardY, cardX + cardW, cardY + cardH, 0x33ffffff);

        context.batcher.wallText(desc, cardX + 8, cardY + 7, 0xdddddd, cardW - 16, 11, 0.5F, 0F);

        super.render(context);
    }
}
