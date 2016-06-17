var bleno = require('bleno');
var BlenoPrimaryService = bleno.PrimaryService;
var config = require('./config');
var Characteristic = require('./ble.characteristic');
var log = require('./logger');

var api = module.exports = {};

api.init = function init(data, callback) {
  if(!data || !data.cache) {
    throw new Error('Error: cannot initialize without cache');
  }
  api.cache = data.cache;
  api.characteristic = new Characteristic(data);
  bleno.on('stateChange', function(state) {
    // state = unknown | resetting | unsupported | unauthorized | poweredOff | poweredOn
    log.info('BLE: stateChange: ' + state);
    api.cache.put(config.cache.ble.state, {state: state});

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
  log.info('BLE: startAdvertising');
  bleno.startAdvertising(config.name, config.serviceUuid);
  api.cache.put(config.cache.ble.advertising, {advertising: true});
  if(callback && !api._callbackCalled) {
    api._callbackCalled = true;
    callback(null);
  }
};

api.stopAdvertising =  function stopAdvertising(callback) {
  log.info('BLE: stopAdvertising');
  bleno.stopAdvertising();
  api.cache.put(config.cache.ble.advertising, {advertising: false});
  if(callback && !api._callbackCalled) {
    api._callbackCalled = true;
    callback(null);
  }
};

api.onAdvertisingStart = function onAdvertisingStart(err) {
  log.info('BLE: advertisingStart: ' + (err ? 'error ' + err : 'success'));

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
  log.info('BLE: onAdvertisingStartError: ' + err);
};

api.onAdvertisingStop = function onAdvertisingStop() {
  log.info('BLE: onAdvertisingStop');
};

api.onAccept = function onAccept(clientAddress) {
  api.cache.put(config.cache.ble.client, {client: clientAddress});
  log.info('BLE: accept: ' + clientAddress);
};

api.onDisconnect = function onDisconnect(clientAddress) {
  api.cache.put(config.cache.ble.client, {client: null});
  log.info('BLE: disconnect: ' + clientAddress);
};

api.onRssiUpdate = function onRssiUpdate(rssi) {
  log.info('BLE: rssiUpdate: ' + rssi);
};

api.onServicesSet = function onServicesSet(err) {
  log.info('BLE: onServicesSet: ' + err);
};

api.onServicesSetError = function onServicesSetError(err) {
  log.info('BLE: onServicesSetError: ' + err);
};

api.sendNotification = function sendNotification(notification) {
  log.info('ble.sendNotification');
  if(api.characteristic) {
    api.characteristic.sendNotification(notification);
  }
};
