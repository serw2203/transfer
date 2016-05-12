/*
drop table aaa_turn;
drop table aaa_oper;
drop table aaa_h_client;
drop table aaa_rest;
drop table aaa_account;
drop table aaa_client;
drop table aaa_oper_type;
drop table aaa_cross_rate;
drop table aaa_currency;
*/

create table aaa_client (
   client_id int not null
);

alter table aaa_client add primary key (client_id);

insert into aaa_client values (0); --owner

create table aaa_h_client (
   h_client_id int not null,
   client_id int not null,
   last_name varchar(48),
   first_name varchar(48),
   middle_name varchar(48),
   modify_date date not null,
   cli_version integer not null
);

insert into aaa_h_client (h_client_id, client_id, last_name, modify_date, cli_version)
values (0, 0, 'OWNER', '01.01.1900', 0);

alter table aaa_h_client add primary key (h_client_id);

alter table aaa_h_client add constraint f_h_client_on_client_client_id
    foreign key (client_id) references aaa_client (client_id) on delete cascade;

create unique index ix_h_client_client_current on aaa_h_client (client_id, cli_version);

create table aaa_account (
   acc_id int not null,
   client_id int not null,
   acc_num varchar(20) not null
);

alter table aaa_account add primary key (acc_id);

alter table aaa_account add constraint un_acc_num unique (acc_num);

alter table aaa_account add constraint f_account_on_client_client_id
    foreign key (client_id) references aaa_client (client_id) on delete cascade;

insert into aaa_account values (0, 0, 'OWNER'); --owner

execute block returns (last_name varchar(48), acc_num varchar (20)) as
   declare variable client_id int;
   declare variable h_client_id int;
   declare variable acc_id int;
begin
    client_id = 1;
    h_client_id = 1;
    acc_id = 5001;
    while (client_id <= 10000) do
    begin
        insert into aaa_client values (:client_id);

        last_name = 'CLI'|| reverse( left( reverse('00000' || :client_id ), 5 ));
        insert into aaa_h_client (h_client_id, client_id, last_name, first_name, middle_name, modify_date, cli_version)
        values (:h_client_id, :client_id, :last_name, 'FN1', 'MN1', getdate() -365 * 10, -2);

        h_client_id = h_client_id + 1;

        insert into aaa_h_client (h_client_id, client_id, last_name, first_name, middle_name, modify_date, cli_version)
        values (:h_client_id, :client_id, :last_name, 'FN2', 'MN2', getdate() -365 * 5, -1);
        h_client_id = h_client_id + 1;

        insert into aaa_h_client (h_client_id, client_id, last_name, first_name, middle_name, modify_date, cli_version)
        values (:h_client_id, :client_id, :last_name, 'FN3', 'MN3', getdate() -100, 0);
        h_client_id = h_client_id + 1;

        acc_num = 'ACC'|| reverse( left( reverse('00000' || cast(:acc_id - 5000 as int)), 5 ));
        insert into aaa_account (acc_id, client_id, acc_num)
        values (:acc_id - 5000, :client_id, :acc_num );
        acc_id = acc_id + 1;

        --suspend;

        if ( cast(:client_id as double precision)/ 2 - cast(:client_id/2 as int) = 0) then
        begin
            acc_num = 'ACC'|| reverse( left( reverse('00000' || cast(:acc_id - 5000 as int)), 5 ));
            insert into aaa_account (acc_id, client_id, acc_num)
            values (:acc_id - 5000, :client_id, :acc_num );
            acc_id = acc_id + 1;
            --suspend;
        end

        client_id = client_id + 1;
    end
end;

create table aaa_currency (
   cur_code varchar(5) not null
);

alter table aaa_currency add constraint un_cur_code unique (cur_code);

insert into aaa_currency
select cur_code from (
  select 'RUB' as cur_code from dual union
  select 'USD' as cur_code from dual union
  select 'EUR' as cur_code from dual union
  select 'GBP' as cur_code from dual union
  select 'SHF' as cur_code from dual
);

create table aaa_oper_type (
   oper_type varchar(8) not null,
   oper_name varchar (128)
);

alter table aaa_oper_type add constraint un_aaa_oper_type unique (oper_type);

