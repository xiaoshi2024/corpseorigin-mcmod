package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.growth.RealmNetworking;
import xiaoshi2022.corpseorigin.growth.RealmProgression;
import xiaoshi2022.corpseorigin.growth.RealmRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 6A 六维雷达面板：单屏同览六维养成（生命/杀伤/减伤/内力/再生/速度）。
 * 左侧六边形蛛网图按 F~A 六档评级绘制轮廓，右侧列出 rank 与最终加成；
 * 点击雷达顶点或右侧行选中属性，下方按钮对该属性加点。所有数值由服务端权威下发。
 */
public final class RealmGrowthScreen extends Screen {
    private static CompoundTag state=new CompoundTag();
    private static final String[] GRADES={"F","E","D","C","B","A","S","SS","SSS"};
    private static final int[] GRADE_COLORS={0xFFB0B0B0,0xFF9FD8A0,0xFF7FD4E8,0xFF8FB8FF,0xFFC79BFF,0xFFFFD77F,0xFFFFC94D,0xFFFFA726,0xFFFFD700};

    private final Screen parent;
    private int selected;
    private int refresh;
    private final List<Button> purchases=new ArrayList<>();

    // 雷达几何（init 时定，绘制/命中共用）
    private int cx, cy, radius;
    private final double[] vx=new double[6], vy=new double[6];          // 数据多边形顶点
    private static final double[] COS=new double[6], SIN=new double[6]; // 六顶点方向（-90° 起顺时针）

    static {
        for(int i=0;i<6;i++){double a=Math.toRadians(-90+60*i);COS[i]=Math.cos(a);SIN[i]=Math.sin(a);}
    }

