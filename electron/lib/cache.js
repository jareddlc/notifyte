var memory = require('memory-cache');
var config = require('./config');
var log = require('./logger');
var socket = require('./socket');

var api = module.exports = {};

api.init = function init(callback) {
  memory.put(config.cache.ble.bluetooth, {
    state: null,
    advertising: false,
    client: null,
    subscribed: null
  });
  memory.put(config.cache.ble.state, {state: null});
  memory.put(config.cache.ble.advertising, {advertising: false});
  memory.put(config.cache.ble.client, {client: null});
  memory.put(config.cache.ble.subscribed, {subscribed: null});
  memory.put(config.cache.notifications, {});
  memory.put(config.cache.sockets, []);

  log.info('Cache initialized');
  if(callback) {
    callback(null);
  }
};

api.get = function get(key) {
  return memory.get(key);
};

api.put = function put(key, data) {
  memory.put(key, data);

  if(key === config.cache.ble.state || key === config.cache.ble.advertising ||
    key === config.cache.ble.client || key === config.cache.ble.subscribed) {
    memory.put(config.cache.ble.bluetooth, {
      state: memory.get(config.cache.ble.state).state,
      advertising: memory.get(config.cache.ble.advertising).advertising,
      client: memory.get(config.cache.ble.client).client,
      subscribed: memory.get(config.cache.ble.subscribed).subscribed
    });
  }

  if(key === config.cache.notifications) {
    socket.emit('/api/notifications', memory.get(config.cache.notifications));
  }

  if(key === config.cache.sockets) {
    socket.emit('/api/sockets', memory.get(config.cache.sockets));
  }
};
