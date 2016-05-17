var _ = require('lodash');
var bleno = require('bleno');
var cache = require('memory-cache');
var config = require('./config');
var log = require('./logger');
var notification = require('./notification');
var util = require('util');

var BlenoCharacteristic = bleno.Characteristic;

var Characteristic = function() {
  Characteristic.super_.call(this, {
    uuid: config.characteristic.uuid,
    properties: config.characteristic.properties,
    value: null
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
  log.info('Characteristic: onSubscribe: ' + maxValueSize);
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

Characteristic.prototype.sendNotification = function(notification) {
  log.debug('Characteristic: sendNotification', notification);
  if(this._updateValueCallback) {
    var buffer = [];
    var json = JSON.stringify(notification);

    if(json.length > config.ble.mtu) {
      var b = new Buffer(config.ble.mtu);
      for(var i = 0; i < json.length; i++) {
        if(i !== 0 && i % config.ble.mtu === 0) {
          buffer.push(b);
          b = new Buffer(config.ble.mtu);
          b.write(json[i], i % config.ble.mtu, 'utf8');
        }
        else if(i === json.length - 1) {
          b.write(json[i], i % config.ble.mtu, 'utf8');
          buffer.push(b);
        }
        else {
          b.write(json[i], i % config.ble.mtu, 'utf8');
        }
      }
    }

    //this._updateValueCallback(buff);
  }
};

Characteristic.prototype._sendNotification = function(buffer) {
  this._updateValueCallback(buffer);
};

module.exports = Characteristic;
