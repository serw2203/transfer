package ru.transfer.service;

import ru.transfer.dao.AnalyticalDao;
import ru.transfer.dao.AnalyticalDaoImpl;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 *
 */
public class AnalyticalServiceImpl implements AnalyticalService {

    private final AnalyticalDao analyticalDao = new AnalyticalDaoImpl();

    @Override
    public CurrencyRoot currencies() throws Exception {
        CurrencyRoot root = new CurrencyRoot();
        root.getCurrencies().addAll(this.analyticalDao.currencies(new Jdbc()));
        return root;
    }

    @Override
    public Client client(Long clientId) throws Exception {
       return this.analyticalDao.client(new Jdbc(), clientId);
    }

    @Override
    public AccountRoot accounts(Long clientId) throws Exception {
        AccountRoot root = new AccountRoot();
        root.getAccounts().addAll(this.analyticalDao.accounts(new Jdbc(), clientId));
        return root;
    }

    @Override
    public Account account(String accNum) throws Exception {
        return this.analyticalDao.account(new Jdbc(), accNum);
    }

    @Override
    public Rate rate(String sCur, String tCur, Timestamp dateRate) throws Exception {
        return this.analyticalDao.rate(new Jdbc(), sCur, tCur, dateRate);
    }

    @Override
    public RateRoot rates(Timestamp dateRate) throws Exception {
        RateRoot root = new RateRoot();
        root.getRates().addAll(this.analyticalDao.rates(new Jdbc(), dateRate));
        return root;
    }

    @Override
    public BigDecimal saldo (String accNum, String curCode) throws Exception{
        return this.analyticalDao.saldo(new Jdbc(), accNum, curCode);
    }

    @Override
    public BalanceRoot balance(String accNum, Timestamp date) throws Exception {
        BalanceRoot balances = new BalanceRoot();
        balances.getBalances().addAll(this.analyticalDao.balance(new Jdbc(), accNum, date));
        return balances;
    }

    @Override
    public ExtractRoot extracts(String accNum, Timestamp startDate, Timestamp stopDate) throws Exception {
        ExtractRoot extracts = new ExtractRoot();
        Jdbc jdbc = new Jdbc();
        jdbc.createTrans();
        try {
            extracts.getInput().addAll(this.analyticalDao.balance(jdbc, accNum, startDate, false));
            extracts.getOutput().addAll(this.analyticalDao.balance(jdbc, accNum, stopDate));
            extracts.getExtracts().addAll(this.analyticalDao.extracts(jdbc, accNum, startDate, stopDate));
            jdbc.commitTrans();
            return extracts;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            e.printStackTrace();
            throw e;
        }
    }
}
