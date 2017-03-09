<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
	</head>
	<body>
	<div  class="nav">
	<ul>
	<li><g:link  action="index">New email test</g:link></li>
	<li><g:link  action="verifyBounces">Verify bounce list / sent message list</g:link></li>
	<li><g:link  action="attachment">New email attachment test</g:link></li>
	<li><g:link  action="inlineImage">New email inline Image test</g:link></li>
	
	</ul>
	</div>
	<g:if test="${flash.message}">
		<div class="message" role="status">${flash.message}</div>
	</g:if>
	<g:form action="${params?.forwardAction?:'sendEmail'}" name="actionForm" >
		<g:hiddenField name="account" value="${params?.account}"/>
		<g:hiddenField name="folder" value="${params?.folder}"/>
		<g:textField name="to" value="${params?.to}" placeholder="to : user@xxx.com"/>
		<g:textField name="subject"  value="${params?.subject}" placeholder="Subject"/><br/>
		<g:textArea name="message" value="${params?.message}" placeholder="Message to send ? i.e. hello.."/>
		<g:textArea name="html" value="${params?.html}" placeholder='html content'/><br/><br/>
		<g:actionSubmit action="sendHTML" value="Send as HTML Email"/>
		<g:actionSubmit action="sendEmails" value="multiple emails"/>
		<g:submitButton name="submit" value="Send standard email"/>
	</g:form>
</body>
</html>
