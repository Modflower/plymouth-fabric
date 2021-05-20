<img width="128" src="src/main/resources/pack.png" alt="Plymouth Database" align="right"/>
<div align="left">

# Plymouth: Database

A database API for use with Tracker. Currently only supports PostgreSQL.

## Downloads

You may download Database from [Modrinth](https://modrinth.com/mods/plymouth-database) or
from [GitHub Releases](https://github.com/the-glitch-network/plymouth-fabric/releases).

## Usage ('Tis rather involved currently due to the database of choice.)

### Prerequisites

- You'll want to be familiar with managing PostgreSQL or similar databases.
- You'll have to be willing to configure the mod initially so it'll work.
- You should be familiar with securing databases to avoid misuse.

### Setup (PostgreSQL + Linux)

1. Install PostgreSQL using your favourite method.
    - [Debian](https://wiki.debian.org/PostgreSql): `apt install postgresql`
    - [Fedora/RHEL](https://fedoraproject.org/wiki/PostgreSQL): `dnf install postgresql-server postgresql-contrib`
        - Older versions of Redhat Enterprise Linux and derivatives may require the use of `yum` in place of `dnf`.
        - This require you to run `sudo postgresql-setup --initdb --unit postgresql` to setup PostgreSQL.
    - [Arch Linux](https://wiki.archlinux.org/title/PostgreSQL): `pacman -S postgresql`
        - This requires you to run `sudo -u postgres initdb -D /var/lib/postgres/data`
          or `su -l postgres -c "initdb -D /var/lib/postgres/data"` to setup PostgreSQL.
    - *Some distributions, such as Fedora and Arch Linux may require you to run `systemctl enable --now postgresql` to
      start the database and to make it restart on system reboot.*
2. Create a user and database for Plymouth to use.
    - If you prefer to use `psql` directly, you can use the following SQL to get this going. You may need to use
      the `postgres` account, which you can access by using `sudo -u postgres psql`.
      ```sql
      CREATE USER plymouth WITH PASSWORD 'Insert a password for the database here. Be sure to escape your \' as necessary.';
      CREATE DATABASE plymouth WITH OWNER = plymouth;
      ```
    - Alternatively, you can use the following two commands. With the `-P` option, you'll be prompted to input a
      password. You may need to use the `postgres` account, which you can access by prepending `sudo -u postgres`, or by
      going into the account with `su postgres`.
      ```sh
      createuser plymouth -P
      createdb plymouth -O plymouth
      ```
3. Drop the mod into the mods folder of your server along with Common then boot it up. A configuration file will be
   created at `config/plymouth.db.properties` for you to edit.
4. Edit the config so that the database, username and password matches what you've used for the database.
    - Reference
      ```properties
      url=jdbc\:postgresql\://127.0.0.1\:5432/database
      user=username
      password=password
      ```
    - From the example so far.
      ```properties
      url=jdbc\:postgresql\://127.0.0.1\:5432/plymouth
      user=plymouth
      password=Insert a password for the database here. Be sure to escape your ' as necessary.
      ```
5. Start the server for reals this time. The database handler will bootstrap the database itself with the tables
   necessary to function.

</div>