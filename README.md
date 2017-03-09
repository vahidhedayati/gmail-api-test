Gmail API Test running on a grails(2.4.4) application
===

The very first email you send for any given gmail account should be done on your application by yourself since it does require verification to then give that key access. Once set a file is generated within the app that contains credentials for gmail to be able to from then on send emails through the API.


# This demo site provides:

##  Message / HTML
### above form can be sent as standard / HTML 
### or standard to multiple users (this will be the to and emailAccount) as the to listing

##  Attachment 
### this will send a standard text email with the attachment included

##  Inline image
### This loads up c:\gmail-test\images.jpg (refer to service file for file)
### when inline image loaded by default it provided:

```html 
<img src="cid:myimage" /> <h1>inline image</h1>
```

The `myimage` tag is then converted to `c:\gmail-test\images.jpg`. Purely used to demonstrate or recreate standards you would have with java mail API. 


## Provides verifyBounce action
### When clicked, invalid emails  hopefully captured here
### It will also list the entire sent messages to who when as well 
#### Basically look into service concurrentMap and look at configuration to manage map 
#### You possibly want to keep it light and clean out old sent messages beyond a certain point 


## Getting started: You must get authorisation key from:

[client_key](https://console.developers.google.com/flows/enableapi?apiid=gmail&credential=client_key) [explained here](https://developers.google.com/gmail/api/auth/web-server), Once you have the file store it in a location as per configuration

#### https://console.developers.google.com/apis/credentials/consent?createClient.. in here put in project name as the APPLICATION NAME you are setting below, also set privacy policy 

#### In https://console.developers.google.com/apis/credentials/oauthclient?project= .. set the name to be TEST I think and redirect URIs to be as hardcord `http://localhost:9091/Callback`

In the controller pay particular interest to configured variables at the top of the controller. B default creates a google API app called `TEST`:
 
```groovy
//Your Google API Application Name
	private final String APPLICATION_NAME = 'TEST'
```

At the very top it is looking for some vital files/locations:

```groovy
private final String GOOGLEFILE = Holders.config.grailsApplication.config.gmailSecurityFile ?:  'client_secret.json'
	private final  File SECURITY_FILE = new File(Holders.config.grailsApplication.config.gmailSecurityPath ?: 'c:\\\\gmail-test\\\\'+GOOGLEFILE);
	private final java.io.File DATA_STORE_DIR = new java.io.File(Holders.config.grailsApplication.config.gmailStoragePath ?: 'c:\\\\gmail-test\\\\vh')
```

This is saying
1: `GOOGLEFILE='client_secret.json'` By default. So this is the file you would download through the linkes provided under getting started. It must match this file name or set fileName  in your config.groovy for `gmailSecurityFile='something.json'`

2: `SECURITY_FILE='c:\gmail-test'` By default. Location for where above file will sit in. File can be set in your config.groovy for `gmailSecurityPath='\somepath'` 

3: `DATA_STORE_DIR='c:\gmail-test\vh'` when it authenticates against google it will generate file and store in this location.File can be set   in your config.groovy for `gmailStoragePath='otherpath'` 



## Overriding / using multiple gmail accounts.
Looking at how getGmail works, it has been rather complicated to try and override a given account, none the less. Take a look at Dynamic Controller where it does such a thing.

#####  http://localhost:8080/gmail-api-test/dynamic?account=client_secret.json?folder=account1
#####  where c:\gmail-test\accounts\account1\client_secret.json already exists and  c:\gmail-test\accounts\account1\vh will be created to store credentials

#####  http://localhost:8080/gmail-api-test/dynamic?account=vh.json&folder=account2
#####  where c:\gmail-test\accounts\account2\vh.json already exists and  c:\gmail-test\accounts\account2\vh will be created to store credentials


##TODO - this does not appear to list messages correctly
Then you can verify each account like:
##### http://localhost:8080/gmail-api-test/dynamic/verifyBounces?account=client_secret.json&folder=account1
##### http://localhost:8080/gmail-api-test/dynamic/verifyBounces?account=vh.json&folder=account2


## Possible bugs in gmail API.
Unsure about this bit a little confusing as to how it works. lets say I generated  client_secret.json under gmail account username `Bob`
When for the very first email that needs to be confirmed via this site on a given account a one off process. During that 1 off process if i log in to gmail with another account of `Bill` The key is verified and emails then appear to come from `bill@gmail.com` Even though I had not visited / configured Bill's account to use the client_secret.json file. 

