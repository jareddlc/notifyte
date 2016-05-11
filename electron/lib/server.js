var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var config = require('./config');
var express = require('express');
var http = require('http');
var log = require('./logger');
var path = require('path');
var routes = require('../routes');

var api = module.exports = {};

api.init = function init(callback) {
  var server = express();

  server.set('views', path.join(__dirname, '../site/views'));
  server.set('view engine', 'html');
  server.engine('html', require('hbs').__express);

  server.use(bodyParser.json());
  server.use(bodyParser.urlencoded({extended: true}));
  server.use(cookieParser());
  server.use(express.static(path.join(__dirname, '../site/static/')));

  server.set('port', process.env.PORT || config.port);

  server.use('/', routes);

  http.createServer(server).listen(server.get('port'), function() {
    log.info('Server: listening on port: ' + config.port);
    if(callback) {
      callback(null);
    }
  });
};
