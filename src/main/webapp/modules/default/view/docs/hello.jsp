<html>
	<head>
		<title>This is a JSP</title>
		<link href="/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css"/>
		<script src="/js/jquery-2.1.4.min.js"></script>
		<script src="/bootstrap/js/bootstrap.min.js"></script>
	</head>
	<body>
		<div class="col-sm-8 col-sm-push-1">
			<p>
				Hello, <%=request.getParameter("abc")%>!
			</p>

			<form method="post">
				<div class="form-group">
					<label for="name">You name:</label>
					<input type="text" class="form-control" id="name" placeholder="You name" name="name">
				</div>
				<button type="submit" class="btn btn-info">
					OK
				</button>
			</form>
			<pre>
			</pre>
		</div>
	</body>
</html>