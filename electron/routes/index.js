var config = require('../lib/config');
var ble = require('./ble');
var express = require('express');
var router = express.Router();

var router = express.Router();

router.get('/', function(req, res) {
  res.json(config.service);
});

router.get('/api/bluetooth/state', ble.getBluetoothState);

router.get('/api/bluetooth/advertising', ble.getBluetoothAdvertising);

router.get('/api/bluetooth/client', ble.getBluetoothClient);

module.exports = router;
