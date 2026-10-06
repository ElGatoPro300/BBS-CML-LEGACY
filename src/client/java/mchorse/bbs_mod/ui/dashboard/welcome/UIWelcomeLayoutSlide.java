package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ui.EditorLayoutNode;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Matrix3x2fStack;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Slide 2: High-fidelity interactive film workspace layout selector.
 *
 * <p>Accurately mirrors Premiere Style, Horizontal (Simplified), and Vertical (Shorts/Reels)
 * workspace arrangements with high-detail mockup previews.</p>
 */
public class UIWelcomeLayoutSlide extends UIElement
{
    public enum LayoutPreset
    {
        PREMIERE(UIKeys.WELCOME_LAYOUT_PREMIERE_NAME, UIKeys.WELCOME_LAYOUT_PREMIERE_DESC, "premiere_style", "1920x1080 • 16:9", "Estilo Premiere"),
        HORIZONTAL(UIKeys.WELCOME_LAYOUT_HORIZONTAL_NAME, UIKeys.WELCOME_LAYOUT_HORIZONTAL_DESC, "horizontal_simplified", "1920x1080 • 16:9", "Horizontal"),
        VERTICAL(UIKeys.WELCOME_LAYOUT_VERTICAL_NAME, UIKeys.WELCOME_LAYOUT_VERTICAL_DESC, "vertical_simplified", "1080x1920 • 9:16", "Vertical (9:16)"),
        CUSTOM(UIKeys.WELCOME_LAYOUT_CUSTOM_NAME, UIKeys.WELCOME_LAYOUT_CUSTOM_DESC, null, "1920x1080 • 16:9", "Personalizado");

        public final IKey title;
        public final IKey description;
        public final String resource;
        public final String badge;
        public final String shortLabel;

        private LayoutPreset(IKey title, IKey description, String resource, String badge, String shortLabel)
        {
            this.title = title;
            this.description = description;
            this.resource = resource;
            this.badge = badge;
            this.shortLabel = shortLabel;
        }
    }

    private UILabel title;
    private UIElement selectorRow;

    private LayoutPreset selectedPreset = LayoutPreset.PREMIERE;

    public UIWelcomeLayoutSlide()
    {
        super();

        this.title = UI.label(UIKeys.WELCOME_LAYOUT_TITLE).labelAnchor(0.5F, 0.5F);

        this.selectorRow = new UIElement();
        this.selectorRow.row(5);

        for (LayoutPreset preset : LayoutPreset.values())
        {
            this.selectorRow.add(new LayoutPill(preset));
        }

        this.title.relative(this).x(0.5F).y(6).w(1F).h(16).anchor(0.5F, 0F);
        this.selectorRow.relative(this).x(0.5F).y(24).w(390).h(24).anchor(0.5F, 0F);

        this.add(this.title, this.selectorRow);
    }

    public LayoutPreset getSelectedPreset()
    {
        return this.selectedPreset;
    }

    public void applySelectedLayout()
    {
        if (this.selectedPreset == LayoutPreset.CUSTOM || this.selectedPreset.resource == null)
        {
            return;
        }

        MapType data = this.loadPresetResource(this.selectedPreset.resource);

        if (data == null)
        {
            return;
        }

        UIFilmPanel filmPanel = null;

        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            if (dashboard != null)
            {
                filmPanel = dashboard.getPanel(UIFilmPanel.class);
            }
        }
        catch (Exception e)
        {}

