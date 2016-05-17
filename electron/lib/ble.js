var bleno = require('bleno');
var BlenoPrimaryService = bleno.PrimaryService;
var cache = require('memory-cache');
var config = require('./config');
var Characteristic = require('./ble.characteristic');
var log = require('./logger');

var api = module.exports = {};
api.characteristic = new Characteristic();

api.init = function init(callback) {
  cache.put(config.cache.ble.client, {client: null});
  bleno.on('stateChange', function(state) {
    // state = unknown | resetting | unsupported | unauthorized | poweredOff | poweredOn
    log.info('on -> stateChange: ' + state);
    cache.put(config.cache.ble.state, {state: state});

    if(state === 'poweredOn') {
      api.startAdvertising(callback);
    }
    else {
      api.stopAdvertising(callback);
    }
  });

  log.info('BLE initialized');

  bleno.on('accept', api.onAccept);
  bleno.on('advertisingStart', api.onAdvertisingStart);
  bleno.on('advertisingStartError', api.onAdvertisingStartError);
  bleno.on('advertisingStop', api.onAdvertisingStop);
  bleno.on('disconnect', api.onDisconnect);
  bleno.on('rssiUpdate', api.onRssiUpdate);
  bleno.on('servicesSet', api.onServicesSet);
  bleno.on('servicesSetError', api.onServicesSetError);
};

api.startAdvertising =  function startAdvertising(callback) {
  log.info('startAdvertising');
  bleno.startAdvertising(config.name, config.serviceUuid);
  cache.put(config.cache.ble.advertising, {advertising: true});
  if(callback) {
    callback(null);
  }
};

api.stopAdvertising =  function stopAdvertising(callback) {
  log.info('stopAdvertising');
  bleno.stopAdvertising();
  cache.put(config.cache.ble.advertising, {advertising: false});
  if(callback) {
    callback(null);
  }
};

api.onAdvertisingStart = function onAdvertisingStart(err) {
  log.info('on -> advertisingStart: ' + (err ? 'error ' + err : 'success'));

  if(!err) {
    bleno.setServices([
      new BlenoPrimaryService({
        uuid: config.serviceUuid[0],
        characteristics: [
          api.characteristic
        ]
      })
    ]);
  }
};

api.onAdvertisingStartError = function onAdvertisingStartError(err) {
  log.info('on -> onAdvertisingStartError: ' + err);
};

api.onAdvertisingStop = function onAdvertisingStop() {
  log.info('on -> onAdvertisingStop');
};

api.onAccept = function onAccept(clientAddress) {
  cache.put(config.cache.ble.client, {client: clientAddress});
  log.info('on -> accept: ' + clientAddress);
};

api.onDisconnect = function onDisconnect(clientAddress) {
  cache.put(config.cache.ble.client, {client: null});
  log.info('on -> disconnect: ' + clientAddress);
};

api.onRssiUpdate = function onRssiUpdate(rssi) {
  log.info('on -> rssiUpdate: ' + rssi);
};

api.onServicesSet = function onServicesSet(err) {
  log.info('on -> onServicesSet: ' + err);
};

api.onServicesSetError = function onServicesSetError(err) {
  log.info('on -> onServicesSetError: ' + err);
};

api.sendNotification = function sendNotification(notification) {
  log.info('ble.sendNotification');
  if(api.characteristic) {
    api.characteristic.sendNotification(notification);
  }
};
