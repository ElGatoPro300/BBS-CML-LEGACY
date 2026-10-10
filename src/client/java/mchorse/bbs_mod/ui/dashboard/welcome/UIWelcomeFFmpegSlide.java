package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.FFMpegUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Slide 3: FFmpeg encoder verification, path status and file finder.
 */
public class UIWelcomeFFmpegSlide extends UIElement
{
    private UILabel title;
    private UILabel subtitle;
    private UIButton pickButton;
    private UIButton guideButton;

    private boolean available;
    private String lastCheckedPath = "";

    public UIWelcomeFFmpegSlide()
    {
        super();

        this.title = UI.label(UIKeys.WELCOME_FFMPEG_TITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle = UI.label(UIKeys.WELCOME_FFMPEG_SUBTITLE).labelAnchor(0.5F, 0.5F);
        this.subtitle.color(0xaaaaaa, true);

        this.pickButton = new UIButton(UIKeys.WELCOME_FFMPEG_PICK, (b) -> this.findOrPickFFmpeg());
        this.guideButton = new UIButton(UIKeys.WELCOME_TUTORIALS, (b) -> UIUtils.openWebLink("https://www.youtube.com/@BBSCML"));

        this.title.relative(this).x(0.5F).y(12).w(1F).h(18).anchor(0.5F, 0F);
        this.subtitle.relative(this).x(0.5F).y(36).w(1F).h(14).anchor(0.5F, 0F);
        this.pickButton.relative(this).x(0.5F).y(106).w(180).h(24).anchor(0.5F, 0F);
        this.guideButton.relative(this).x(0.5F).y(136).w(180).h(20).anchor(0.5F, 0F);

        this.add(this.title, this.subtitle, this.pickButton, this.guideButton);

        this.refreshStatus();
    }

    public void refreshStatus()
    {
        this.lastCheckedPath = BBSSettings.videoEncoderPath.get();
        this.available = FFMpegUtils.checkFFMPEG();
    }

    private void findOrPickFFmpeg()
    {
        File[] roots = File.listRoots();
        File searchRoot = (roots != null && roots.length > 0) ? roots[0] : new File("C:\\");

        Optional<Path> found = FFMpegUtils.findFFMpeg(searchRoot.toPath());

        if (found.isPresent())
        {
            BBSSettings.videoEncoderPath.set(found.get().toAbsolutePath().toString());
            this.refreshStatus();
            UIUtils.playClick();
        }
        else
        {
            UIUtils.openWebLink("https://www.youtube.com/@BBSCML");
        }
    }

    @Override
    public void render(UIContext context)
    {
        String currentPath = BBSSettings.videoEncoderPath.get();

        if (!currentPath.equals(this.lastCheckedPath))
        {
            this.refreshStatus();
        }

        FontRenderer font = context.batcher.getFont();
        int cx = this.area.mx();
        int statusY = this.area.y + 64;

        if (this.available)
        {
            String text = "✔ " + UIKeys.WELCOME_FFMPEG_FOUND.get();
            context.batcher.text(text, cx - font.getWidth(text) / 2, statusY, 0x44ee88, true);

            String pathShort = font.limitToWidth(currentPath, this.area.w - 40);
            context.batcher.text(pathShort, cx - font.getWidth(pathShort) / 2, statusY + 16, 0x888899, true);
        }
        else
        {
            String text = "✖ " + UIKeys.WELCOME_FFMPEG_NOT_FOUND.get();
            context.batcher.text(text, cx - font.getWidth(text) / 2, statusY, 0xff5555, true);

            String hint = font.limitToWidth(currentPath.isEmpty() ? UIKeys.WELCOME_FFMPEG_NO_PATH.get() : currentPath, this.area.w - 40);
            context.batcher.text(hint, cx - font.getWidth(hint) / 2, statusY + 16, 0x888899, true);
        }

        super.render(context);
    }
}
