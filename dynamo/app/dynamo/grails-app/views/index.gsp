<%@ page import="grails.util.Environment" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <!-- Meta properties -->
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="charset" content="utf-8"/>

    <!-- Styles -->
    <asset:stylesheet src="application.css"/>
    <link href="https://fonts.googleapis.com/css?family=Raleway:400,600" rel="stylesheet" type="text/css"/>
    <link href="https://fonts.googleapis.com/css?family=Lato" rel="stylesheet" type="text/css"/>

    <!-- Scripts -->
    <asset:javascript src="application.js"/>
    <script type="text/javascript" src="https://code.jquery.com/jquery-1.12.1.min.js"></script>

    <title>Gatvakam Dynamo</title>
</head>

<body>

<nav class="navbar navbar-default">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse"
                    data-target="#bs-example-navbar-collapse-6" aria-expanded="false">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Dynamo</a>
        </div>

        <div class="collapse navbar-collapse">
            <ul class="nav navbar-nav">
                <li><g:link controller="Main" action="info">Server info</g:link></li>
            </ul>

            <ul class="nav navbar-nav navbar-right">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true"
                       aria-expanded="false">Application Status <span class="caret"></span></a>
                    <ul class="dropdown-menu">
                        <li><a href="#">Environment: ${Environment.current.name}</a></li>
                        <li><a href="#">App profile: ${grailsApplication.config.grails?.profile}</a></li>
                        <li><a href="#">App version:
                            <g:meta name="info.app.version"/></a>
                        </li>
                        <li role="separator" class="divider"></li>
                        <li><a href="#">Grails version:
                            <g:meta name="info.app.grailsVersion"/></a>
                        </li>
                        <li><a href="#">Groovy version: ${GroovySystem.getVersion()}</a></li>
                        <li><a href="#">JVM version: ${System.getProperty('java.version')}</a></li>
                        <li role="separator" class="divider"></li>
                        <li><a href="#">Reloading active: ${Environment.reloadingAgentEnabled}</a></li>
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true"
                       aria-expanded="false">Artefacts <span class="caret"></span></a>
                    <ul class="dropdown-menu">
                        <li><a href="#">Controllers: ${grailsApplication.controllerClasses.size()}</a></li>
                        <li><a href="#">Domains: ${grailsApplication.domainClasses.size()}</a></li>
                        <li><a href="#">Services: ${grailsApplication.serviceClasses.size()}</a></li>
                        <li><a href="#">Tag Libraries: ${grailsApplication.tagLibClasses.size()}</a></li>
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true"
                       aria-expanded="false">Installed Plugins <span class="caret"></span></a>
                    <ul class="dropdown-menu">
                        <g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
                            <li><a href="#">${plugin.name} - ${plugin.version}</a></li>
                        </g:each>
                    </ul>
                </li>
            </ul>

        </div>
    </div>
</nav>

<div class="container" style="margin-top: 10px">
    <div class="row">
        <div class="col-lg-12">
            <div class="well">
                <h3>Distribuované programové systémy</h3>
                <h4>Gabriel Takács, Miroslav Takács, Matúš Demko</h4>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-lg-6 col-md-6 col-xs-6 thumb">
            <div class="well">
                <ul id="serverinfo">
                </ul>
            </div>
        </div>

        <div class="col-lg-6 col-md-6 col-xs-6 ">
            <div class="well">
                <div id="controllers" role="navigation">
                    <h2>Available Controllers:</h2>
                    <ul>
                        <g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName }}">
                            <li class="controller">
                                <g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </div>
        </div>

    </div>

    <div class="row">
        <div class="col-lg-6 col-md-6 col-xs-6 ">
            <div class="panel panel-default">
                <div class="panel-body form-horizontal">
                    <g:form method="post" class="form-horizontal" controller="main">
                        <div class="form-group">
                            <div class="col-sm-3 >
                                <label control-label">Key:
                            </div>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" name="key"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="col-sm-3 >
                                <label control-label">Value:
                            </div>
                            <div class="col-sm-8">
                                <input type="text" class="form-control" name="value"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <g:actionSubmit class="btn btn-default col-sm-4 col-sm-offset-1" value="Save" action="postData"/>
                        </div>

                    </g:form>

                </div>
            </div>
        </div>
        <div class="col-lg-6 col-md-6 col-xs-6 ">
            <div class="panel panel-default">
                <div class="panel-body form-horizontal">
                        <div class="form-group">
                            <div class="col-sm-3 >
                                <label control-label">Key:
                            </div>
                            <div class="col-sm-9">
                                <input type="text" class="form-control" name="key" id="input-key"/>
                            </div>
                        </div>
                        <input type="button" class="btn btn-info" value="Get" onclick="getData()">
                    <div id="getDataResult">

                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

</body>

<script>
    $.ajax({
        url: "${createLink(controller:'Main', action: 'info')}", success: function (result) {
            var res = '';
            $.each(result, function (k, v) {
                res += "<li>" + k + ": " + v + "</li>";
            });
            $("#serverinfo").html(res);
        }
    });
    function getData() {
        var data = {"key": $("#input-key").val()};
        console.log(data);
        $.ajax({
            data: data,
            url: "${createLink(controller:'Main', action: 'getData')}",
            success: function (result) {
                console.log(result);
                var res = '';
                $.each(result, function (k, v) {
                    res += "<li>" + k + ": " + v + "</li>";
                });
                $("#getDataResult").html(JSON.stringify(result));
            }
        });
    }

</script>
</html>