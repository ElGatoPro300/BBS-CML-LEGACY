package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Onboarding & Farewell Wizard Orchestrator for BBS CML 2.x.
 *
 * <p>Presents a full-screen nostalgic painted meadow experience with paginated slides,
 * smooth transition navigation, synchronized ambient audio, and a cinematic fade-out upon completion.</p>
 */
public class UIWelcomeOverlayPanel extends UIElement
{
    private static final float CLOSE_DURATION_SEC = 1.2F;

    private final UINostalgicBackground background = new UINostalgicBackground();
    private final List<UIElement> slides = new ArrayList<>();
    private final UIElement slideContainer;
    private final UIElement dotsContainer;
    private final UIIcon prevArrow;
    private final UIIcon nextArrow;

    private int currentSlideIndex = 0;

    /* Smooth exit fade transition state */
    private boolean isClosing;
    private boolean isClosed;
    private long closeStartTime;

    private UIWelcomeCinematicIntro intro;

    public UIWelcomeOverlayPanel()
    {
        this(true);
    }

    public UIWelcomeOverlayPanel(boolean playIntro)
    {
        super();

        this.mousePropagation = EventPropagation.BLOCK;
        this.keyboardPropagation = EventPropagation.BLOCK;

        this.slideContainer = new UIElement();
        this.slideContainer.relative(this).xy(0.5F, 0.5F).w(410).h(228).anchor(0.5F, 0.5F);

        /* Register slides with full relative sizing */
        UIElement farewellSlide = new UIWelcomeFarewellSlide();
        farewellSlide.relative(this.slideContainer).wh(1F, 1F);
        this.slides.add(farewellSlide);

        UIElement themeSlide = new UIWelcomeThemeSlide();
        themeSlide.relative(this.slideContainer).wh(1F, 1F);
        this.slides.add(themeSlide);

        UIWelcomeLayoutSlide layoutSlide = new UIWelcomeLayoutSlide();
        layoutSlide.relative(this.slideContainer).wh(1F, 1F);
        this.slides.add(layoutSlide);

        UIElement ffmpegSlide = new UIWelcomeFFmpegSlide();
        ffmpegSlide.relative(this.slideContainer).wh(1F, 1F);
        this.slides.add(ffmpegSlide);

        UIElement linksSlide = new UIWelcomeLinksSlide(this::startClosingAnimation, this::playIntro);
        linksSlide.relative(this.slideContainer).wh(1F, 1F);
        this.slides.add(linksSlide);

        this.slideContainer.add(this.slides.get(0));

        /* Navigation controls */
        this.prevArrow = new UIIcon(Icons.MOVE_LEFT, (b) -> this.prevSlide());
        this.nextArrow = new UIIcon(Icons.MOVE_RIGHT, (b) -> this.nextSlide());

        this.prevArrow.relative(this).x(0.5F, -235).y(0.5F, 0).wh(24, 24).anchor(0.5F, 0.5F);
        this.nextArrow.relative(this).x(0.5F, 235).y(0.5F, 0).wh(24, 24).anchor(0.5F, 0.5F);

        /* Pagination dots container placed below the card */
        int dotSize = 12;
        int dotMargin = 6;
        int dotsW = this.slides.size() * dotSize + (this.slides.size() - 1) * dotMargin;

        this.dotsContainer = new UIElement();
        this.dotsContainer.relative(this).x(0.5F).y(0.5F, 134).w(dotsW).h(16).anchor(0.5F, 0.5F);
        this.dotsContainer.row(dotMargin);

        for (int i = 0; i < this.slides.size(); i++)
        {
            final int index = i;
            this.dotsContainer.add(new PaginationDot(index, () -> this.setSlide(index)));
        }

        this.add(this.slideContainer, this.prevArrow, this.nextArrow, this.dotsContainer);
        this.updateNavigationState();

        if (playIntro)
        {
            this.playIntro();
        }
    }

    public void playIntro()
    {
        this.intro = new UIWelcomeCinematicIntro(null);
        this.intro.full(this);
        this.intro.start();
    }

