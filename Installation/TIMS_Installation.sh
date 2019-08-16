#!/bin/bash
# Copyright (C) 2017 A*STAR

# TIMS (Translation Informatics Management System) is an software effort
# by the ABSD (Analytics of Biological Sequence Data) team in the
# Bioinformatics Institute (BII), Agency of Science, Technology and Research
# (A*STAR), Singapore.

# This file is part of TIMS.
 
# TIMS is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.

# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Run this script at the home directory using ./TIMS_Installation.sh |& tee output.txt
# The output from the script will be displayed both on the screen and in the output.txt file.

# All the messages from this script will be in green.
MS='\033[0;32m' # Green
NC='\033[0m' # No Color
# Get the Ubuntu version
version=$(lsb_release -rs)
# Begin installation and give a rough estimate on when it will complete.
echo -e ${MS}Begin TIMS installation. The whole installation estimated will takes around 5 hours to complete...

# Install Git using apt-get.
echo -e Installing Git...${NC}
sudo apt-get install git

# Install JDK 8 using apt-get.
echo -e ${MS}Installing Java JDK...${NC}
# software-properties-common is needed before you could use add-apt-repository
sudo apt-get update
sudo apt-get install software-properties-common
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk
# Install unzip
sudo apt-get install unzip

# Install SWI-Prolog:
sudo apt-add-repository ppa:swi-prolog/stable
sudo apt-get update
sudo apt-get install swi-prolog
sudo apt-get install swi-prolog-java

# Update the Java library path to include swi-prolog libraries:
if [ $(uname -m) == 'x86_64' ];
then
echo 'export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/lib/swi-prolog/lib/x86_64-linux"' >> ~/.profile
else
echo 'export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/lib/swi-prolog/lib/amd64"' >> ~/.profile
fi

# Install Ant using apt-get.
echo -e ${MS}Installing Ant...${NC}
sudo apt-get install ant

# Add JAVA_HOME and ANT_HOME as they are needed during the compilation of TIMS.
echo -e ${MS}Adding environment variables JAVA_HOME and ANT_HOME...${NC}
touch ~/.profile
echo 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64' >> ~/.profile
echo 'export ANT_HOME=/opt/apache-ant-1.10.1' >> ~/.profile
echo 'export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:${PATH}' >> ~/.profile
source ~/.profile

# Clone and create a local tims repository at user home directory.
echo -e ${MS}Cloning TIMS repository from GitHub...
git clone https://github.com/bii-absd/tims --branch tims_agpl3 --single-branch ~/tims
sudo chown -R $USER:$USER ~/tims
echo -e ${MS}Building the project now...${NC}
cd ~/tims
ant -Dlibs.CopyLibs.classpath=./libraries/org-netbeans-modules-java-j2seproject-copylibstask.jar

# Install and setup PostgreSQL 9.4 for TIMS database.
echo -e ${MS}Installing PostgreSQL 9.4...${NC}
wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O - | sudo apt-key add -
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
sudo apt-get update
sudo apt-get install postgresql-client-9.4 postgresql-9.4 postgresql-contrib-9.4
echo -e ${MS}Setting up PostgreSQL...
sudo -u postgres psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'tims2017';"
echo -e ${MS}Updating PostgreSQL pg_hba.conf file...${NC}
sudo cp ~/tims/scripts/pg_hba.conf /etc/postgresql/9.4/main/
sudo chown postgres:postgres /etc/postgresql/9.4/main/pg_hba.conf
echo -e ${MS}Setup PostgreSQL password file...${NC}
touch ~/.pgpass
echo '*:*:*:postgres:tims2017' > ~/.pgpass
sudo chmod 0600 ~/.pgpass
sudo chown $USER:$USER ~/.pgpass
echo -e ${MS}Restarting PostgreSQL service...${NC}
sudo service postgresql restart
echo -e ${MS}Create and setup TIMS database...${NC}
psql -U postgres -c 'CREATE DATABASE tims WITH TEMPLATE = template0'
psql -d tims -U postgres < ~/tims/scripts/tims_database_creation.sql
psql -d tims -U postgres -f ~/tims/scripts/tims_database_setup.sql

# Install and setup GlassFish for TIMS.
echo -e ${MS}Installing GlassFish...${NC}
cd /opt
sudo wget http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip
sudo unzip glassfish-4.1.zip
sudo rm glassfish-4.1.zip
echo -e ${MS}Setting up GlassFish...${NC}
sudo chgrp -R $USER /opt/glassfish4/glassfish
sudo chown -R $USER /opt/glassfish4/glassfish
echo -e ${MS}Copy PostgreSQL JDBC library into GlassFish...${NC}
cp ~/tims/libraries/postgresql-9.4-1202.jdbc41.jar /opt/glassfish4/glassfish/lib/
echo -e ${MS}Adding environment variables PORTAL_HOME and CATALINA_HOME...${NC}
echo 'export PORTAL_HOME=/var/cbioportal' >> ~/.profile
echo 'export CATALINA_HOME=/opt/tomcat-8.0.45' >> ~/.profile
echo 'export PATH=/opt/glassfish4/bin:${PATH}' >> ~/.profile
source ~/.profile
# Restart GlassFish and setup JDBC connection pool and resource. Update the port to 8081 so that it will not clash with Tomcat.
echo -e ${MS}Start GlassFish server...${NC}
asadmin start-domain
echo -e ${MS}Creating JDBC connection pool...${NC}
asadmin create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGConnectionPoolDataSource --restype javax.sql.ConnectionPoolDataSource --property portNumber=5432:password=tims2017:user=postgres:serverName=localhost:databaseName=tims tims_conn_pool
echo -e ${MS}Creating JDBC resource...${NC}
asadmin create-jdbc-resource --connectionpoolid tims_conn_pool jdbc/tims_conn_pool
echo -e ${MS}Updating the port of the network listener to 8081...${NC}
asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.port=8081
echo -e ${MS}Setup Java Virtual Machine maximum heap size to 1G...${NC}
asadmin delete-jvm-options -Xmx512m
asadmin create-jvm-options -Xmx1g

