package de.in.autoMower.sim;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectData implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PointDTO {
        public double x;
        public double y;

        public PointDTO() {
        }

        public PointDTO(Point2D p) {
            this.x = p.getX();
            this.y = p.getY();
        }

        public Point2D toPoint() {
            return new Point2D.Double(x, y);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZonePointDTO implements Serializable {
        public double x;
        public double y;
        public int percentage;

        public ZonePointDTO() {
        }

        public ZonePointDTO(ZonePoint zp) {
            this.x = zp.getPoint().getX();
            this.y = zp.getPoint().getY();
            this.percentage = zp.getPercentage();
        }

        public ZonePoint toZonePoint() {
            return new ZonePoint(new Point2D.Double(x, y), percentage);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MultiLineDTO {
        public List<PointDTO> points;
        public int colorRGB;
        public boolean closed;

        public MultiLineDTO() {
        }

        public MultiLineDTO(MultiLine2D ml) {
            this.points = ml.points.stream().map(PointDTO::new).collect(Collectors.toList());
            this.colorRGB = ml.color.getRGB();
            this.closed = ml.closed;
        }

        public MultiLine2D toMultiLine() {
            MultiLine2D ml = new MultiLine2D(new java.awt.Color(colorRGB));
            if (points != null) {
                for (PointDTO p : points) {
                    ml.addPoint(p.toPoint());
                }
            }
            if (closed)
                ml.closePath();
            return ml;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MowerDTO {
        public double speedInCmPerSec;
        public double mowingWidthInCm;
        public double batteryCapacityWh = 50.0;
        public double energyConsumptionWhPerCm = 0.00045;
        public double chargeRateWhPerSec = 0.02;
        public PointDTO currentPosition;

        public int version = 1; // Default to 1

        public MowerDTO() {
        }

        public MowerDTO(AbstractAutoMowerModel mower) {
            this.speedInCmPerSec = mower.getSpeedInCmPerSec();
            this.mowingWidthInCm = mower.getMowingWidthInCm();
            this.batteryCapacityWh = mower.batteryCapacityWh;
            this.energyConsumptionWhPerCm = mower.energyConsumptionWhPerCm;
            this.chargeRateWhPerSec = mower.chargeRateWhPerSec;
            if (mower.getCurrentPosition() != null) {
                this.currentPosition = new PointDTO(mower.getCurrentPosition());
            }
            this.version = mower.getModelVersion();
        }
    }

    public MultiLineDTO border;
    public List<MultiLineDTO> obstacles;
    public double calibration;
    public MowerDTO mower;
    public String backgroundImageBase64; // PNG image as Base64 string
    public PointDTO chargingStation;
    public List<ZonePointDTO> zonePoints;

    public ProjectData() {
    }
}
