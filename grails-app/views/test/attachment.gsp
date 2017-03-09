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
	<g:if test="${flash.message}">
		<div class="message" role="status">${flash.message}</div>
	</g:if>
	<g:uploadForm name="attachmentForm" controller="testing" action="sendAttachment" >
		<g:textField name="to" value="${params?.to}" placeholder="to : user@xxx.com"/>
		<g:textField name="subject"  value="${params?.subject}" placeholder="Subject"/><br/>
		<g:textArea name="message" value="${params?.message}" placeholder="Message to send ? i.e. hello.."/><br/>
		<input type="file" name="attachment"/>
		<g:submitButton name="submit" value="Send email attachment"/>
	</g:uploadForm>
</body>
</html>