    public RealmGrowthScreen(Screen parent){super(Component.translatable("realm.corpseorigin.title"));this.parent=parent;}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RealmNetworking.State.TYPE,(payload,context)->context.client().execute(()->{
            state=payload.data()==null?new CompoundTag():payload.data().copy();
            if(payload.open()) context.client().gui.setScreen(new RealmGrowthScreen(context.client().gui.screen()));
        }));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{
            state=new CompoundTag();ClientState.evolutionLevel=1;ClientState.pointsToNextLevel=0;
        });
    }
    public static void open(){ClientPlayNetworking.send(new RealmNetworking.Action("view","",0));}
    private void send(String action,String stat,int count){ClientPlayNetworking.send(new RealmNetworking.Action(action,stat,count));}
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.translatable(label),b->action.run()).bounds(x,y,w,20).build());}

    @Override protected void init(){
        int x=width/2-145; purchases.clear();
        cx=x+72; cy=116; radius=44;
        button("realm.corpseorigin.button.back",x+180,16,110,this::onClose);
        for(int i=0;i<3;i++){
            int count=new int[]{1,10,100}[i];
            purchases.add(addRenderableWidget(Button.builder(Component.translatable("realm.corpseorigin.train",count),b->send("train",RealmProgression.STATS.get(selected),count)).bounds(x+i*98,180,94,20).build()));
        }
        button("realm.corpseorigin.button.meditate",x,204,142,()->{send("meditate","",0);minecraft.gui.setScreen(null);});
        button("realm.corpseorigin.button.recharge",x+148,204,142,()->send("recharge","",0));
        button("realm.corpseorigin.button.blood",x,228,94,()->send("refine","blood",0));
        button("realm.corpseorigin.button.qi",x+98,228,94,()->send("refine","qi",0));
        button("realm.corpseorigin.button.medicine",x+196,228,94,()->send("refine","medicine",0));
        button("realm.corpseorigin.button.burst",x,252,142,()->{send("burst","",0);minecraft.gui.setScreen(null);});
    }
    @Override public void tick(){
        // 实时相对：面板打开期间每秒拉一次最新养成数据，等级/历练/点数变化立即反映
        if(++refresh>=20){refresh=0;send("refresh","",0);}
        String stat=RealmProgression.STATS.get(selected);
        int paid=state.getIntOr("rank_"+stat,0),total=state.getIntOr("total_"+stat,0);
        for(int i=0;i<purchases.size();i++){
            int count=new int[]{1,10,100}[i];long cost=RealmRules.cost(paid,count,state.getIntOr("base_cost",10),state.getIntOr("step_cost",2));
            purchases.get(i).active=state.getBooleanOr("enabled",false) && minecraft.player!=null && !minecraft.player.isCreative()
                    && total+count<=state.getIntOr("rank_limit",0) && cost<=state.getIntOr("available",0);
            purchases.get(i).setMessage(Component.translatable("realm.corpseorigin.price",count,cost));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
        super.extractRenderState(g,mx,my,pt);
        int x=width/2-145;
        g.centeredText(font,title,width/2,6,0xffffffff);
        g.centeredText(font,Component.translatable("realm.corpseorigin.panel6a.account",
                state.getStringOr("tier",""),state.getIntOr("available",0),state.getIntOr("earned",0),state.getIntOr("to_next",0)),
                width/2,44,0xffffd77f);

        boolean enabled=state.getBooleanOr("enabled",false);
        drawRadar(g,mx,my,enabled);
        drawList(g,mx,my,enabled);

        String stat=RealmProgression.STATS.get(selected);
        g.centeredText(font,Component.translatable("realm.corpseorigin.panel_practice",
                state.getLongOr("xp_"+stat,0),state.getIntOr("xp_per_rank",100)),cx,168,0xffaaccff);
        // 右下说明（崩星按钮右侧空间）
        int hintX=x+148;
        g.text(font,cut("realm.corpseorigin.panel_materials",138),hintX,254,0xff999999,false);
        g.text(font,cut("realm.corpseorigin.panel_restore",state.getIntOr("recharge_cost",50),138),hintX,264,0xff999999,false);
    }
    /** 6A 雷达图：六层网格（F→A）+ 轴线 + 数据轮廓 + 顶点标签 */
    private void drawRadar(GuiGraphicsExtractor g,int mx,int my,boolean enabled){
        for(int i=0;i<6;i++){
            double ratio=enabled?statRatio(i):0;
            vx[i]=cx+COS[i]*radius*ratio; vy[i]=cy+SIN[i]*radius*ratio;
        }
        for(int l=1;l<=9;l++){                       // 九层同心六边形：内 F → 外 SSS
            double r=radius*l/9.0;
            strokePoly(g,cx,cy,r,l==9?0xFF8A6D3B:0xFF31414F);
        }
        for(int i=0;i<6;i++) line(g,cx,cy,cx+COS[i]*radius,cy+SIN[i]*radius,0xFF22303C);
        fillConvex(g,vx,vy,0x5AFFD77F);              // 当前养成轮廓（半透明金）
        strokePoly(g,vx,vy,0xFFFFD77F);
        for(int i=0;i<6;i++){                        // 数据点 + 顶点标签（名 + 评级）
            int px=(int)Math.round(vx[i]),py=(int)Math.round(vy[i]);
            g.fill(px-1,py-1,px+1,py+1,0xFFFFFFFF);
            int lx=(int)Math.round(cx+COS[i]*(radius+13)),ly=(int)Math.round(cy+SIN[i]*(radius+13))-4;
            int grade=enabled?grade(i):0;
            g.centeredText(font,Component.translatable("realm.corpseorigin.stat_short."+RealmProgression.STATS.get(i))
                    .append(" "+GRADES[grade]),lx,ly,i==selected?0xFFFFD77F:GRADE_COLORS[grade]);
        }
        if(!enabled) g.centeredText(font,Component.translatable("realm.corpseorigin.panel6a.disabled"),cx,cy-4,0xFFFF5757);
    }
    /** 右侧六维数值列表：名 / 评级 / 总强化 / 最终加成 */
    private void drawList(GuiGraphicsExtractor g,int mx,int my,boolean enabled){
        int x0=width/2+16;
        for(int i=0;i<6;i++){
            int y=58+i*16; String stat=RealmProgression.STATS.get(i);
            boolean hover=mx>=x0-3&&mx<=x0+183&&my>=y-3&&my<=y+11;
            if(hover||i==selected) g.fill(x0-3,y-3,x0+183,y+11,i==selected?0x33FFD77F:0x22FFFFFF);
            int grade=enabled?grade(i):0;
            g.text(font,Component.translatable("realm.corpseorigin.stat_short."+stat),x0,y,i==selected?0xFFFFD77F:0xFFE0E0E0,false);
            g.text(font,Component.literal(GRADES[grade]),x0+30,y,GRADE_COLORS[grade],false);
            // 强化列按剩余空间截断，防止高等级长数值与右侧加成列重叠
            Component bonus=bonusText(i);
            int bonusW=font.width(bonus);
            String rankText=shortNum(state.getIntOr("total_"+stat,0)).getString()
                    +"/"+shortNum(statTarget(Math.max(1,state.getIntOr("level",1)))).getString();
            rankText=font.plainSubstrByWidth(rankText,Math.max(10,x0+183-bonusW-6-(x0+56)));
            g.text(font,Component.literal(rankText),x0+56,y,0xFF999999,false);
            g.text(font,bonus,x0+183-bonusW,y,0xFF7FD4FF,false);
        }
    }
    private Component bonusText(int i){
        double v=state.getDoubleOr("bonus_"+RealmProgression.STATS.get(i),0);
        return switch(i){
            case 0,1,3 -> Component.literal("+"+shortNum(v));
            case 2 -> Component.literal(String.format(Locale.ROOT,"%.1f%%",v*100));
            case 4 -> Component.literal(String.format(Locale.ROOT,"%.2f%%/s",v*100));
            default -> Component.literal(String.format(Locale.ROOT,"+%.1f%%",v*100));
        };
    }
    /** 6A 评判标准（相对制）：该维强化 / 动态目标（每级 300 强，随等级水涨船高）占 75%，等级占 25%；
     *  加点哪维哪维涨，六维轮廓立见差距，低等级也不会靠少量强化冲到 SSS */
    private double statRatio(int i){
        int level=Math.max(1,state.getIntOr("level",1)),limit=state.getIntOr("rank_limit",0);
        double rankRatio=limit>0?Math.min(1,state.getIntOr("total_"+RealmProgression.STATS.get(i),0)/(double)statTarget(level)):0;
        return rankRatio*.75 + Math.min(1,level/20.0)*.25;
    }
    private static int statTarget(int level){ return Math.max(300, level*300); }
    private int grade(int i){
        return (int)Math.min(8,statRatio(i)*8.9999);
    }
    /** 大数缩写：兆 / 亿 / 万 / 原值（避免高等级长数值撑爆面板列） */
    private static Component shortNum(double v){
        if(v>=1e12) return Component.translatable("realm.corpseorigin.number.trillion",String.format(Locale.ROOT,"%.2f",v/1e12));
        if(v>=1e8) return Component.translatable("realm.corpseorigin.number.hundred_million",String.format(Locale.ROOT,"%.2f",v/1e8));
        if(v>=1e4) return Component.translatable("realm.corpseorigin.number.ten_thousand",String.format(Locale.ROOT,"%.1f",v/1e4));
        return Component.literal(String.valueOf((long)v));
    }
    private Component cut(String key,int arg,int maxWidth){
        return Component.literal(font.plainSubstrByWidth(Component.translatable(key,arg).getString(),maxWidth));
    }
    private Component cut(String key,int maxWidth){
        return Component.literal(font.plainSubstrByWidth(Component.translatable(key).getString(),maxWidth));
    }
    // ---------- 雷达绘制基元（点阵法，同 NestRadarScreen 惯例） ----------
    private void line(GuiGraphicsExtractor g,double x1,double y1,double x2,double y2,int color){
        int steps=(int)Math.max(Math.abs(x2-x1),Math.abs(y2-y1));
        for(int i=0;i<=steps;i++){
            double t=steps==0?0:(double)i/steps;
            int px=(int)Math.round(x1+(x2-x1)*t),py=(int)Math.round(y1+(y2-y1)*t);
            g.fill(px,py,px+1,py+1,color);
        }
    }
    private void strokePoly(GuiGraphicsExtractor g,double ox,double oy,double r,int color){
        for(int i=0;i<6;i++){int j=(i+1)%6;line(g,ox+COS[i]*r,oy+SIN[i]*r,ox+COS[j]*r,oy+SIN[j]*r,color);}
    }
    private void strokePoly(GuiGraphicsExtractor g,double[] xs,double[] ys,int color){
        for(int i=0;i<6;i++){int j=(i+1)%6;line(g,xs[i],ys[i],xs[j],ys[j],color);}
    }
    /** 凸多边形扫描线填充（六边形数据轮廓专用） */
    private void fillConvex(GuiGraphicsExtractor g,double[] xs,double[] ys,int color){
        int minY=(int)Math.ceil(min(ys)),maxY=(int)Math.floor(max(ys));
        for(int y=minY;y<=maxY;y++){
            Double lo=null,hi=null;
            for(int i=0;i<6;i++){
                int j=(i+1)%6;double y1=ys[i],y2=ys[j];
                if(y1!=y2&&y>=Math.min(y1,y2)&&y<=Math.max(y1,y2)){
                    double x=xs[i]+(xs[j]-xs[i])*(y-y1)/(y2-y1);
                    if(lo==null||x<lo)lo=x; if(hi==null||x>hi)hi=x;
                }
            }
            if(lo!=null&&hi!=null) g.fill((int)Math.round(lo),y,(int)Math.round(hi)+1,y+1,color);
        }
    }
    private static double min(double[] a){double m=a[0];for(double v:a)if(v<m)m=v;return m;}
    private static double max(double[] a){double m=a[0];for(double v:a)if(v>m)m=v;return m;}
    // ---------- 交互：点击雷达顶点 / 右侧行选中 ----------
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){
            for(int i=0;i<6;i++){
                int px=(int)Math.round(cx+COS[i]*radius),py=(int)Math.round(cy+SIN[i]*radius);
                if(Math.pow(e.x()-px,2)+Math.pow(e.y()-py,2)<=100){selected=i;return true;}
            }
            int x0=width/2+16;
            for(int i=0;i<6;i++){
                int y=58+i*16;
                if(e.x()>=x0-3&&e.x()<=x0+183&&e.y()>=y-3&&e.y()<=y+11){selected=i;return true;}
            }
        }
        return super.mouseClicked(e,doubled);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
