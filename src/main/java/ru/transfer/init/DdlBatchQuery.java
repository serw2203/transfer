package ru.transfer.init;

import ru.transfer.query.BatchQuery;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class DdlBatchQuery implements BatchQuery {

    private final static String[] DDL_QUERIES = new String[]{
            "create sequence seq_id start with 1;",

            "create table aaa_client ( client_id bigint default seq_id.nextval primary key);",

            "insert into aaa_client values (0);",

            "create table aaa_h_client ( h_client_id bigint default seq_id.nextval primary key, client_id bigint not null, last_name varchar(48), " +
                    "first_name varchar(48), middle_name varchar(48), modify_date timestamp not null, cli_version int not null);",

            "insert into aaa_h_client (h_client_id, client_id, last_name, modify_date, cli_version) values (0, 0, 'OWNER', '1900-01-01 00:00:00.0', 0);",

            "alter table aaa_h_client add constraint f_h_client_on_client_client_id foreign key (client_id) references aaa_client (client_id) on delete cascade;",

            "create unique index ix_h_client_client_current on aaa_h_client (client_id, cli_version);",

            "create table aaa_account (acc_id bigint default seq_id.nextval primary key,client_id bigint not null,acc_num varchar(20) not null);",

            "alter table aaa_account add constraint un_acc_num unique (acc_num);",

            "alter table aaa_account add constraint f_account_on_client_client_id foreign key (client_id) references aaa_client (client_id) on delete cascade;",

            "insert into aaa_account values (0, 0, 'OWNER');",

            "create table aaa_currency ( cur_code varchar(5) primary key );",

            "insert into aaa_currency select cur_code from ( select 'RUB' as cur_code union " +
                    "select 'USD' as cur_code union select 'EUR' as cur_code union select 'GBP' as cur_code union select 'SHF' as cur_code);",

            "create table aaa_oper_type ( oper_type varchar(8) not null primary key, oper_name varchar (128));",

            "insert into aaa_oper_type select oper_type, oper_name from ( select 'INPUT' as oper_type, 'Ввод денежных средств' as oper_name union select 'OUTPUT' as oper_type, " +
                    "'Вывод денежных средств' union select 'CONVERT' as oper_type, 'Конвертация денежных средств' union select 'TRANSFER' as oper_type, 'Перевод денежных средств на другой счет');",
            "create table aaa_oper ( oper_id bigint default seq_id.nextval primary key, h_client_id  bigint not null, oper_date date not null, oper_type varchar(12) not null, comment varchar(255) );",

            "alter table aaa_oper add constraint f_oper_on_oper_type_oper_type foreign key (oper_type) references aaa_oper_type (oper_type);",

            "alter table aaa_oper add constraint f_oper_on_h_client_h_client_id foreign key (h_client_id) references aaa_h_client (h_client_id);",

            "create index ix_oper_date on aaa_oper (oper_date desc);",

            "create table aaa_turn ( turn_id bigint default seq_id.nextval primary key, oper_id bigint not null, acc_id bigint not null, cur_code varchar(5) not null, d_amount decimal(20,2) default 0 not null, " +
                    "k_amount decimal(20,2) default 0 not null, turn_date date not null);",

            "alter table aaa_turn add constraint f_turn_on_acc_acc_id foreign key (acc_id) references aaa_account (acc_id);",

            "alter table aaa_turn add constraint f_turn_on_curr_cur_code foreign key (cur_code) references aaa_currency (cur_code);",

            "alter table aaa_turn add constraint f_turn_on_oper_oper_id foreign key (oper_id) references aaa_oper (oper_id);",

            "create index ix_oper_acc_cur on aaa_turn (acc_id, cur_code);",

            "create  index ix_turn_date on aaa_turn (turn_date desc);",

            "create table aaa_rest ( acc_id bigint not null, cur_code varchar(5) not null, amount decimal(20,2) default 0 not null, modify_date date not null);",

            "create unique index ix_rest_acc_cur on aaa_rest (acc_id, cur_code);",

            "create index ix_rest_modify_date on aaa_rest (modify_date desc);",

            "alter table aaa_rest add constraint f_rest_on_acc_acc_id foreign key (acc_id) references aaa_account (acc_id);",

            "alter table aaa_rest add constraint f_rest_on_curr_cur_code foreign key (cur_code) references aaa_currency (cur_code);",

            "create table aaa_cross_rate (scur_code varchar(5) not null,tcur_code varchar(5) not null, date_rate date not null, rate decimal(20,8) default 0 not null);",

            "alter table aaa_cross_rate add constraint f_rate_on_scurr_cur_code foreign key (scur_code) references aaa_currency (cur_code);",

            "alter table aaa_cross_rate add constraint f_rate_on_tcurr_cur_code foreign key (tcur_code) references aaa_currency (cur_code);",

            "create index ix_rate_date on aaa_cross_rate (date_rate desc);"
    };

    @Override
    public Statement createStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        for (String query : DDL_QUERIES) {
            statement.addBatch(query);
        }
        return statement;
    }
}
