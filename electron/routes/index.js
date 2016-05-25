var config = require('../lib/config');
var ble = require('./ble');
var notifications = require('./notifications');
var express = require('express');
var router = express.Router();
var sockets = require('./sockets');

var router = express.Router();

router.get('/', function(req, res) {
  res.json(config.service);
});

router.get('/api/', function(req, res) {
  var api = {
    '/api/bluetooth/': {
      'GET': 'Returns the bluetooth information'
    },
    '/api/bluetooth/state': {
      'GET': 'Returns the computers bluetooth power state'
    },
    '/api/bluetooth/advertising': {
      'GET': 'Returns the computers bluetooth advertising state',
      'POST': 'Sets computers bluetooth advertising state',
    },
    '/api/bluetooth/client': {
      'GET': 'Returns the clients bluetooth address'
    },
    '/api/notifications': {
      'GET': 'Returns the list of notifications',
      'POST': 'Create a notification'
    },
    '/api/sockets': {
      'GET': 'Returns the list of connected sockets'
    },
  };
  res.json(api);
});

router.get('/api/bluetooth/', ble.getBluetooth);

router.get('/api/bluetooth/state', ble.getBluetoothState);

router.get('/api/bluetooth/advertising', ble.getBluetoothAdvertising);

router.post('/api/bluetooth/advertising', ble.postBluetoothAdvertising);

router.get('/api/bluetooth/client', ble.getBluetoothClient);

router.get('/api/notifications', notifications.getNotifications);

router.post('/api/notifications', notifications.postNotifications);

router.get('/api/sockets', sockets.getSockets);

module.exports = router;
