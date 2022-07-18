package hu.hero.landar.database;

import hu.hero.landar.database.PICDATA;

public class PICDATA3D extends PICDATA1 {
    private double elevation;

    public PICDATA3D(PICDATA pic , double e ){
        setOffice(pic.getOffice());
        setSectnum(pic.getSectnum());
        setPtnum(pic.getPtnum());
        setCoordx(pic.getCoordx());
        setCoordy(pic.getCoordy());
        setSurveyor(pic.getSurveyor());
        setPictime(formatTime(pic.getPictime()));
        setUploadtime(formatTime(pic.getUploadtime()));
        setFilename(pic.getFilename());
        setElevation(e);
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
}