insert into aaa_oper_type
select oper_type, oper_name from (
  select 'INPUT' as oper_type, 'Ввод денежных средств' as oper_name from dual union
  select 'OUTPUT' as oper_type, 'Вывод денежных средств' from dual union
  select 'CONVERT' as oper_type, 'Конвертация денежных средств' from dual union
  select 'TRANSFER' as oper_type, 'Перевод денежных средств на другой счет' from dual
);

create table aaa_oper (
    oper_id int not null,
    h_client_id  int not null,
    oper_date date not null,
    oper_type varchar(12) not null,
    comment varchar(255)
);

alter table aaa_oper add primary key (oper_id);

alter table aaa_oper add constraint f_oper_on_oper_type_oper_type
    foreign key (oper_type) references aaa_oper_type (oper_type);

alter table aaa_oper add constraint f_oper_on_h_client_h_client_id
    foreign key (h_client_id) references aaa_h_client (h_client_id);

create descending index ix_oper_date on aaa_oper (oper_date);


create table aaa_turn (
    turn_id int not null,
    oper_id int not null,
    acc_id int not null,
    cur_code varchar(5) not null,
    d_amount double precision default 0 not null,
    k_amount double precision default 0 not null,
    turn_date date not null
);

alter table aaa_turn add primary key (turn_id);

alter table aaa_turn add constraint f_turn_on_acc_acc_id
    foreign key (acc_id) references aaa_account (acc_id);

alter table aaa_turn add constraint f_turn_on_curr_cur_code
    foreign key (cur_code) references aaa_currency (cur_code);

alter table aaa_turn add constraint f_turn_on_oper_oper_id
    foreign key (oper_id) references aaa_oper (oper_id);

create index ix_oper_acc_cur on aaa_turn (acc_id, cur_code);

create descending index ix_turn_date on aaa_turn (turn_date);

create table aaa_rest (
    acc_id int not null,
    cur_code varchar(5) not null,
    amount double precision default 0 not null,
    modify_date date not null
);

create unique index ix_rest_acc_cur on aaa_rest (acc_id, cur_code);

create descending index ix_rest_modify_date on aaa_rest (modify_date);

alter table aaa_rest add constraint f_rest_on_acc_acc_id
    foreign key (acc_id) references aaa_account (acc_id);

alter table aaa_rest add constraint f_rest_on_curr_cur_code
    foreign key (cur_code) references aaa_currency (cur_code);

/*
execute block as
    declare variable i int;
begin
    i = 0;
    while (i < 10000) do
    begin
        insert into aaa_turn (oper_id, acc_id, cur_code, d_amount, k_amount,
                                   oper_date, oper_date_tz)
        select gen_id (bases_keys_id_gen,  1), a.acc_id, c.cur_code, 1, 0,
                                   getdate(), getdate() from aaa_account a, aaa_currency c;

        insert into aaa_turn (oper_id, acc_id, cur_code, d_amount, k_amount,
                                   oper_date, oper_date_tz)
        select gen_id (bases_keys_id_gen,  1), a.acc_id, c.cur_code, 0, 1,
                                   getdate(), getdate() from aaa_account a, aaa_currency c;

        i = i + 1;
    end
end

merge into aaa_rest r using (
     select acc_id, cur_code, sum(d_amount) - sum(k_amount) amount,
            max(oper_date) as oper_date_lst  from aaa_turn
     group by acc_id, cur_code
) x on (r.acc_id = x.acc_id and r.cur_code = x.cur_code)
when matched then update set amount = x.amount
when not matched then insert values (x.acc_id, x.cur_code, x.amount, x.oper_date_lst);
*/

create table aaa_cross_rate (
   scur_code varchar(5) not null,
   tcur_code varchar(5) not null,
   date_rate date not null,
   rate double precision default 0 not null
);

alter table aaa_cross_rate add constraint f_rate_on_scurr_cur_code
    foreign key (scur_code) references aaa_currency (cur_code);

alter table aaa_cross_rate add constraint f_rate_on_tcurr_cur_code
    foreign key (tcur_code) references aaa_currency (cur_code);

create descending index ix_rate_date on aaa_cross_rate (date_rate);

insert into aaa_cross_rate (scur_code, tcur_code, date_rate, rate)
select s.cur_code , t.cur_code, f_date(x.date_),
       f_round( s.ranq / t.ranq + 1/extract ( day from date_ ), 6)  from
