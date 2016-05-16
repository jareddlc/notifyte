var cache = require('memory-cache');
var config = require('./config');
var log = require('./logger');
var notification = require('./notification');

var api = module.exports = {};
api._sockets = [];

api.onConnect = function onConnect(socket) {
  log.info('Socket: socket connected', socket.id);
  api._sockets[socket.id] = socket;
  if(!api._id) {
    api._id = socket.id;
  }

  socket.emit('onConnect', {});

  socket.on('disconnect', function() {
    log.info('Socket: socket disconnected', socket.id);
    if(api._sockets[socket.id]) {
      delete api._sockets[socket.id];
    }
    if(api._id) {
      delete api._id;
    }
  });

  socket.on('/api/bluetooth/state', function(data) {
    socket.emit('/api/bluetooth/state', cache.get(config.cache.ble.state));
  });

  socket.on('/api/bluetooth/advertising', function(data) {
    socket.emit('/api/bluetooth/advertising', cache.get(config.cache.ble.advertising));
  });

  socket.on('/api/bluetooth/client', function(data) {
    socket.emit('/api/bluetooth/client', cache.get(config.cache.ble.client));
  });

  socket.on('/api/notifications', function(data) {
    socket.emit('/api/notifications', notification.cacheGet());
  });
};

api.emit = function emit(name, data) {
  if(api._sockets[api._id]) {
    log.info('Socket: emitting', {name: name, data: data});
    api._sockets[api._id].emit(name, data);
  }
  else {
    log.info('Socket: no available socket');
  }
};