        if (filmPanel != null)
        {
            filmPanel.applyFilmLayoutFromPreset(data);
        }
        else
        {
            BaseType layoutData = data.get("film_layout");

            if (layoutData != null)
            {
                EditorLayoutNode root = EditorLayoutNode.fromData(layoutData);

                if (root != null)
                {
                    BBSSettings.editorLayoutSettings.setFilmLayoutRoot(root);
                }
            }

            if (data.has("video_frame_width") && data.has("video_frame_height"))
            {
                BBSSettings.videoSettings.width.set(data.getInt("video_frame_width"));
                BBSSettings.videoSettings.height.set(data.getInt("video_frame_height"));
            }

            if (data.has("timeline_toolbar_docks"))
            {
                BBSSettings.timelineToolbarDocks.applyPreset(data);
            }
        }
    }

    private MapType loadPresetResource(String resource)
    {
        try (InputStream stream = UIFilmPanel.class.getResourceAsStream("/assets/bbs/layout_presets/" + resource + ".json"))
        {
            if (stream == null)
            {
                return null;
            }

            BaseType data = DataToString.fromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));

            return data != null && data.isMap() ? data.asMap() : null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void render(UIContext context)
    {
        int cx = this.area.mx();

        /* 1. Large Detailed Layout Diagram Box */
        int diagW = 390;
        int diagH = 126;
        int diagX = cx - diagW / 2;
        int diagY = this.area.y + 52;

        context.batcher.box(diagX, diagY, diagX + diagW, diagY + diagH, 0xfa0e1216);
        context.batcher.outline(diagX, diagY, diagX + diagW, diagY + diagH, 0x38ffffff);

        this.renderDetailedLayoutPreview(context.batcher, this.selectedPreset, diagX + 1, diagY + 1, diagW - 2, diagH - 2);

        /* 2. Layout Description & Format Badge below */
        String desc = this.selectedPreset.description.get();
        int descY = diagY + diagH + 5;
        context.batcher.wallText(desc, diagX + 4, descY, 0xbbccdd, diagW - 8, 10, 0.5F, 0F);

        super.render(context);
    }

    private void renderDetailedLayoutPreview(Batcher2D batcher, LayoutPreset preset, float x, float y, float w, float h)
    {
        if (preset == LayoutPreset.PREMIERE)
        {
            /* 1. Premiere Layout (Photo 1):
             * Left Column (35%): Top Replay multi-track Timeline (60%), Bottom General Properties (40%)
             * Center Column (40%): Top 3D Viewport (60%), Bottom Camera Clip Timeline (40%)
             * Right Column (25%): Top Replays List (55%), Bottom General Inspector (45%) */
            float leftW = w * 0.35F;
            float midW = w * 0.40F;
            float rightW = w - leftW - midW;

            float topH = h * 0.58F;
            float botH = h - topH;

            this.drawReplayMultiTrackTimeline(batcher, x, y, leftW, topH, "Línea de tiempo de replay");
            this.drawPropertiesGeneralPanel(batcher, x, y + topH, leftW, botH, "Propiedades generales");

            this.drawViewportPanel(batcher, x + leftW, y, midW, topH, "Visor", false);
            this.drawCameraClipTimeline(batcher, x + leftW, y + topH, midW, botH, "Línea de tiempo de cámara");

            this.drawReplaysActorListPanel(batcher, x + leftW + midW, y, rightW, h * 0.55F, "Reproducciones");
            this.drawGeneralInspectorPanel(batcher, x + leftW + midW, y + h * 0.55F, rightW, h * 0.45F, "General");
        }
        else if (preset == LayoutPreset.HORIZONTAL)
        {
            /* 2. Horizontal Simplified (Photo 2):
             * Top Section (56%): Left Panoramic 3D Viewport (66%), Right General Properties (34%)
             * Bottom Section (44%): Left Tabbed Unified Timeline (66%), Right Replays List (55%) + General (45%) */
            float topH = h * 0.56F;
            float botH = h - topH;
            float leftW = w * 0.66F;
            float rightW = w - leftW;

            this.drawViewportPanel(batcher, x, y, leftW, topH, "Visor Panorámico", false);
            this.drawPropertiesGeneralPanel(batcher, x + leftW, y, rightW, topH, "Propiedades generales");

            this.drawTabbedUnifiedTimeline(batcher, x, y + topH, leftW, botH, "Línea de tiempo de cámara");
            this.drawReplaysActorListPanel(batcher, x + leftW, y + topH, rightW, botH * 0.55F, "Reproducciones");
            this.drawGeneralInspectorPanel(batcher, x + leftW, y + topH + botH * 0.55F, rightW, botH * 0.45F, "General");
        }
        else if (preset == LayoutPreset.VERTICAL)
        {
            /* 3. Vertical Shorts / Reels (Photo 3):
             * Left Column (27%): Top General Properties (35%), Bottom Replays List (35%) + General (30%)
             * Center Column (37%): Full-height Vertical Tabbed Timeline (Camera / Replay / Action)
             * Right Column (36%): Full-height Tall 9:16 3D Viewport */
            float leftW = w * 0.27F;
            float rightW = w * 0.36F;
            float midW = w - leftW - rightW;

            this.drawPropertiesGeneralPanel(batcher, x, y, leftW, h * 0.35F, "Propiedades generales");
            this.drawReplaysActorListPanel(batcher, x, y + h * 0.35F, leftW, h * 0.35F, "Reproducciones");
            this.drawGeneralInspectorPanel(batcher, x, y + h * 0.70F, leftW, h * 0.30F, "General");

            this.drawVerticalTallTimeline(batcher, x + leftW, y, midW, h, "Línea de tiempo de cámara");
            this.drawViewportPanel(batcher, x + leftW + midW, y, rightW, h, "Visor (9:16)", true);
        }
        else
        {
            /* 4. Custom / Default */
            float leftW = w * 0.62F;
            float rightW = w - leftW;
            float topH = h * 0.58F;
            float botH = h - topH;

            this.drawViewportPanel(batcher, x, y, leftW, topH, "Visor 3D", false);
            this.drawReplaysActorListPanel(batcher, x + leftW, y, rightW, topH * 0.55F, "Reproducciones");
            this.drawGeneralInspectorPanel(batcher, x + leftW, y + topH * 0.55F, rightW, topH * 0.45F, "General");
            this.drawCameraClipTimeline(batcher, x, y + topH, leftW, botH, "Línea de tiempo de cámara");
            this.drawPropertiesGeneralPanel(batcher, x + leftW, y + topH, rightW, botH, "Propiedades generales");
        }
    }

    private void drawHeader(Batcher2D batcher, float x, float y, float w, float h, String title, int bgColor, boolean activeTab)
    {
        batcher.box(x, y, x + w, y + h, bgColor);

        if (activeTab)
        {
            batcher.box(x, y, x + w, y + 1.2F, 0xff0088ff);
        }

        batcher.box(x, y + h - 0.8F, x + w, y + h, 0x28ffffff);

        if (w > 20)
        {
            float scale = 0.50F;
            Matrix3x2fStack matrices = batcher.getContext().pose();
            matrices.pushMatrix();
            matrices.scale(scale, scale);
            batcher.text(title, (x + 3) / scale, (y + 2.2F) / scale, 0xffe2e8f0, false);
            matrices.popMatrix();
        }
    }

    private void drawViewportPanel(Batcher2D batcher, float x, float y, float w, float h, String title, boolean isVertical)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff16202c, false);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        /* Dark 3D Viewport space */
        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff070b10);

        /* Perspective 3D floor grid */
        float horizonY = bodyY + bodyH * (isVertical ? 0.68F : 0.55F);
        batcher.box(x + 3, horizonY, x + w - 3, horizonY + 0.6F, 0x44337799);

        int gridLines = isVertical ? 4 : 6;

        for (int i = 0; i < gridLines; i++)
        {
            float nx = x + w * (0.15F + i * (0.70F / Math.max(1, gridLines - 1)));
            batcher.line(nx, horizonY, x + w * (0.05F + i * (0.90F / Math.max(1, gridLines - 1))), bodyY + bodyH - 2, 0.6F, 0x28386b99);
        }

        /* 3D Origin axes indicator in bottom-left corner */
        float ax = x + 4;
        float ay = bodyY + bodyH - 4;
        batcher.line(ax, ay, ax + 5, ay, 1.0F, 0xffff3333); /* Red X */
        batcher.line(ax, ay, ax, ay - 5, 1.0F, 0xff33ff33); /* Green Y */

        /* Center camera crosshair marker */
        float cx = x + w * 0.5F;
        float cy = bodyY + bodyH * 0.48F;
        batcher.box(cx - 2.5F, cy, cx + 2.5F, cy + 0.6F, 0x8800ffcc);
        batcher.box(cx, cy - 2.5F, cx + 0.6F, cy + 2.5F, 0x8800ffcc);

        /* Viewport mini-toolbar pill at bottom */
        float tbH = 5F;
        float tbW = Math.min(w - 12, 54F);
        float tbX = x + (w - tbW) * 0.5F;
        float tbY = bodyY + bodyH - tbH - 2;

        batcher.box(tbX, tbY, tbX + tbW, tbY + tbH, 0x33ffffff);
        batcher.outline(tbX, tbY, tbX + tbW, tbY + tbH, 0x33ffffff);

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawReplayMultiTrackTimeline(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff102028, true);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff090f14);

        /* Ruler bar */
        float rulerH = 5F;
        batcher.box(x, bodyY, x + w, bodyY + rulerH, 0xff121820);

        for (float rx = x + 6; rx < x + w - 4; rx += 11)
        {
            batcher.box(rx, bodyY + 1, rx + 0.8F, bodyY + rulerH - 1, 0x55ffffff);
        }

        /* Left track header column mockup (> Mundo, v Modelo, Visible...) */
        float labelColW = Math.min(w * 0.32F, 34F);
        batcher.box(x, bodyY + rulerH, x + labelColW, bodyY + bodyH, 0xff0f151c);
        batcher.box(x + labelColW, bodyY + rulerH, x + labelColW + 0.6F, bodyY + bodyH, 0x22ffffff);

        /* Horizontal channel lines & keyframe points */
        float curY = bodyY + rulerH + 3;
        int[] trackColors = {0xff38bdf8, 0xffa3e635, 0xfffbbf24, 0xfff472b6};
        float[] kfNorm = {0.20F, 0.45F, 0.70F, 0.88F};

        int idx = 0;

        while (curY + 4 < bodyY + bodyH - 4)
        {
            /* Channel line */
            batcher.box(x + labelColW + 2, curY, x + w - 2, curY + 0.6F, 0x30ffffff);

            /* Keyframe diamonds on track */
            for (float kn : kfNorm)
            {
                float kx = x + labelColW + 4 + (w - labelColW - 10) * kn;

                if (kx < x + w - 4)
                {
                    batcher.box(kx - 1F, curY - 1F, kx + 1F, curY + 1F, trackColors[idx % trackColors.length]);
                }
            }

            curY += 5.2F;
            idx++;
        }

        /* Lime playhead cursor line */
        float playX = x + labelColW + 6;
        batcher.box(playX, bodyY, playX + 1.0F, bodyY + bodyH, 0xff00ff66);

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawCameraClipTimeline(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff102418, true);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff0a120e);

        /* Ruler */
        float rulerH = 5F;
        batcher.box(x, bodyY, x + w, bodyY + rulerH, 0xff121e16);

        for (float rx = x + 6; rx < x + w - 4; rx += 14)
        {
            batcher.box(rx, bodyY + 1, rx + 0.8F, bodyY + rulerH - 1, 0x55ffffff);
        }

        /* Green clip bar "Estacionario" */
        float clipY = bodyY + rulerH + 4;
        float clipH = Math.min(bodyH - rulerH - 8, 8F);
        float clipW = w * 0.78F;

        if (clipH > 2)
        {
            batcher.box(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff10b981);
            batcher.outline(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff34d399);

            /* Mini clip label */
            if (clipW > 30)
            {
                float scale = 0.42F;
                Matrix3x2fStack matrices = batcher.getContext().pose();
                matrices.pushMatrix();
                matrices.scale(scale, scale);
                batcher.text("Estacionario", (x + 6) / scale, (clipY + 1.5F) / scale, 0xff042f1f, false);
                matrices.popMatrix();
            }
        }

        /* Lime playhead cursor */
        float playX = x + 16F;
        batcher.box(playX, bodyY, playX + 1.0F, bodyY + bodyH, 0xff00ff66);

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawTabbedUnifiedTimeline(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff12221a, true);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff0a130f);

        /* Ruler */
        float rulerH = 5F;
        batcher.box(x, bodyY, x + w, bodyY + rulerH, 0xff142018);

        for (float rx = x + 8; rx < x + w - 4; rx += 14)
        {
            batcher.box(rx, bodyY + 1, rx + 0.8F, bodyY + rulerH - 1, 0x55ffffff);
        }

        /* Green clip bar */
        float clipY = bodyY + rulerH + 4;
        float clipH = Math.min(bodyH - rulerH - 8, 9F);
        float clipW = w * 0.82F;

        if (clipH > 2)
        {
            batcher.box(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff10b981);
            batcher.outline(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff34d399);
        }

        /* Lime playhead */
        float playX = x + 20F;
        batcher.box(playX, bodyY, playX + 1.0F, bodyY + bodyH, 0xff00ff66);

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawVerticalTallTimeline(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff102018, true);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff09110d);

        /* Ruler */
        float rulerH = 5F;
        batcher.box(x, bodyY, x + w, bodyY + rulerH, 0xff121e16);

        for (float rx = x + 6; rx < x + w - 4; rx += 12)
        {
            batcher.box(rx, bodyY + 1, rx + 0.8F, bodyY + rulerH - 1, 0x55ffffff);
        }

        /* Tall green clip block */
        float clipY = bodyY + rulerH + 6;
        float clipH = 14F;
        float clipW = w * 0.85F;

        batcher.box(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff10b981);
        batcher.outline(x + 4, clipY, x + 4 + clipW, clipY + clipH, 0xff34d399);

        /* Lime playhead */
        float playX = x + 12F;
        batcher.box(playX, bodyY, playX + 1.0F, bodyY + bodyH, 0xff00ff66);

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawPropertiesGeneralPanel(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff1c1824, false);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff0f0d14);

        /* Field rows mockup */
        float curY = bodyY + 3;

        while (curY + 4 < bodyY + bodyH - 2)
        {
            batcher.box(x + 3, curY, x + w - 3, curY + 3.8F, 0xff201b2a);
            batcher.box(x + 5, curY + 1, x + w * 0.42F, curY + 2.8F, 0x77a78bfa);
            curY += 5.8F;
        }

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawReplaysActorListPanel(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff182230, false);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff0b1016);

        /* Row 1: Selected active actor (blue row) */
        float rowH = 6.2F;
        float rY = bodyY + 2;

        /* Row 1: Item with yellow/orange badge */
        batcher.box(x + 2, rY, x + w - 2, rY + rowH, 0xff1e40af);
        batcher.box(x + w - 6, rY + 1.5F, x + w - 3, rY + rowH - 1.5F, 0xffeab308);

        /* Row 2: Item with green badge */
        rY += rowH + 1.2F;
        if (rY + rowH < bodyY + bodyH - 1)
        {
            batcher.box(x + 2, rY, x + w - 2, rY + rowH, 0xff161e2c);
            batcher.box(x + w - 6, rY + 1.5F, x + w - 3, rY + rowH - 1.5F, 0xff22c55e);
        }

        /* Row 3: Item */
        rY += rowH + 1.2F;
        if (rY + rowH < bodyY + bodyH - 1)
        {
            batcher.box(x + 2, rY, x + w - 2, rY + rowH, 0xff121824);
        }

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    private void drawGeneralInspectorPanel(Batcher2D batcher, float x, float y, float w, float h, String title)
    {
        float hdrH = 9.5F;
        this.drawHeader(batcher, x, y, w, hdrH, title, 0xff201b24, false);

        float bodyY = y + hdrH;
        float bodyH = h - hdrH;

        if (bodyH <= 2) return;

        batcher.box(x, bodyY, x + w, bodyY + bodyH, 0xff0e0c12);

        /* Action buttons mockup ("Elegir", "Editar") */
        float btnY = bodyY + 3;
        float btnH = Math.min(bodyH - 6, 6.5F);
        float btnW = (w - 10) * 0.5F;

        if (btnH > 2)
        {
            batcher.box(x + 3, btnY, x + 3 + btnW, btnY + btnH, 0xff0284c7);
            batcher.box(x + 6 + btnW, btnY, x + 6 + btnW * 2, btnY + btnH, 0xff0284c7);
        }

        /* Toggle switch row */
        float toggleY = btnY + btnH + 3;
        if (toggleY + 4 < bodyY + bodyH - 1)
        {
            batcher.box(x + 3, toggleY, x + w * 0.40F, toggleY + 2.5F, 0x66ffffff);
            batcher.box(x + w - 10, toggleY, x + w - 3, toggleY + 3F, 0xff0ea5e9);
        }

        batcher.outline(x, y, x + w, y + h, 0x24ffffff);
    }

    /**
     * Sleek layout pill button.
     */
    private class LayoutPill extends UIElement
    {
        private final LayoutPreset preset;

        public LayoutPill(LayoutPreset preset)
        {
            super();

            this.preset = preset;
            this.h(22);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context))
            {
                UIWelcomeLayoutSlide.this.selectedPreset = this.preset;
                UIWelcomeLayoutSlide.this.applySelectedLayout();
                UIUtils.playClick();

                return true;
            }

            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            boolean hover = this.area.isInside(context);
            boolean selected = UIWelcomeLayoutSlide.this.selectedPreset == this.preset;
            int primary = BBSSettings.primaryColor.get();

            int pillBg = selected ? (0x66000000 | (primary & 0x70ffffff)) : (hover ? 0x28ffffff : 0x14ffffff);
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), pillBg);

            int outlineColor = selected ? (Colors.A100 | primary) : (hover ? 0x66ffffff : 0x28ffffff);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), outlineColor);

            FontRenderer font = context.batcher.getFont();
            String title = this.preset.shortLabel;
            int tx = this.area.mx(font.getWidth(title));
            int ty = this.area.my(font.getHeight());

            int textColor = selected ? Colors.WHITE : (hover ? 0xffffff : 0xcccccc);
            context.batcher.text(title, tx, ty, textColor, true);

            super.render(context);
        }
    }
}
