create database giiwa;
create role giiwa with password 'g123123';
alter role giiwa with login;
grant all on database giiwa to giiwa;