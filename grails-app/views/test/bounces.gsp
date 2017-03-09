<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
	</head>
	
	<body>
	<div  class="nav">
	<ul>
	<li><g:link action="index">New email test</g:link></li>
	<li><g:link action="verifyBounces">Verify bounce list / sent message list</g:link></li>
	<li><g:link action="attachment">New email attachment test</g:link></li>
	<li><g:link action="inlineImage">New email inline Image test</g:link></li>
	</ul>
	</div>
		<g:if test="${instance.size()>0}"> 
		<h2>Bounces found : ${instance.size()}</h2><br/>
		<div class="errors">
		<g:each in="${instance}" var="failed">
		    ${failed.bouncedRecord} --> ${failed.mapRecord?.messageTo?.toString()} ${failed.mapRecord?.date} <br/>
		</g:each>
		</div>
		</g:if>
	<br/>
	<g:if test="${gmail.GmailService.messageMap.size()>0}"> 
		<h2>Total messages sent so far: ${gmail.GmailService.messageMap.size()}</h2><br/>
		<g:each in="${gmail.GmailService.messageMap}" var="m">
		  	${m.key } - ${m.value?.messageTo.toString() } -- ${m.value?.date } <br/>
		</g:each>
	</g:if>
	</body>
</html>
		