import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Offline PNG export; original art is never packaged at generation resolution. */
class PrepareSkinAsset {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("source destination width height");
        var source = ImageIO.read(Path.of(args[0]).toFile());
        int width = Integer.parseInt(args[2]);
        int height = Integer.parseInt(args[3]);
        var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = output.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        var destination = Path.of(args[1]);
        Files.createDirectories(destination.getParent());
        ImageIO.write(output, "png", destination.toFile());
        System.out.printf("%s: %dx%d, %,d bytes; decoded ARGB: %,d bytes%n",
                destination, width, height, Files.size(destination), width * height * 4);
    }
}
