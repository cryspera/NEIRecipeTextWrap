package cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 仅用于离线编译的桩类，不会被打包。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    String modid();

    String name() default "";

    String version() default "";

    String acceptedMinecraftVersions() default "";

    boolean clientSideOnly() default false;

    String acceptableRemoteVersions() default "";
}