    public void prevSlide()
    {
        if (this.currentSlideIndex > 0)
        {
            this.setSlide(this.currentSlideIndex - 1);
            UIUtils.playClick();
        }
    }

    public void nextSlide()
    {
        if (this.currentSlideIndex < this.slides.size() - 1)
        {
            this.setSlide(this.currentSlideIndex + 1);
            UIUtils.playClick();
        }
    }

    public void setSlide(int index)
    {
        if (index < 0 || index >= this.slides.size() || index == this.currentSlideIndex)
        {
            return;
        }

        this.slideContainer.removeAll();
        this.currentSlideIndex = index;
        this.slideContainer.add(this.slides.get(index));
        this.slideContainer.resize();

        this.updateNavigationState();
    }

    private void updateNavigationState()
    {
        this.prevArrow.setVisible(this.currentSlideIndex > 0);
        this.nextArrow.setVisible(this.currentSlideIndex < this.slides.size() - 1);
    }

    @Override
    protected IUIElement childrenMouseClicked(UIContext context)
    {
        if (this.intro != null && !this.intro.isFinished())
        {
            this.intro.skip();

            return this;
        }

        if (this.isClosing)
        {
            return this;
        }

        return super.childrenMouseClicked(context);
    }

    @Override
    protected IUIElement childrenKeyPressed(UIContext context)
    {
        if (this.intro != null && !this.intro.isFinished())
        {
            this.intro.skip();

            return this;
        }

        return super.childrenKeyPressed(context);
    }

    public void startClosingAnimation()
    {
        if (this.isClosing)
        {
            return;
        }

        this.isClosing = true;
        this.closeStartTime = System.currentTimeMillis();
        UIUtils.playClick();
    }

    private void setIntroAudioVolume(float volume)
    {
        if (BBSModClient.getSounds() == null)
        {
            return;
        }

        for (SoundPlayer player : BBSModClient.getSounds().getPlayers())
        {
            if (player.getBuffer() != null && UIWelcomeCinematicIntro.THEEND_MUSIC.equals(player.getBuffer().getId()))
            {
                player.setVolume(Math.max(0.0F, volume * 0.2F));
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        float energy = 1.0F;
        float particleEnergy = 1.0F;

        if (this.intro != null && !this.intro.isFinished())
        {
            float reveal = this.intro.getRevealProgress();
            energy = reveal;
            particleEnergy = reveal;
        }
        else if (this.isClosing)
        {
            float elapsed = (System.currentTimeMillis() - this.closeStartTime) / 1000F;
            float progress = Math.min(1.0F, Math.max(0.0F, elapsed / CLOSE_DURATION_SEC));

            /* Dissolve meadow atmosphere gradually to reveal BBS dashboard */
            energy = 1.0F - progress * progress * (3.0F - 2.0F * progress);

            /* Dissolve floating breeze particles dynamically with the card */
            float cardProgress = Math.min(1.0F, progress / 0.45F);
            particleEnergy = 1.0F - cardProgress * cardProgress * (3.0F - 2.0F * cardProgress);

            /* Fade out intro audio smoothly */
            this.setIntroAudioVolume(energy);

            if (progress >= 1.0F)
            {
                if (!this.isClosed)
                {
                    this.isClosed = true;
                    BBSSettings.welcomeFarewellSeen.set(true);

                    if (BBSModClient.getSounds() != null)
                    {
                        BBSModClient.getSounds().stop(UIWelcomeCinematicIntro.THEEND_MUSIC);
                    }

                    this.setVisible(false);
                    this.removeFromParent();
                }

                return;
            }
        }

        /* 0. Solid opaque backing quad */
        int baseAlpha = this.isClosing ? (int) (0xFF * energy) : 0xFF;

        if (baseAlpha > 0)
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), (baseAlpha << 24) | 0x040308);
        }

        /* 1. Render atmospheric nostalgic painted background */
        this.background.render(context, this.area, energy, particleEnergy);

