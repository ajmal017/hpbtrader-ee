package com.highpowerbear.hpbtrader.shared.entity;

import com.highpowerbear.hpbtrader.shared.common.HtrUtil;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.Calendar;

/**
 *
 * @author rkolar
 */
@XmlAccessorType(XmlAccessType.NONE)
@Entity
@Table(name = "bar", uniqueConstraints = @UniqueConstraint(columnNames = {"qDateBarClose", "series_id"}))
public class Bar implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableGenerator(name="bar", table="sequence", pkColumnName="seq_name", valueColumnName="seq_count")
    @Id
    @GeneratedValue(generator="bar")
    private Long id;
    @Temporal(value=TemporalType.TIMESTAMP)
    private Calendar qDateBarClose;
    @XmlElement
    private Double qOpen;
    @XmlElement
    private Double high;
    @XmlElement
    private Double low;
    @XmlElement
    private Double qClose;
    @XmlElement
    private Integer volume;
    private Integer count;
    private Double wap;
    private Boolean hasGaps;
    @ManyToOne
    private Series series;
    
    @XmlElement
    public long getTimeInMillisBarClose() {
        return qDateBarClose.getTimeInMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bar bar = (Bar) o;

        return !(id != null ? !id.equals(bar.id) : bar.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    
    public Boolean getHasGaps() {
        return hasGaps;
    }

    public void setHasGaps(Boolean hasGaps) {
        this.hasGaps = hasGaps;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getqClose() {
        return qClose;
    }

    public void setqClose(Double qClose) {
        this.qClose = qClose;
    }
    
    public Calendar getqDateBarClose() {
        return qDateBarClose;
    }

    public void setqDateBarClose(Calendar qDateBarClose) {
        this.qDateBarClose = qDateBarClose;
    }
    
    public Double getqOpen() {
        return qOpen;
    }

    public void setqOpen(Double qOpen) {
        this.qOpen = qOpen;
    }
    
    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }
    
    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }
    
    public Double getWap() {
        return wap;
    }

    public void setWap(Double wap) {
        this.wap = wap;
    }
    
    public boolean valuesEqual(Bar otherBar) {
        if (otherBar == null) {
            return false;
        }
        if (    this.qOpen.equals(otherBar.getqOpen()) &&
                this.high.equals(otherBar.getHigh()) &&
                this.low.equals(otherBar.getLow()) &&
                this.qClose.equals(otherBar.getqClose()) &&
                this.volume.equals(otherBar.getVolume()) &&
                this.count.equals(otherBar.getCount()) &&
                this.wap.equals(otherBar.getWap()) &&
                this.hasGaps.equals(otherBar.getHasGaps())
           )
        {
            return true;
        }
        return false;
    }
    
    public void copyValuesFrom(Bar otherBar) {
        if (otherBar == null) {
            return;
        }
        this.qOpen = otherBar.getqOpen();
        this.high = otherBar.getHigh();
        this.low = otherBar.getLow();
        this.qClose = otherBar.getqClose();
        this.volume = otherBar.getVolume();
        this.count = otherBar.getCount();
        this.wap = otherBar.getWap();
        this.hasGaps = otherBar.getHasGaps();
    }
    
    public String printValues() {
        return series.getSymbol() + ": " + HtrUtil.getFormattedDate(qDateBarClose) + ", " + qOpen + ", " + high + ", " + low + ", " + qClose + ", " + volume + ", " + count + ", " + hasGaps;
    }
}
