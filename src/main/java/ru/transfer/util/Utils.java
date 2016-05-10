package ru.transfer.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class Utils {

    public static XMLGregorianCalendar convertDate(Date date) throws DatatypeConfigurationException {
        if (date == null) {
            return null;
        } else {
            GregorianCalendar gregory = new GregorianCalendar();
            gregory.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregory);
        }
    }
}
