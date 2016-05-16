var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var config = require('./config');
var express = require('express');
var http = require('http');
var log = require('./logger');
var path = require('path');
var routes = require('../routes');
var socket = require('./socket');
var socketio  = require('socket.io');

var api = module.exports = {};

api.init = function init(callback) {
  var app = express();

  app.set('views', path.join(__dirname, '../site/views'));
  app.set('view engine', 'html');
  app.engine('html', require('hbs').__express);

  app.use(bodyParser.json());
  app.use(bodyParser.urlencoded({extended: true}));
  app.use(cookieParser());
  app.use(express.static(path.join(__dirname, '../site/static/')));

  app.set('port', process.env.PORT || config.port);

  app.use('/', routes);

  var server = http.createServer(app).listen(app.get('port'), function() {
    log.info('Server: listening on port: ' + config.port);
    if(callback) {
      callback(null);
    }
  });

  var io = socketio.listen(server);

  io.on('connection', socket.onConnect);
  io.on('error', function ioError(err) {
    log.error('Socket: error', err);
  });
};
