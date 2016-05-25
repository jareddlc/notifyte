var _ = require('lodash');
var config = require('./config');
var log = require('./logger');

var api = module.exports = {};
api._sockets = [];

api.init = function init(data, callback) {
  if(!data || !data.cache) {
    throw new Error('Error: cannot initialize without cache');
  }
  api.cache = data.cache;

  if(callback) {
    callback(null);
  }
};

api.onConnect = function onConnect(socket) {
  log.info('Socket: socket connected', socket.id);
  api._sockets[socket.id] = socket;
  api.put(socket);

  if(!api._id) {
    api._id = socket.id;
  }

  socket.emit('onConnect', {});

  socket.on('disconnect', function() {
    log.info('Socket: socket disconnected', socket.id);
    if(api._sockets[socket.id]) {
      api.del(socket);
      delete api._sockets[socket.id];
    }
    if(api._id) {
      delete api._id;
    }
  });

  socket.on('/api/bluetooth/', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/bluetooth/', api.cache.get(config.cache.ble.bluetooth));
    }
  });

  socket.on('/api/bluetooth/state', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/bluetooth/state', api.cache.get(config.cache.ble.state));
    }
  });

  socket.on('/api/bluetooth/advertising', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/bluetooth/advertising', api.cache.get(config.cache.ble.advertising));
    }
  });

  socket.on('/api/bluetooth/client', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/bluetooth/client', api.cache.get(config.cache.ble.client));
    }
  });

  socket.on('/api/notifications', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/notifications', api.cache.get(config.cache.notifications));
    }
  });

  socket.on('/api/sockets', function(data) {
    if(data.method === 'GET') {
      socket.emit('/api/sockets', api.cache.get(config.cache.sockets));
    }
  });
};

api.get = function get() {
  return api.cache.get(config.cache.sockets);
};

api.put = function put(socket) {
  var sockets = api.get();
  sockets.push({id: socket.id, connected: socket.connected});
  api.cache.put(config.cache.sockets, sockets);
};

api.del = function get(socket) {
  var sockets = api.get();

  var index = _.findLastIndex(sockets, function(s) {
    return s.id === socket.id;
  });

  if(index >= 0) {
    sockets[index].connected = socket.connected;

    api.cache.put(config.cache.sockets, sockets);
  }
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
