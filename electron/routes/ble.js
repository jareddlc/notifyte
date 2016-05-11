var config = require('../lib/config');
var cache = require('memory-cache');

var api = module.exports = {};

api.getBluetoothState = function getBluetoothState(req, res) {
  res.json(cache.get(config.cache.ble.state));
};

api.getBluetoothAdvertising = function getBluetoothAdvertising(req, res) {
  res.json(cache.get(config.cache.ble.advertising));
};

api.getBluetoothClient = function getBluetoothClient(req, res) {
  res.json(cache.get(config.cache.ble.client));
};
