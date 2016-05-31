package ru.transfer.init;

import ru.transfer.conf.Config;
import ru.transfer.query.BatchQueries;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 *
 */
public class DdlBatchQueries implements BatchQueries {

    private final static String[] DDL_QUERIES = new String[]{
            "create sequence seq_id start with 1;",

            "create table aaa_client ( " +
                    "client_id bigint primary key);",

            "insert into aaa_client values (0);",

            "create table aaa_h_client ( " +
                    "h_client_id bigint default seq_id.nextval primary key, " +
                    "client_id bigint not null, " +
                    "last_name varchar(48), " +
                    "first_name varchar(48), " +
                    "middle_name varchar(48), " +
                    "modify_date timestamp not null, " +
                    "cli_version int not null);",

            "insert into aaa_h_client (h_client_id, client_id, last_name, modify_date, cli_version) " +
                    "values (0, 0, '" + Config.OWNER + "', '1900-01-01 00:00:00.0', 0);",

            "alter table aaa_h_client add constraint f_h_client_on_client_client_id foreign key (client_id) " +
                    "references aaa_client (client_id);",

            "create unique index ix_h_client_client_current on aaa_h_client (client_id, cli_version);",

            "create index ix_h_client_mdate on aaa_h_client (modify_date desc);",

            "create table aaa_account (" +
                    "acc_id bigint default seq_id.nextval primary key," +
                    "client_id bigint not null," +
                    "acc_num varchar(20) not null);",

            "alter table aaa_account add constraint un_acc_num unique (acc_num);",

            "alter table aaa_account add constraint f_account_on_client_client_id foreign key (client_id) " +
                    "references aaa_client (client_id);",

            "insert into aaa_account values (0, 0, '" + Config.ACCOWNER + "');",

            "create table aaa_currency ( cur_code varchar(5) primary key);",

            "insert into aaa_currency (cur_code) select cur_code from ( " +
                    "select 'RUB' as cur_code union " +
                    "select 'USD' as cur_code union " +
                    "select 'EUR' as cur_code union " +
                    "select 'GBP' as cur_code union " +
                    "select 'CHF' as cur_code );",

            "create table aaa_oper_type ( oper_type varchar(8) not null primary key, oper_name varchar (128));",

            "insert into aaa_oper_type select oper_type, oper_name from ( " +
                    "select 'INIT' as oper_type, 'Инициирование остатков' as oper_name union " +
                    "select 'INPUT' as oper_type, 'Ввод денежных средств' as oper_name union " +
                    "select 'OUTPUT' as oper_type, 'Вывод денежных средств' union " +
                    "select 'TRANSFER' as oper_type, 'Перевод денежных средств на другой счет');",

            "create table aaa_oper ( " +
                    "oper_id bigint primary key, " +
                    "h_client_id  bigint not null, " +
                    "oper_date timestamp not null, " +
                    "oper_type varchar(12) not null, " +
                    "oper_acc_id bigint not null, " +
                    "oper_cur_code varchar(5) not null, " +
                    "rate decimal(18, 8) not null);",

            "alter table aaa_oper add constraint f_oper_on_oper_type_oper_type foreign key (oper_type) " +
                    "references aaa_oper_type (oper_type);",

            "alter table aaa_oper add constraint f_oper_on_h_client_h_client_id foreign key (h_client_id) " +
                    "references aaa_h_client (h_client_id);",

            "alter table aaa_oper add constraint f_oper_on_acc_oper_acc_id foreign key (oper_acc_id) " +
                    "references aaa_account (acc_id);",

            "alter table aaa_oper add constraint f_oper_on_curr_oper_cur_code foreign key (oper_cur_code) " +
                    "references aaa_currency (cur_code);",

            "create index ix_oper_date on aaa_oper (oper_date desc);",

            "create table aaa_turn ( " +
                    "turn_id bigint default seq_id.nextval primary key, " +
                    "oper_id bigint not null, " +
                    "acc_id bigint not null, " +
                    "cur_code varchar(5) not null, " +
                    "d_amount decimal(18,2) default 0 not null, " +
                    "k_amount decimal(18,2) default 0 not null, " +
                    "turn_date timestamp not null);",

            "alter table aaa_turn add constraint f_turn_on_acc_acc_id foreign key (acc_id) " +
                    "references aaa_account (acc_id);",

            "alter table aaa_turn add constraint f_turn_on_curr_cur_code foreign key (cur_code) " +
                    "references aaa_currency (cur_code);",

            "alter table aaa_turn add constraint f_turn_on_oper_oper_id foreign key (oper_id) " +
                    "references aaa_oper (oper_id);",

            "create index ix_turn_acc_cur on aaa_turn (acc_id, cur_code);",

            "create index ix_turn_date on aaa_turn (turn_date desc);",

            "create table aaa_balance ( " +
                    "acc_id bigint not null, " +
                    "cur_code varchar(5) not null, " +
                    "balance decimal(18,2) default 0 not null);",

            "create unique index ix_balance_acc_cur on aaa_balance (acc_id, cur_code);",

            "alter table aaa_balance add constraint f_balance_on_acc_acc_id foreign key (acc_id) " +
                    "references aaa_account (acc_id);",

            "alter table aaa_balance add constraint f_balance_on_curr_cur_code foreign key (cur_code) " +
                    "references aaa_currency (cur_code);",

            "create table aaa_cross_rate (" +
                    "scur_code varchar(5) not null," +
                    "tcur_code varchar(5) not null, " +
                    "date_rate date not null, " +
                    "rate decimal(18,8) default 0 not null);",

            "alter table aaa_cross_rate add constraint f_rate_on_scurr_cur_code foreign key (scur_code) " +
                    "references aaa_currency (cur_code);",

            "alter table aaa_cross_rate add constraint f_rate_on_tcurr_cur_code foreign key (tcur_code) " +
                    "references aaa_currency (cur_code);",

            "create index ix_rate_date on aaa_cross_rate (date_rate desc);"
    };

    @Override
    public Statement createStatement (Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        for (String query : DDL_QUERIES) {
            statement.addBatch(query);
        }
        return statement;
    }
}