        /* 2. Render welcome card and controls with smooth scale, slide down, and fade */
        if (this.intro != null && !this.intro.isFinished())
        {
            float reveal = this.intro.getRevealProgress();

            if (reveal > 0.01F)
            {
                float cardAlpha = reveal;
                float dy = (1.0F - reveal) * 16.0F;

                Matrix3x2fStack matrices = context.batcher.getContext().getMatrices();
                matrices.pushMatrix();
                matrices.translate(0F, dy);

                int cardBg = ((int) (0xb8 * cardAlpha) << 24) | 0x090c14;
                context.batcher.box(this.slideContainer.area.x, this.slideContainer.area.y,
                    this.slideContainer.area.ex(), this.slideContainer.area.ey(), cardBg);
                context.batcher.outline(this.slideContainer.area.x, this.slideContainer.area.y,
                    this.slideContainer.area.ex(), this.slideContainer.area.ey(),
                    ((int) (0x38 * cardAlpha) << 24) | 0x77aaee);

                super.render(context);

                matrices.popMatrix();
            }

            this.intro.area.copy(this.area);
            this.intro.render(context);

            if (this.intro.isFinished())
            {
                this.intro = null;
            }
        }
        else if (this.isClosing)
        {
            float elapsed = (System.currentTimeMillis() - this.closeStartTime) / 1000F;
            float progress = Math.min(1.0F, Math.max(0.0F, elapsed / CLOSE_DURATION_SEC));
            float cardProgress = Math.min(1.0F, progress / 0.45F);

            if (cardProgress < 1.0F)
            {
                float cardAlpha = 1.0F - cardProgress * cardProgress * (3.0F - 2.0F * cardProgress);
                float scale = 1.0F - 0.05F * cardProgress;
                float dy = cardProgress * 18.0F;

                float cx = this.slideContainer.area.mx();
                float cy = this.slideContainer.area.my();

                Matrix3x2fStack matrices = context.batcher.getContext().getMatrices();
                matrices.pushMatrix();
                matrices.translate(cx, cy);
                matrices.scale(scale, scale);
                matrices.translate(-cx, -cy + dy);

                int cardBg = ((int) (0xb8 * cardAlpha) << 24) | 0x090c14;
                context.batcher.box(this.slideContainer.area.x, this.slideContainer.area.y,
                    this.slideContainer.area.ex(), this.slideContainer.area.ey(), cardBg);
                context.batcher.outline(this.slideContainer.area.x, this.slideContainer.area.y,
                    this.slideContainer.area.ex(), this.slideContainer.area.ey(),
                    ((int) (0x38 * cardAlpha) << 24) | 0x77aaee);

                super.render(context);

                matrices.popMatrix();
            }
        }
        else
        {
            int cardBg = 0xc0090c14;
            context.batcher.box(this.slideContainer.area.x, this.slideContainer.area.y,
                this.slideContainer.area.ex(), this.slideContainer.area.ey(), cardBg);
            context.batcher.outline(this.slideContainer.area.x, this.slideContainer.area.y,
                this.slideContainer.area.ex(), this.slideContainer.area.ey(), 0x3877aaee);

            super.render(context);
        }
    }

    /**
     * Interactive pagination dot.
     */
    private class PaginationDot extends UIElement
    {
        private final int index;
        private final Runnable onClick;

        public PaginationDot(int index, Runnable onClick)
        {
            super();

            this.index = index;
            this.onClick = onClick;
            this.wh(12, 12);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context))
            {
                if (this.onClick != null)
                {
                    this.onClick.run();
                    UIUtils.playClick();
                }

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            boolean active = UIWelcomeOverlayPanel.this.currentSlideIndex == this.index;
            boolean hover = this.area.isInside(context);

            int primary = BBSSettings.primaryColor.get() & Colors.RGB;
            int dotColor = active ? (Colors.A100 | primary) : (hover ? 0x88ffffff : 0x33ffffff);

            int sz = active ? 6 : 4;
            int dx = this.area.mx(sz);
            int dy = this.area.my(sz);

            context.batcher.box(dx, dy, dx + sz, dy + sz, dotColor);

            super.render(context);
        }
    }
}
