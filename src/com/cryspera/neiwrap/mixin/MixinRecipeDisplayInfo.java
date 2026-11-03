// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (c) 2026 cryspera

package com.cryspera.neiwrap.mixin;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.modularui.api.math.Pos2d;

import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.api.util.GTRecipe;
import gregtech.nei.RecipeDisplayInfo;

/**
 * NEI 配方面板文字修正，两个独立的小改动：
 *
 * <h3>1. 长文本自动换行</h3>
 * GregTech 在 {@link RecipeDisplayInfo#drawText(String, int, int)} 里一次性
 * {@code fontRenderer.drawString(...)}，既不换行也不裁剪，长 mod 名会压到面板边框外。
 * 这里在方法头部接手：先用 {@code listFormattedStringToWidth} 按可用宽度切行，
 * 再对每一行回调 {@code drawText}（yPos 会自动累加），最后取消原调用。
 *
 * <h3>2. 物品与文字不再重叠</h3>
 * 配方输入超过 {@code maxItemInputs} 时，{@code GTNEIDefaultHandler.CachedDefaultRecipe}
 * 会把多出来的物品按“实际数量”重新取一遍网格坐标，这些格子会一直往下排，
 * 越过 recipeBackgroundSize 的背景范围，直接压在时间 / 来源文字上。
 * 这里在画第一行文字之前，按同一套 {@code getItemInputPositions} 算出网格的真实底边，
 * 把文字起点下移到网格下方（只在确实溢出时才动，正常配方完全不受影响）。
 *
 * <p>
 * 注意：本类字节码里对 MC 成员的引用全部使用 SRG 名（{@code func_71410_x} / {@code field_71466_p} /
 * {@code func_78256_a} / {@code func_78271_c}），这是 1.7.10 生产环境的约定；
 * 写 MCP 名会在运行时抛 {@code NoSuchMethodError}。
 */
@Mixin(value = RecipeDisplayInfo.class, remap = false)
public class MixinRecipeDisplayInfo {

    /** 防止自己回调 drawText 时再次进入本方法。 */
    @Unique
    private static boolean neiwrap$wrapping = false;

    /** 本次 RecipeDisplayInfo（= 本帧这一次配方绘制）是否已经做过文字下沉。默认 false，无需初始化。 */
    @Unique
    private boolean neiwrap$layoutDone;

    @Unique
    private static Field neiwrap$yPosField = null;

    @Unique
    private static boolean neiwrap$yPosLookedUp = false;

    @Inject(method = "drawText(Ljava/lang/String;II)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void neiwrap$onDrawText(String text, int xStart, int yShift, CallbackInfo ci) {
        RecipeDisplayInfo self = (RecipeDisplayInfo) (Object) this;

        if (!neiwrap$layoutDone) {
            neiwrap$layoutDone = true;
            neiwrap$shiftTextBelowItems(self);
        }

        if (neiwrap$wrapping || text == null || text.isEmpty() || yShift <= 0) {
            return;
        }

        List<String> wrapped = neiwrap$split(text, self, xStart);
        if (wrapped == null || wrapped.size() < 2) {
            return;
        }

        neiwrap$wrapping = true;
        try {
            for (int i = 0; i < wrapped.size(); i++) {
                self.drawText(wrapped.get(i), xStart, yShift);
            }
        } finally {
            neiwrap$wrapping = false;
        }
        ci.cancel();
    }

