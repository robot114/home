package com.zsm.location;

public class GeodeticCoordinate {

	private static final double DISTANCE_TOILANCE = 1e-6;	// About 0.1m

	public static class CoordinateData {
		public double latitude;
		public double longitude;

		public CoordinateData( double latitude, double longitude ) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		public CoordinateData() {
		}
	}

	public enum TYPE { 
		WGS84,		// World Geodetic System 84, equals to GPS or Beidou. Google Earth use this.
		GCJ02,		// Guo Ce Ju 02, only used in China. AMap, Google Map in China, use this
		BD09		// Baidu, only used by BaiduSystem
	}

	private CoordinateData data = new CoordinateData();
	private TYPE type;
	
	public GeodeticCoordinate( double latitude, double longitude, TYPE type ) {
		setData( latitude, longitude, type );
	}
	
	public GeodeticCoordinate( GeodeticCoordinate gc ) {
		this( gc.data.latitude, gc.data.longitude, gc.type );
	}
	
	public void setData( double latitude, double longitude, TYPE type ) {
		data.latitude = latitude;
		data.longitude = longitude;
		this.type = type;
	}
	
	public void transformTo( TYPE target ) {
		if( type == TYPE.WGS84 && target == TYPE.GCJ02 ) {
			wgs84ToGcj02();
		} else if( type == TYPE.WGS84 && target == TYPE.BD09 ) {
			wgs84ToBd09();
		} else if( type == TYPE.GCJ02 && target == TYPE.WGS84 ) {
			gcj02ToWgs84();
		} else if( type == TYPE.GCJ02 && target == TYPE.BD09 ) {
			gcj02ToBd09();
		} else if( type == TYPE.BD09 && target == TYPE.WGS84 ) {
			bd09ToWgs84();
		} else if( type == TYPE.BD09 && target == TYPE.GCJ02 ) {
			bd09ToGcj02();
		}
		
		type = target;
	}
	
	public TYPE getType() {
		return type;
	}
	
	public double getLatitude() {
		return data.latitude;
	}
	
	public double getLongitude() {
		return data.longitude;
	}

	@Override
	public boolean equals(Object o) {
		if( this == o ) {
			return true;
		}
		
		if( o == null ) {
			return false;
		}
		
		GeodeticCoordinate gc = (GeodeticCoordinate)o;
		if( type == gc.type ) {
			return closeEnough(gc);
		}
		
		GeodeticCoordinate newGc = new GeodeticCoordinate(gc);
		newGc.transformTo(type);
		
		return closeEnough(newGc);
	}

	private boolean closeEnough(GeodeticCoordinate gc) {
		return Math.abs( data.latitude - gc.data.latitude ) < DISTANCE_TOILANCE
				&& Math.abs( data.longitude - gc.data.longitude ) < DISTANCE_TOILANCE;
	}

	@Override
	public int hashCode() {
		long h1 = (long)Double.valueOf(data.latitude).hashCode();
		long h2 = (long)Double.valueOf(data.longitude).hashCode();
		long h3 = (long)type.ordinal();
		return (int)(h1 ^ h2 ^ h3);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		
		buf.append( "(" )
		   .append( data.latitude )
		   .append( ", " )
		   .append( data.longitude )
		   .append( ", " )
		   .append( type )
		   .append( ")" );
		return buf.toString();
	}
	
    //
    // Krasovsky 1940
    //
    // a = 6378245.0, 1/f = 298.3
    // b = a * (1 - f)
    // ee = (a^2 - b^2) / a^2;
    public static final double pi = 3.1415926535897932384626;
    public static final double a = 6378245.0;
    public static final double ee = 0.00669342162296594323;

    private void wgs84ToGcj02( ) {
        if (outOfChina(data.latitude, data.longitude)) {
            return;
        }
        double dLat = transformLat(data.longitude - 105.0, data.latitude - 35.0);
        double dLon = transformLon(data.longitude - 105.0, data.latitude - 35.0);
        double radLat = data.latitude / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        data.latitude = data.latitude + dLat;
        data.longitude = data.longitude + dLon;
    }

    private void gcj02ToWgs84() {
    	CoordinateData cd = transform(data.latitude, data.longitude);
        data.latitude = data.latitude * 2 - cd.latitude;
        data.longitude = data.longitude * 2 - cd.longitude;
    }

    private void gcj02ToBd09() {
        double x = data.longitude, y = data.latitude;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * pi);
        data.longitude = z * Math.cos(theta) + 0.0065;
        data.latitude = z * Math.sin(theta) + 0.006;
    }

    private void bd09ToGcj02() {
        double x = data.longitude - 0.0065, y = data.latitude - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * pi);
        data.longitude = z * Math.cos(theta);
        data.latitude = z * Math.sin(theta);
    }

    private void bd09ToWgs84() {
        bd09ToGcj02();
        gcj02ToWgs84();
    }

    private void wgs84ToBd09() {
    	wgs84ToGcj02();
    	gcj02ToBd09();
    }

    private boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    private CoordinateData transform(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new CoordinateData(lat, lon);
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new CoordinateData(mgLat, mgLon);
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
}