# Deploy TIMS to GlassFish server.
echo -e ${MS}Deploying TIMS application to GlassFish...${NC}
sudo mkdir /var/TIMS
sudo mkdir /var/TIMS/users
sudo mkdir /var/TIMS/users/tims-admin
sudo mkdir /var/TIMS/users/tims-pi
sudo mkdir /var/TIMS/users/tims-admin/output
sudo mkdir /var/TIMS/users/tims-admin/config
sudo mkdir /var/TIMS/users/tims-admin/log
sudo mkdir /var/TIMS/users/tims-pi/output
sudo mkdir /var/TIMS/users/tims-pi/config
sudo mkdir /var/TIMS/users/tims-pi/log
sudo chown -R $USER:$USER /var/TIMS
asadmin deploy ~/tims/dist/TIMS.war

# Install and setup Tomcat for cBioPortal.
echo -e ${MS}Installing Tomcat server...${NC}
cd /opt
sudo wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/apache-tomcat-8.0.45.tar.gz
sudo tar xzf apache-tomcat-8.0.45.tar.gz
sudo rm apache-tomcat-8.0.45.tar.gz
sudo mv /opt/apache-tomcat-8.0.45/ /opt/tomcat-8.0.45/
sudo chown -R $USER:$USER /opt/tomcat-8.0.45/
echo -e ${MS}Start Tomcat server...${NC}
/opt/tomcat-8.0.45/bin/catalina.sh start

# Install and setup MySQL 5.6 for cBioPortal database.
echo -e ${MS}Installing MySQL server 5.6. Do not update the pasword for root...${NC}
if [ $version == "16.04" ]
then
sudo add-apt-repository 'deb http://archive.ubuntu.com/ubuntu trusty universe'
sudo apt-get update
sudo apt install mysql-server-5.6 mysql-client-5.6
else
# Do these steps for Ubuntu version that is lower than 16.04
sudo add-apt-repository 'deb http://archive.ubuntu.com/ubuntu trusty main'
sudo apt-get update
sudo apt-get install mysql-server-5.6 mysql-client-5.6
fi
# Restart MySQL service after the installation
sudo service mysql start
echo -e ${MS}Installing MySQL JDBC driver...${NC}
sudo apt-get install libmysql-java
echo -e ${MS}Creating cBioPortal database...${NC}
sudo mysql -u root -e "CREATE DATABASE cbioportal16;"
sudo mysql -u root -e "CREATE USER 'cbio_user'@'localhost' IDENTIFIED BY 'password';"
sudo mysql -u root -e "GRANT ALL ON cbioportal16.* TO 'cbio_user'@'localhost';"
sudo mysql -u root -e "FLUSH PRIVILEGES;"
echo -e ${MS}Setting up cBioPortal database. This will take about 45 minutes to complete...${NC}
cd ~
# 1. cgds
wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/cgds.sql
mysql -u cbio_user -ppassword cbioportal16 < cgds.sql
rm cgds.sql
# 2. hg19
wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/seed-cbioportal_hg19_v2.6.0.sql.gz
gunzip seed-cbioportal_hg19_v2.6.0.sql.gz
mysql -u cbio_user -ppassword cbioportal16 < seed-cbioportal_hg19_v2.6.0.sql
rm seed-cbioportal_hg19_v2.6.0.sql

# Deploy cBioPortal to Tomcat server.
echo -e ${MS}Deploying cBioPortal application...${NC}
sudo mkdir /var/cbioportal
sudo chown $USER:$USER /var/cbioportal
# Setup cBioPortal export study resources.
cd /var/cbioportal
wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/scripts-1.15.0.jar
wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/cbioportal.war
wget http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/cbioportal_scripts.zip
unzip cbioportal_scripts.zip
# Make sure all the Python scripts are executable.
chmod +x /var/cbioportal/*.py*
sudo cp ~/tims/scripts/tomcat-users.xml /opt/tomcat-8.0.45/conf
sudo cp ~/tims/scripts/context.xml /opt/tomcat-8.0.45/conf
sudo cp /var/cbioportal/cbioportal.war /opt/tomcat-8.0.45/webapps
rm /var/cbioportal/cbioportal.war
rm /var/cbioportal/cbioportal_scripts.zip
sudo apt-get install python-pip
sudo apt-get install python-requests
echo -e ${MS}TIMS Application Installation Completed...${NC}
echo -e ${MS}Proceed to install the pipelines package${NC}
source ~/tims/scripts/install-tims-pipeline.script
echo -e ${MS}Installation completed, rebooting the system now.${NC}
sudo reboot
