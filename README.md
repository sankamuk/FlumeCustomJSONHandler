
# Secure Flume JSONHandler for HTTPSource

This repository is the code base for the blog

https://mukherjeesankar.wordpress.com/2019/05/06/how-to-flume-secure-and-ha/

Here we create a code for custom JSONHandler for HTTPSource which accepts an token in header with name ***"auth"*** and validates if it matches with configuration value with name ***"authtoken"***.

## Installation and Deployment

To build you just need to run maven package ***(i.e. mvn package)*** to create artifact. It will create ***flume-http-source-customhandler-1.0-SNAPSHOT.jar*** which need to be placed in Flume installation lib directory.


## Configuration

To use this you need to customise your Flume agent configuration with below configuration. 

```

<agent name>.sources.<source id>.handler = com.mukherjee.sankar.flume.customhandler.CustomJSONHandler
<agent name>.sources.<source id>.handler.authtoken = sankar
  
```

NOTE: Here "sankar" is your token based on which your event will be present. All source event should have an header field named "auth" and value "sankar".

## Testing

Now you can POST event like below.

> curl -X POST -H 'Content-Type: application/json; charset=UTF-8' -d '[{"headers": {"auth":"sankar"},"body": "Test"}]' http://localhost:4444

Successfully accepted by flume.

```

    19/05/06 16:42:15 INFO sink.LoggerSink: Event: { headers:{auth=sankar} body: 54 65 73 74                                        Test }
    
```

Unauthenticated request will throw exepetion back to client

> curl -X POST -H 'Content-Type: application/json; charset=UTF-8' -d '[{"headers": {"auth":"wrong"},"body": "new message"}]' http://localhost:4444

Client get Bad Request Exception

```

  <html>
  <head>
  <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
  <title>Error 400 Bad request from client. Request has invalid Authorisation Header.</title>
  </head>
  <body><h2>HTTP ERROR 400</h2>
  <p>Problem accessing /. Reason:
  <pre>    Bad request from client. Request has invalid Authorisation Header.</pre></p><hr><a            href="http://eclipse.org/jetty">Powered by Jetty:// 9.4.6.v20170531</a><hr/>

  </body>
  </html>
  
```

***Thanks You***

