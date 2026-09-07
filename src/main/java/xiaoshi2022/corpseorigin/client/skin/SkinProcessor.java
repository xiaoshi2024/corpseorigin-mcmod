package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;

public class SkinProcessor {

    public static NativeImage convertLegacySkin(NativeImage oldImage) {
        NativeImage newImage = new NativeImage(64, 64, true);
        newImage.copyFrom(oldImage);
        oldImage.close();

        newImage.fillRect(0, 32, 64, 32, 0);

        newImage.copyRect(4, 16, 16, 32, 4, 4, true, false);
        newImage.copyRect(8, 16, 16, 32, 4, 4, true, false);
        newImage.copyRect(0, 20, 24, 32, 4, 12, true, false);
        newImage.copyRect(4, 20, 16, 32, 4, 12, true, false);
        newImage.copyRect(8, 20, 8, 32, 4, 12, true, false);
        newImage.copyRect(12, 20, 16, 32, 4, 12, true, false);
        newImage.copyRect(44, 16, -8, 32, 4, 4, true, false);
        newImage.copyRect(48, 16, -8, 32, 4, 4, true, false);
        newImage.copyRect(40, 20, 0, 32, 4, 12, true, false);
        newImage.copyRect(44, 20, -8, 32, 4, 12, true, false);
        newImage.copyRect(48, 20, -16, 32, 4, 12, true, false);
        newImage.copyRect(52, 20, -8, 32, 4, 12, true, false);

        setNoAlpha(newImage, 0, 0, 32, 16);
        setNoAlpha(newImage, 0, 16, 64, 32);
        setNoAlpha(newImage, 16, 48, 48, 64);

        return newImage;
    }

    private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
            }
        }
    }
}