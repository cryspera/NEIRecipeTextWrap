package net.minecraft.client;

/**
 * 编译期桩类。方法名故意写成 SRG 名（func_71410_x = getMinecraft，field_71466_p = fontRenderer），
 * 这样编出来的字节码与正式发布的 1.7.10 模组一致，运行时由 FML 的反混淆转换器映射成实际名字。
 */
public class Minecraft {

    public net.minecraft.client.gui.FontRenderer field_71466_p;

    /** field_71462_r = currentScreen */
    public net.minecraft.client.gui.GuiScreen field_71462_r;

    public static Minecraft func_71410_x() {
        return null;
    }
}
