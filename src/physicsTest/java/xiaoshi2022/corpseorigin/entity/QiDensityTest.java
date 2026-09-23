package xiaoshi2022.corpseorigin.entity;

import xiaoshi2022.corpseorigin.skill.chapter.QiDensity;

public final class QiDensityTest {
    public static void main(String[] args){
        check(QiDensity.at(0,4,1),1);
        check(QiDensity.at(2,4,1),.25f);
        check(QiDensity.at(4,4,1),0);
        check(QiDensity.at(8,4,1),0);
        check(QiDensity.at(0,4,.5f),.5f);
        check(QiDensity.at(0,0,1),0);
        check(QiDensity.at(Double.NaN,4,1),0);
        check(QiDensity.at(0,4,0),0);
        System.out.println("Qi density: 8 regression checks passed.");
    }
    private static void check(float value,float expected){
        if(!Float.isFinite(value) || Math.abs(value-expected)>1e-6)throw new AssertionError(value+" != "+expected);
    }
}
