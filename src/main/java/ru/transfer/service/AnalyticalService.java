package ru.transfer.service;

import ru.transfer.model.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 *
 */
public interface AnalyticalService {
    CurrencyRoot currencies() throws Exception;

    Client client(Long clientId) throws Exception;

    AccountRoot accounts(Long clientId) throws Exception;

    Account account (String accNum) throws Exception;

    Rate rate (String sCur, String tCur, Timestamp dateRate) throws Exception;

    RateRoot rates (Timestamp dateRate) throws Exception;

    BigDecimal saldo (String accNum, String curCode) throws Exception;

    BalanceRoot balance (String accNum, Timestamp date) throws Exception;

    ExtractRoot extracts (String accNum, Timestamp startDate, Timestamp stopDate) throws Exception;
}
