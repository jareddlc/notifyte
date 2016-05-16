var _ = require('lodash');
var bleno = require('bleno');
var cache = require('memory-cache');
var config = require('./config');
var Descriptor = bleno.Descriptor;
var log = require('./logger');
var notification = require('./notification');
var util = require('util');

var BlenoCharacteristic = bleno.Characteristic;

var Characteristic = function() {
  var descriptor = new Descriptor({
      uuid: config.descriptor.uuid,
      value: null
  });
  Characteristic.super_.call(this, {
    uuid: config.characteristic.uuid,
    properties: config.characteristic.properties,
    value: null,
    descriptors: [
      descriptor
    ]
  });

  this._hasRemaining = false;
  this._byteBuffer = null;
  this._value = new Buffer(0);
  this._updateValueCallback = null;
};
util.inherits(Characteristic, BlenoCharacteristic);

Characteristic.prototype.onReadRequest = function(offset, callback) {
  log.debug('Characteristic: onReadRequest: value = ' + this._value.toString());
  callback(this.RESULT_SUCCESS, this._value);
};

Characteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {
  log.debug('Characteristic: onWriteRequest: value = ' + this._value.toString() + ' size = ' + data.length);

  if(this._hasRemaining) {
    this._byteBuffer += _.cloneDeep(data);
  }
  else {
    this._byteBuffer = _.cloneDeep(data);
  }

  if(data.length === config.ble.limit) {
    this._hasRemaining = true;
    this._value = _.cloneDeep(this._byteBuffer);
  }
  else {
    this._hasRemaining = false;
    this._value = _.cloneDeep(this._byteBuffer);
    this._byteBuffer = null;
  }

  if(this._updateValueCallback) {
    log.debug('Characteristic: onWriteRequest: notifying');
    this._updateValueCallback(this._value);
  }

  var json = notification.parse(this._value.toString());
  log.info(json);
  if(json !== null) {
    notification.cachePut(json);
  }

  callback(this.RESULT_SUCCESS);
};

Characteristic.prototype.onSubscribe = function(maxValueSize, updateValueCallback) {
  log.info('Characteristic: onSubscribe');
  this._updateValueCallback = updateValueCallback;
};

Characteristic.prototype.onUnsubscribe = function() {
  log.info('Characteristic: onUnsubscribe');
  cache.put(config.cache.ble.client, {client: null});
  this._updateValueCallback = null;
};

Characteristic.prototype.onNotify = function() {
  log.info('Characteristic: onNotify');
};

module.exports = Characteristic;