    /**
     * 把文字起点下移到物品网格下方。
     *
     * <p>
     * 只有“配方内容超出面板容量”（正是 GT 追加溢出格子的条件）时才计算，
     * 其余配方直接返回，保证其它配方表的外观与原来完全一致。
     */
    @Unique
    private static void neiwrap$shiftTextBelowItems(RecipeDisplayInfo info) {
        try {
            GTRecipe recipe = info.recipe;
            if (recipe == null) {
                return;
            }
            RecipeMapFrontend frontend = info.recipeMap.getFrontend();
            BasicUIProperties ui = frontend.getUIProperties();

            boolean overflow = false;
            if (recipe.mInputs != null) {
                overflow |= recipe.mInputs.length > ui.maxItemInputs;
            }
            if (recipe.mOutputs != null) {
                overflow |= recipe.mOutputs.length > ui.maxItemOutputs;
            }
            if (recipe.mFluidInputs != null) {
                overflow |= recipe.mFluidInputs.length > ui.maxFluidInputs;
            }
            if (recipe.mFluidOutputs != null) {
                overflow |= recipe.mFluidOutputs.length > ui.maxFluidOutputs;
            }
            if (!overflow) {
                return;
            }

            int lastSlotY = -1;
            if (recipe.mInputs != null && recipe.mInputs.length > 0) {
                lastSlotY = Math.max(lastSlotY, neiwrap$maxY(frontend.getItemInputPositions(recipe.mInputs.length)));
            }
            if (recipe.mOutputs != null && recipe.mOutputs.length > 0) {
                lastSlotY = Math.max(lastSlotY, neiwrap$maxY(frontend.getItemOutputPositions(recipe.mOutputs.length)));
            }
            if (recipe.mFluidInputs != null && recipe.mFluidInputs.length > 0) {
                lastSlotY = Math.max(lastSlotY, neiwrap$maxY(frontend.getFluidInputPositions(recipe.mFluidInputs.length)));
            }
            if (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0) {
                lastSlotY = Math.max(lastSlotY, neiwrap$maxY(frontend.getFluidOutputPositions(recipe.mFluidOutputs.length)));
            }
            if (recipe.mSpecialItems != null) {
                Pos2d special = frontend.getSpecialItemPosition();
                if (special != null) {
                    lastSlotY = Math.max(lastSlotY, special.y);
                }
            }
            if (lastSlotY < 0) {
                return;
            }

            // 槽位高 18px，物品本身还向下偏 1px；再留 1px 空隙
            int targetY = lastSlotY + 18 + 2;
            int currentY = info.getYPos();
            if (targetY <= currentY) {
                return;
            }

            // 别把文字推到面板外边：按屏幕高度估算配方区的底边（顶部标题/页签约 98px），
            // 再给最多 3 行文字留位置。估算偏保守时最多退回“文字贴着物品”，不会跑到面板外。
            int limit = neiwrap$textBottomLimit() - 30;
            if (limit > currentY && targetY > limit) {
                targetY = limit;
            }
            if (targetY <= currentY) {
                return;
            }

            Field field = neiwrap$yPos();
            if (field == null) {
                return;
            }
            field.setInt(info, targetY);
        } catch (Throwable ignored) {
            // 任何异常都退回原版排版，绝不影响配方显示
        }
    }

    /** 配方区在屏幕坐标下的可用底边（拿不到时返回极大值，等于不做限制）。 */
    @Unique
    private static int neiwrap$textBottomLimit() {
        try {
            Minecraft minecraft = Minecraft.func_71410_x();
            if (minecraft != null && minecraft.field_71462_r != null) {
                return minecraft.field_71462_r.field_146295_m - 98;
            }
        } catch (Throwable ignored) {
            // 落到“不限制”
        }
        return Integer.MAX_VALUE;
    }

    @Unique
    private static int neiwrap$maxY(List<Pos2d> positions) {
        int max = -1;
        if (positions == null) {
            return max;
        }
        for (int i = 0; i < positions.size(); i++) {
            Pos2d pos = positions.get(i);
            if (pos != null && pos.y > max) {
                max = pos.y;
            }
        }
        return max;
    }

    @Unique
    private static Field neiwrap$yPos() {
        if (!neiwrap$yPosLookedUp) {
            neiwrap$yPosLookedUp = true;
            try {
                Field field = RecipeDisplayInfo.class.getDeclaredField("yPos");
                field.setAccessible(true);
                neiwrap$yPosField = field;
            } catch (Throwable ignored) {
                neiwrap$yPosField = null;
            }
        }
        return neiwrap$yPosField;
    }

    /**
     * @return 折行后的文本；不需要折行（或出现任何异常）时返回 null，让原逻辑照常绘制。
     */
    @Unique
    private static List<String> neiwrap$split(String text, RecipeDisplayInfo info, int xStart) {
        try {
            Minecraft minecraft = Minecraft.func_71410_x();
            if (minecraft == null) {
                return null;
            }
            FontRenderer fontRenderer = minecraft.field_71466_p;
            if (fontRenderer == null) {
                return null;
            }

            int maxWidth = neiwrap$maxTextWidth(info, xStart);
            if (maxWidth <= 0) {
                return null;
            }
            if (fontRenderer.func_78256_a(text) <= maxWidth) {
                return null;
            }
            return fontRenderer.func_78271_c(text, maxWidth);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 面板内可用的文字宽度 = 面板宽度 - 左右各一个 xStart 边距。
     */
    @Unique
    private static int neiwrap$maxTextWidth(RecipeDisplayInfo info, int xStart) {
        int panelWidth = -1;
        try {
            panelWidth = info.recipeMap.getFrontend().getNEIProperties().recipeBackgroundSize.width;
        } catch (Throwable ignored) {
            // 落到兜底值
        }
        if (panelWidth <= 32) {
            return 166;
        }
        int maxWidth = panelWidth - 2 * xStart;
        return maxWidth > 32 ? maxWidth : 166;
    }
}
