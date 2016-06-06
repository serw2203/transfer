package ru.transfer.dao;

import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 *
 */
public interface AnalyticalDao {
    List<Currency> currencies(Jdbc jdbc) throws Exception;

    Client client(Jdbc jdbc, Long clientId) throws Exception;

    List<Account> accounts(Jdbc jdbc, Long clientId) throws Exception;

    Account account(Jdbc jdbc, String accNum) throws Exception;

    Rate rate (Jdbc jdbc,String sCur, String tCur, Timestamp dateRate) throws Exception;

    List<Rate> rates (Jdbc jdbc,Timestamp dateRate) throws Exception;

    BigDecimal saldo (Jdbc jdbc,String accNum, String curCode) throws Exception;

    List<Balance> balance(Jdbc jdbc, String accNum, Timestamp date) throws Exception;

    List<Balance> balance(Jdbc jdbc, String accNum, Timestamp dat, boolean includeDate) throws Exception;

    List<Extract> extracts (Jdbc jdbc, String accNum, Timestamp startDate, Timestamp stopDate) throws Exception;
 }
