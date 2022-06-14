package hu.hero.landar.database;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import hu.hero.landar.MainActivity;


public class PICDATA{
    private String office;
    private String sectnum;
    private String ptnum;
    private double coordx;
    private double coordy;
    private String surveyor;
    private int    pictime;
    private int    uploadtime=0;
    private String filename;

    public PICDATA(){
    }

    public PICDATA(PICDATA a ){
        copy(a);
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
    public void setPtnum(int ptnum) {
        setPtnum( String.format("%05d",ptnum));
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

    public int getPictime() {
        return pictime;
    }

    public void setPictime(int pictime) {
        this.pictime = pictime;
    }

    public int getUploadtime() {
        return uploadtime;
    }

    public void setUploadtime(int uploadtime) {
        this.uploadtime = uploadtime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPicDateString(){
        long T = (long)getPictime()*1000;
        Date date = new Date(T);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Taipei")); // 設定時區
        calendar.setTime(date);
        return String.format("%d年%d月%d日",calendar.get(Calendar.YEAR)-1911,
                                                   calendar.get(Calendar.MONTH)+1,
                                                   calendar.get(Calendar.DATE) );
    }

    public String getPicTimeString(){
        long T = (long)getPictime()*1000;
        Date date = new Date(T);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Taipei")); // 設定時區
        calendar.setTime(date);
        return String.format("%d年%d月%d日 %d時%d分%d秒",calendar.get(Calendar.YEAR)-1911,
                calendar.get(Calendar.MONTH)+1,
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
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
                ", pictime=" + pictime +
                ", uploadtime=" + uploadtime +
                ", filename='" + filename + '\'' +
                '}';
    }

    public void copy( PICDATA p ){
        setOffice(p.getOffice());
        setSectnum(p.getSectnum());
        setPtnum(p.getPtnum());
        setFilename((p.getFilename()));
        setSurveyor(p.getSurveyor());
        setCoordx(p.getCoordx());
        setCoordy(p.getCoordy());
        setPictime(p.getPictime());
        setUploadtime(getUploadtime());
    }
    public PICDATA(Map<String, Object> data) {
        office = (String)data.get("OFFICE");
        sectnum = (String)data.get("SECTNUM");
        ptnum = (String)data.get("PTNUM");
        coordx = (double)data.get("COORDX");
        coordy = (double)data.get("COORDY");
        surveyor = (String)data.get("SURVEYOR");
        pictime = (int)data.get("PICTIME");
        uploadtime = (int)data.get("UPLOADTIME");
        filename = (String)data.get("FILENAME");
    }
}
