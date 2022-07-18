package hu.hero.landar.database;

import java.util.Date;
import java.util.Map;

public class PICDATA1 {
    private String office;
    private String sectnum;
    private String ptnum;
    private double coordx;
    private double coordy;
    private String surveyor;
    private String pictime;
    private String uploadtime;
    private String filename;

    public PICDATA1(){}

    public PICDATA1(PICDATA pic ){
        setOffice(pic.getOffice());
        setSectnum(pic.getSectnum());
        setPtnum(pic.getPtnum());
        setCoordx(pic.getCoordx());
        setCoordy(pic.getCoordy());
        setSurveyor(pic.getSurveyor());
        setPictime(formatTime(pic.getPictime()));
        setUploadtime(formatTime(pic.getUploadtime()));
        setFilename(pic.getFilename());
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public String getSectnum() {
        return sectnum;
    }

    public void setSectnum(String sectnum) {
        this.sectnum = sectnum;
    }

    public String getPtnum() {
        return ptnum;
    }

    public void setPtnum(String ptnum) {
        this.ptnum = ptnum;
    }

    public double getCoordx() {
        return coordx;
    }

    public void setCoordx(double coordx) {
        this.coordx = coordx;
    }

    public double getCoordy() {
        return coordy;
    }

    public void setCoordy(double coordy) {
        this.coordy = coordy;
    }

    public String getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(String surveyor) {
        this.surveyor = surveyor;
    }

    public String getPictime() {
        return pictime;
    }

    public void setPictime(String pictime) {
        this.pictime = pictime;
    }

    public String getUploadtime() {
        return uploadtime;
    }

    public void setUploadtime(String uploadtime) {
        this.uploadtime = uploadtime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        return "{" +
                "office='" + office + '\'' +
                ", sectnum='" + sectnum + '\'' +
                ", ptnum='" + ptnum + '\'' +
                ", coordx=" + coordx +
                ", coordy=" + coordy +
                ", surveyor='" + surveyor + '\'' +
                ", pictime='" + pictime + '\'' +
                ", uploadtime=" + uploadtime +
                ", filename='" + filename + '\'' +
                '}';
    }

    public PICDATA1(Map<String, Object> data) {
        office = (String)data.get("OFFICE");
        sectnum = (String)data.get("SECTNUM");
        ptnum = (String)data.get("PTNUM");
        coordx = (double)data.get("COORDX");
        coordy = (double)data.get("COORDY");
        surveyor = (String)data.get("SURVEYOR");
        pictime = (String)data.get("PICTIME");
        uploadtime = (String)data.get("UPLOADTIME");
        filename = (String)data.get("FILENAME");
    }

    public static String formatTime( int time ){
        if( time == 0 ) {
            return "";
        }
        Date now = new Date();
        long diff = now.getTime()-(long)time*1000;

        int days = (int)Math.floor(diff / (1000 * 60 * 60 * 24));
        diff -=  days * (1000 * 60 * 60 * 24);

        int hours = (int)Math.floor(diff / (1000 * 60 * 60));
        diff -= hours * (1000 * 60 * 60);

        int mins = (int)Math.floor(diff / (1000 * 60));
        diff -= mins * (1000 * 60);

        int seconds = (int)Math.floor(diff / (1000));
        diff -= seconds * (1000);

        String out;
        if( days >= 365 )
            out=(int)Math.floor(days/365)+"年前";
        else if( days >= 1 )
            out=(int)days+"天前";
        else if( hours >= 1 )
            out=(int)hours+"小時前";
        else if( mins >= 1 )
            out = (int)mins+"分鐘前";
        else
            out = "剛剛";
        return out;
    }
}
