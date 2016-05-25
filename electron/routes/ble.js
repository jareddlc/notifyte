var config = require('../lib/config');
var cache = require('../lib/cache');

var api = module.exports = {};

api.getBluetooth = function getBluetooth(req, res) {
  res.json(cache.get(config.cache.ble.bluetooth));
};

api.getBluetoothState = function getBluetoothState(req, res) {
  res.json(cache.get(config.cache.ble.state));
};

api.getBluetoothAdvertising = function getBluetoothAdvertising(req, res) {
  res.json(cache.get(config.cache.ble.advertising));
};

api.postBluetoothAdvertising = function postBluetoothAdvertising(req, res) {
  res.send(200);
};

api.getBluetoothClient = function getBluetoothClient(req, res) {
  res.json(cache.get(config.cache.ble.client));
};

api.getBluetoothSubscribed= function getBluetoothSubscribed(req, res) {
  res.json(cache.get(config.cache.ble.subscribed));
};
