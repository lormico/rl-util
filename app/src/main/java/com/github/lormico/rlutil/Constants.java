package com.github.lormico.rlutil;

import java.time.format.DateTimeFormatter;

public abstract class Constants {

    public static final String ONE_LINER = "oneliner";
    public static final String NORTHBOUND = "northbound";
    public static final String SOUTHBOUND = "southbound";
    public static final String SERVICE_STATUS = "service_status";

    public static final String WEEKDAY = "weekday";
    public static final String SATURDAY = "saturday";
    public static final String HOLIDAY = "holiday";

    public static final String WEBPAGE = "https://www.atac.roma.it/function/pg_news.asp?act=3&r=16616&p=159";

    public static final int DEPARTURES_UPDATE_INTERVAL = 150000;
    public static final int NOTIFICATION_UPDATE_INTERVAL = 60000;

    public static final DateTimeFormatter HMF = DateTimeFormatter.ofPattern("HH:mm");

}
