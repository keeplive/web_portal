<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html>
<html lang="zh">
  <head>
    <title>智会-充值账户</title>
	<jsp:include page="common/_head.jsp"></jsp:include>
  </head>

  <body>
    <div class="navbar navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="#">智会</a>
          <ul class="nav">
          	<li><a href="#">退出登录</a></li>
          </ul>
          <div class="nav-collapse">
            <ul class="nav pull-right">
              <li class="active"><a href="account">充值账户</a></li>
              <li><a href="history">我的群聊</a></li>
              <li><a href="setting">系统设置</a></li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
    	<div class="row well">
			<div class="span4 offset3">
				<h1>账户余额：￥2,000.00</h1>
			</div>
			<a class="span1 btn btn-large btn-success" href="deposite">在线充值</a>
    	</div>
		<div class="row">
			<div class="page-header span6 offset3">
				<h2>我的充值记录</h2>
			</div>
		</div>
		<div class="row">
			<div class="span6 offset3">
				<table class="table table-striped">
					<thead>
						<tr>
						<th>充值日期</th>
						<th>充值金额</th>
						</tr>
					</thead>
					<tbody>
					<% for(int i=0; i<10; i++) {%>
						<tr>
						<td>2012-08-11 16:02</td>
						<td>￥100.00</td>
						</tr>
					<% } %>
					</tbody>
				</table>
				<ul class="pager">
					<li class="previous"><a href=#>上一页</a></li>
					<li class="next"><a href=#>下一页</a></li>
				</ul>
			</div>
		</div>
    	
		<jsp:include page="common/_footer.jsp"></jsp:include>
    </div> <!-- /container -->

    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="http://code.jquery.com/jquery-1.7.2.js"></script>
    <script src="js/bootstrap.js"></script>

  </body>
</html>