(select cur_code,
          case cur_code when 'RUB' then 65.75
                        when 'EUR' then 1.05
                        when 'GBP' then 0.68
                        when 'SHF' then 0.73
          else 1 end ranq
          from aaa_currency)  s
join (select cur_code,
          case cur_code when 'RUB' then 65.75
                        when 'EUR' then 1.05
                        when 'GBP' then 0.68
                        when 'SHF' then 0.73
          else 1 end ranq
          from aaa_currency) t on s.cur_code != t.cur_code
join (  select d.date_ - 365 * x.n as date_ from (
            with recursive d (date_) as (
                select getdate () date_ from dual union all
                select date_ - 2 from d where datediff (day, date_, getdate () ) < 365
            ) select date_ from d ) d
        join (select 0 n from dual union select 1 from dual union
              select 2   from dual union select 3 from dual union
              select 4   from dual union select 5 from dual union
              select 6   from dual union select 7 from dual union
              select 8   from dual union select 9 from dual  ) x on 1 = 1
) x on 1 = 1;

/*
select hc.h_client_id as h_client_id, hc.client_id, x.date_ from aaa_h_client hc
join (select d.date_ - 365 * x.n as date_ from (
            with recursive d (date_) as (
                select getdate () date_ from dual union all
                select date_ - 3 from d where datediff (day, date_, getdate () ) < 365
            ) select date_ from d ) d
        join (select 0 n from dual union select 1 from dual union
              select 2   from dual union select 3 from dual union
              select 4   from dual union select 5 from dual union
              select 6   from dual union select 7 from dual union
              select 8   from dual union select 9 from dual  ) x on 1 = 1 ) x on x.date_ >= hc.modify_date
*/


--операция
/*
execute block as
    declare variable in_acc_id int;
    declare variable in_curr varchar(5);
    declare variable out_acc_id int;
    declare variable out_curr varchar(5);

    declare variable in_amount double precision;
    declare variable out_amount double precision;

    declare variable sturn_id int;
    declare variable tturn_id int;
    declare variable date_ timestamp;

    declare variable h_client_id int;
begin
    sturn_id = coalesce((select max(turn_id) from aaa_turn), 0) + 1;
    tturn_id = sturn_id + 1;

    date_ = '06.04.2007 20:00:00';
    --клиент принес 100 usd
    out_amount = 10;
    out_curr = 'HKD';
    out_acc_id = 0;

    --хочет положить в рублях
    in_curr = 'EUR';
    in_acc_id = 2;

    --количество рублей за доллар
    select first 1 f_round( cr.rate * :out_amount , 2) from aaa_cross_rate cr
    where cr.tcur_code = :out_curr and cr.scur_code = :in_curr and date_rate <= :date_
    order by cr.date_rate desc into :in_amount;

    if (in_amount is not null) then
    begin
        insert into aaa_turn (turn_id, acc_id, cur_code, d_amount, k_amount, turn_date)
        values (:sturn_id, :out_acc_id, :out_curr, 0, :out_amount, :date_);

        insert into aaa_turn (turn_id, acc_id, cur_code, d_amount, k_amount, turn_date)
        values (:tturn_id, :in_acc_id, :in_curr, :in_amount, 0, :date_);

        select hc.h_client_id from aaa_account a
        join aaa_h_client hc on hc.client_id = a.client_id and hc.cli_version = 0
        where a.acc_id = :in_acc_id into :h_client_id;

        insert into aaa_oper (h_client_id, sturn_id, tturn_id, oper_date, oper_type)
        values (:h_client_id, :sturn_id, :tturn_id, f_date (:date_), 'INPUT');

        merge into aaa_rest r using (
            select acc_id, cur_code, d_amount - k_amount as amount from aaa_turn
            where turn_id in (:sturn_id, :tturn_id) and acc_id != 0) t
        on (r.acc_id = t.acc_id and r.cur_code = t.cur_code)
        when matched then update set amount = r.amount + t.amount, modify_date = :date_
        when not matched then insert values (t.acc_id, t.cur_code, t.amount, :date_);
    end
end

select c.client_id, hc.last_name, hc.first_name, hc.middle_name, hc.modify_date from aaa_client c
join aaa_h_client hc on c.client_id = hc.client_id and hc.cli_version = 0
where c.client_id != 0 = c.client_id = ?;

*/
