package de.in.autoMower.sim;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class ZonePoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private Point2D point;
    private int percentage;

    public ZonePoint(Point2D point, int percentage) {
        this.point = point;
        this.percentage = percentage;
    }

    public Point2D getPoint() {
        return point;
    }

    public void setPoint(Point2D point) {
        this.point = point;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
