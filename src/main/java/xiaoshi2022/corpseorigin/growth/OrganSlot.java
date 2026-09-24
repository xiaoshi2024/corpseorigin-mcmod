package xiaoshi2022.corpseorigin.growth;

/** Pixel offsets and degrees in the chosen joint's local space. */
public record OrganSlot(String organ, String joint, float x, float y, float z,
                        float rx, float ry, float rz, float scale, boolean mirror) {
    public static final java.util.List<String> JOINTS = java.util.List.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg", "full_body");
    public boolean replacesBody() { return "full_body".equals(joint); }
    public boolean valid() {
        return organ != null && organ.length() <= 96 && JOINTS.contains(joint)
                && bounded(x, 48) && bounded(y, 48) && bounded(z, 48)
                && bounded(rx, 180) && bounded(ry, 180) && bounded(rz, 180)
                && Float.isFinite(scale) && scale >= .1f && scale <= 3;
    }
    private static boolean bounded(float v, float max) { return Float.isFinite(v) && Math.abs(v) <= max; }
}
