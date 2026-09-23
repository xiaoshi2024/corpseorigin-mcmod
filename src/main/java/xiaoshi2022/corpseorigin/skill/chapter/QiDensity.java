package xiaoshi2022.corpseorigin.skill.chapter;

/** Smooth radial density shared by the mist and its camera fog. */
public final class QiDensity {
    private QiDensity() {}
    public static float at(double distance,double radius,float fade){
        if(!Double.isFinite(distance) || !Double.isFinite(radius) || radius<=0 || !Float.isFinite(fade))return 0;
        double edge=Math.max(0,1-Math.max(0,distance)/radius);
        return (float)(edge*edge*Math.clamp(fade,0,1));
    }
}
