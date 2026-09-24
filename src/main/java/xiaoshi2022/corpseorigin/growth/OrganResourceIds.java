package xiaoshi2022.corpseorigin.growth;

/** Catalogs store physical file paths; GeckoLib caches use relative, extension-free IDs. */
public final class OrganResourceIds {
    private OrganResourceIds() {}
    public static String model(String file) { return strip(file,"geckolib/models/",".geo.json"); }
    public static String animation(String file) { return strip(file,"geckolib/animations/",".animation.json"); }
    private static String strip(String file,String prefix,String suffix) {
        int colon=file.indexOf(':');
        String namespace=colon<0?"minecraft":file.substring(0,colon);
        String path=file.substring(colon+1);
        if(!path.startsWith(prefix)||!path.endsWith(suffix))throw new IllegalArgumentException("Invalid organ resource: "+file);
        return namespace+":"+path.substring(prefix.length(),path.length()-suffix.length());
    }
}
