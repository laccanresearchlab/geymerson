## A python script to download data collected by
## the micaz wireless sensors network configured
## at the Laboratorio de Computacao Cientifica e
## Analise Numerica (LaCCAN). The requested data 
## will be sent to the user informed e-mail in
## <userEmail> field
## 
## @author: Geymerson Ramos <geymerson.r@gmail.com>
## March 05, 2017, 23:27 pm

import httplib
import datetime

## Uncomment date codes bellow if you want to pass
## the inputs in the terminal and comment the fromDate and
## to date at line 27 and 28

##Read initial date and time
#input = raw_input("Initial date (format YYYY-MM-DD-hh-mm-ss): ")
#input_list = input.split('-')
#userInput = [int(x.strip()) for x in input_list]
#fromDate = datetime.datetime(userInput[0], userInput[1], userInput[2], userInput[3], userInput[4], userInput[5])

##Read final date and time
#input = raw_input("Final date (format YYYY-MM-DD-hh-mm-ss): ")
#input_list = input.split('-')
#userInput = [int(x.strip()) for x in input_list]
#toDate = datetime.datetime(userInput[0], userInput[1], userInput[2], userInput[3], userInput[4], userInput[5])

## pass parameters to datetime: (int year, int month, int day, int hour, int min, int seconds)
## change it as you may
fromDate = datetime.datetime(2017, 03, 05, 0, 0, 0)
toDate = datetime.datetime(2017, 03, 05, 23, 59, 59)
userEmail = "example@email.com"
env_id = ["lab_15", "lab_16", "lab_17", "reun"]

#Query example
#"/api/v1/sensors/2017-03-05 00:00:00/2017-03-05 23:59:59/lab_15/fulano@email.com/allFields"
query = "/api/v1/sensors0/"
query += fromDate.strftime('%Y-%m-%d %H-%M-%S') +'/' 
query += toDate.strftime('%Y-%m-%d %H-%M-%S') + '/' + env_id[0] + '/' + userEmail + "/allFields"
#print query

#Setup and connect
conn = httplib.HTTPConnection("192.168.200.242")
print "Requesting data from:", fromDate, "to:", toDate
conn.request("GET", query)
r1 = conn.getresponse()
if r1.status == 200:
	print "Response: ", r1.status, " Request received, verify your email"
else:
	print "Response: ", r1.status, "Request failed: ", r1.reason
print r1.status, r1.reason
conn.close()
