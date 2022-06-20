package hu.hero.landar.Geo;

public class Point3 {
    public double x;
    public double y;
    public double h;
    public Point3( double x , double y , double h ){
        this.x = x;
        this.y = y;
        this.h = h;
    }
    public Point3( double x , double y ){
        this.x = x;
        this.y = y;
        this.h = 0;
    }
}
