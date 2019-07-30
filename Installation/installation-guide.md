# TIMS Automated Installation Guide

1.	The user who is going to install TIMS needs to have administrative right on the machine.

2.	Copy the installation script (i.e. TIMS_Installation.sh) into your home directory (e.g. /home/admin if your user name is admin)

3.	Make the script executable: chmod +x TIMS_Installation.sh

4.	Since the installation require administrative privilege, you may want to keep your "sudo" session last a bit longer than default which is 30 minutes, in order to prevent the installation from keep asking for password when the session timeout. To edit the sudoers timeout session, please follow the steps below:
	- On the command prompt, please type: `sudo visudo`
	- Please change the line:
		Defaults `env_reset` To Defaults `env_reset,timestamp_timeout=300`
	-- [Note: if you never want a password prompt, you can change the timestamp_timeout to -1, but this is not recommended]
	- Press Ctrl+X and then Y to finish editing and save changes. 
	- Please exit the Terminal and reopen it.
	-- [Note: after finish installation, you may want to change back the timestamp_timeout to default]

5.	Run the script from your home directory: ./TIMS_Installation.sh |& tee output.txt

6.	The output from the installation will be stored in output.txt

7.	The script took around 5 hours to run (depending on your internet speed.)

8.	Things to note during the installation:
	- You will be prompt for user password a couple of times.
	- When prompt to change MySQL password, just press enter.
	- When with the option of yes|no, just enter yes.

9.	Once the installation is completed, your computer will be reboot.

10.	Open a new Terminal and start the glassfish and mysql servers. On the command prompt, type:
	- asadmin start-domain
	- sudo service mysql start

11.	Please make sure your /etc/hosts file has been setup properly i.e. it contain the following line:
Your_IP_Address	Your_Hostname

12.	In order for TIMS to send out the status email, please ensure that the SMTP server is being installed.

13.	You can now access TIMS through the following link:
http://your-machine-ipaddress:8081/TIMS/login.xhtml

14.	Below is a screenshot of TIMS:
